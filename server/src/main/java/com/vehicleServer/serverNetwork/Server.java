package com.vehicleServer.serverNetwork;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleServer.managers.CommandManager;
import com.vehicleShared.managers.CollectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private final int port;
    private CollectionManager collectionManager;
    private final ExecutorService readerPool = Executors.newFixedThreadPool(4);
    private final ExecutorService responderPool = Executors.newCachedThreadPool();
    private final Map<SocketChannel, String> authenticatedUsers = new ConcurrentHashMap<>();
    private final Map<SocketChannel, ByteArrayOutputStream> clientBuffers = new ConcurrentHashMap<>();
    private String dbUser;
    private String dbPassword;

    public Server(int port) {
        this.port = port;
        logger.debug("инициализация сервера на порту {}", port);
        try {
            initializeManagers();
        } catch (Exception e) {
            logger.error("ошибка при инициализации: {}", e.getMessage(), e);
            throw new RuntimeException("не удалось инициализировать сервер", e);
        }
    }

    private void initializeManagers() {
        logger.debug("инициализация CollectionManager");
        collectionManager = new CollectionManager();
        logger.debug("инициализация CommandManager");
        CommandManager.initialize(collectionManager, logger);
        logger.debug("менеджеры инициализированы");
    }

    private void promptDbCredentials() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("введите логин базы данных: ");
        dbUser = scanner.nextLine().trim();
        System.out.print("введите пароль базы данных: ");
        dbPassword = scanner.nextLine().trim();
        logger.debug("получены креды базы: user={}", dbUser);
    }

    public void start() {
        logger.info("сервер запускается на порту {}", port);
        promptDbCredentials();

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            logger.debug("открытие ServerSocketChannel");
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            Selector selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            logger.info("сервер готов принимать подключения");

            Thread consoleThread = new Thread(this::handleConsoleInput);
            consoleThread.setDaemon(true);
            consoleThread.start();

            while (true) {
                logger.debug("ожидание событий в selector");
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        acceptClient(serverChannel, selector);
                    } else if (key.isReadable()) {
                        readerPool.submit(() -> processClient(key, selector));
                    }
                }
            }
        } catch (IOException e) {
            logger.error("ошибка сервера: {}", e.getMessage(), e);
            throw new RuntimeException("сервер завершил работу из-за ошибки", e);
        }
    }

    private void handleConsoleInput() {
        Scanner scanner = new Scanner(System.in);
        logger.info("консоль сервера готова. команды: info, show, exit");

        while (true) {
            System.out.print("сервер> ");
            if (scanner.hasNextLine()) {
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) continue;

                String[] parts = input.split(" ", 2);
                String command = parts[0];
                String argument = parts.length > 1 ? parts[1] : null;

                if ("exit".equalsIgnoreCase(command)) {
                    logger.info("сервер завершает работу");
                    System.exit(0);
                }

                Response response = CommandManager.executeRequest(new Request(command, argument, "console", "none"));
                System.out.println("результат: " + response.getMessage());
                if (response.getException() != null) {
                    System.out.println("ошибка: " + response.getException().getMessage());
                }
            }
        }
    }

    private void acceptClient(ServerSocketChannel serverChannel, Selector selector) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientBuffers.put(clientChannel, new ByteArrayOutputStream());
        clientChannel.register(selector, SelectionKey.OP_READ, clientChannel);
        logger.info("клиент подключился: {}", clientChannel.getRemoteAddress());
    }

    private void processClient(SelectionKey key, Selector selector) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteArrayOutputStream clientBuffer = clientBuffers.get(clientChannel);
        if (clientBuffer == null) {
            logger.error("буфер клиента не найден для {}", clientChannel);
            try {
                clientChannel.close();
                key.cancel();
            } catch (IOException e) {
                logger.error("ошибка закрытия канала: {}", e.getMessage());
            }
            return;
        }

        synchronized (clientBuffer) {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(4096);
                int bytesRead;

                synchronized (clientChannel) {
                    bytesRead = clientChannel.read(buffer);
                }

                if (bytesRead == -1) {
                    clientChannel.close();
                    authenticatedUsers.remove(clientChannel);
                    clientBuffers.remove(clientChannel);
                    logger.info("клиент отключился: {}", clientChannel.getRemoteAddress());
                    key.cancel();
                    return;
                }

                buffer.flip();
                byte[] readData = new byte[buffer.remaining()];
                buffer.get(readData);
                clientBuffer.write(readData);
                logger.debug("прочитано {} байт, буфер: {}", readData.length, clientBuffer.size());

                byte[] data = clientBuffer.toByteArray();
                while (data.length >= 4) {
                    ByteBuffer lengthBuffer = ByteBuffer.wrap(data, 0, 4);
                    int expectedLength = lengthBuffer.getInt();
                    logger.debug("ожидаемая длина: {}", expectedLength);

                    if (expectedLength <= 0 || data.length < 4 + expectedLength) {
                        break;
                    }

                    byte[] requestData = new byte[expectedLength];
                    System.arraycopy(data, 4, requestData, 0, expectedLength);
                    logger.debug("десериализация запроса, длина: {}", requestData.length);
                    Request request = deserialize(requestData);
                    logger.info("получен запрос: {}", request);

                    Response response;
                    String command = request.getCommand();
                    String login = request.getLogin();
                    String password = request.getPassword();

                    if (command.equals("login") || command.equals("register")) {
                        boolean dbInitialized = collectionManager.initDb(
                                "jdbc:postgresql://pg:5432/studs",
                                dbUser,
                                dbPassword
                        );
                        if (!dbInitialized) {
                            logger.error("не удалось подключиться к базе");
                            response = Response.error("ошибка подключения к базе");
                        } else {
                            if (command.equals("login")) {
                                try {
                                    if (collectionManager.authenticateUser(login, password)) {
                                        authenticatedUsers.put(clientChannel, login);
                                        collectionManager.loadFromDb();
                                        response = Response.success("авторизация успешна");
                                        logger.info("успешная авторизация: {}", login);
                                    } else {
                                        response = Response.error("неверный логин или пароль");
                                        logger.warn("неверный логин/пароль: {}", login);
                                    }
                                } catch (SQLException e) {
                                    response = Response.error("ошибка базы: " + e.getMessage());
                                    logger.error("ошибка базы: {}", e.getMessage());
                                }
                            } else {
                                try {
                                    if (collectionManager.registerUser(login, password)) {
                                        authenticatedUsers.put(clientChannel, login);
                                        collectionManager.loadFromDb();
                                        response = Response.success("регистрация успешна");
                                        logger.info("успешная регистрация: {}", login);
                                    } else {
                                        response = Response.error("пользователь уже существует");
                                        logger.warn("пользователь уже существует: {}", login);
                                    }
                                } catch (SQLException e) {
                                    response = Response.error("ошибка базы: " + e.getMessage());
                                    logger.error("ошибка базы: {}", e.getMessage());
                                }
                            }
                        }
                    } else {
                        String userId = authenticatedUsers.get(clientChannel);
                        if (userId == null) {
                            response = Response.error("требуется авторизация: используйте 'login' или 'register'");
                            logger.warn("неавторизованный запрос: {}", command);
                        } else {
                            request.setLogin(userId);
                            response = CommandManager.executeRequest(request);
                            if (command.equals("shutdown")) {
                                notifyAllClients(selector, response);
                                logger.info("сервер завершает работу");
                                System.exit(0);
                            }
                        }
                    }

                    Response finalResponse = response;
                    responderPool.submit(() -> sendResponse(key, clientChannel, finalResponse));
                    byte[] remainingData = new byte[data.length - 4 - expectedLength];
                    System.arraycopy(data, 4 + expectedLength, remainingData, 0, remainingData.length);
                    clientBuffer.reset();
                    clientBuffer.write(remainingData);
                    data = clientBuffer.toByteArray();
                }
                key.interestOps(SelectionKey.OP_READ);
            } catch (IOException | ClassNotFoundException e) {
                logger.error("ошибка при обработке клиента: {}", e.getMessage(), e);
                try {
                    clientChannel.close();
                    authenticatedUsers.remove(clientChannel);
                    clientBuffers.remove(clientChannel);
                    key.cancel();
                } catch (IOException ignored) {}
            }
        }
    }

    private void notifyAllClients(Selector selector, Response exitResponse) {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                SocketChannel clientChannel = (SocketChannel) key.channel();
                responderPool.submit(() -> sendResponse(key, clientChannel, exitResponse));
            }
        }
    }

    private void sendResponse(SelectionKey key, SocketChannel clientChannel, Response response) {
        try {
            byte[] data = serialize(response);
            ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
            lengthBuffer.putInt(data.length);
            lengthBuffer.flip();
            synchronized (clientChannel) {
                while (lengthBuffer.hasRemaining()) {
                    clientChannel.write(lengthBuffer);
                }
                ByteBuffer dataBuffer = ByteBuffer.wrap(data);
                while (dataBuffer.hasRemaining()) {
                    clientChannel.write(dataBuffer);
                }
            }
            logger.info("отправлен ответ клиенту {}: {}", clientChannel.getRemoteAddress(), response);
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            logger.error("ошибка отправки ответа: {}", e.getMessage());
            try {
                clientChannel.close();
                authenticatedUsers.remove(clientChannel);
                clientBuffers.remove(clientChannel);
                key.cancel();
            } catch (IOException ignored) {}
        }
    }

    private Request deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (Request) ois.readObject();
        }
    }

    private byte[] serialize(Response response) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(response);
            oos.flush();
            return baos.toByteArray();
        }
    }
}