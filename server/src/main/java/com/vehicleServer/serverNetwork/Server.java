package com.vehicleServer.serverNetwork;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleServer.managers.CommandManager;
import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.managers.DbManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private final int port;
    private CollectionManager collectionManager;
    private DbManager dbManager;
    private final ExecutorService readerPool = Executors.newFixedThreadPool(4);
    private final ExecutorService responderPool = Executors.newCachedThreadPool();
    private final Map<SocketChannel, String> authenticatedUsers = new ConcurrentHashMap<>();
    private final Map<SocketChannel, ByteArrayOutputStream> clientBuffers = new ConcurrentHashMap<>();
    private final Map<SocketChannel, SocketAddress> clientAddresses = new ConcurrentHashMap<>();
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
        logger.debug("инициализация DbManager и CollectionManager");
        dbManager = new DbManager();
        collectionManager = new CollectionManager(dbManager);
        logger.debug("инициализация CommandManager");
        CommandManager.initialize(collectionManager, dbManager, logger);
        logger.debug("менеджеры инициализированы");
    }

    private void promptDbCredentials() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("введите логин базы данных: ");
        dbUser = scanner.nextLine().trim();
        System.out.print("введите пароль базы данных: ");
        dbPassword = scanner.nextLine().trim();
    }

    public void start() {
        promptDbCredentials();
        String url = "jdbc:postgresql://pg:5432/studs";
        if (!dbManager.initDb(url, dbUser, dbPassword)) {
            logger.error("не удалось подключиться к базе данных");
            return;
        }
        try {
            collectionManager.loadFromDb();
            logger.info("коллекция загружена из базы данных");
        } catch (SQLException e) {
            logger.error("ошибка загрузки коллекции: {}", e.getMessage(), e);
            return;
        }
        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
            serverSocket.bind(new InetSocketAddress(port));
            serverSocket.configureBlocking(false);
            Selector selector = Selector.open();
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            logger.info("сервер запущен на порту {}", port);
            while (true) {
                System.out.println("ожидание событий в selector в " + System.currentTimeMillis());
                selector.select();
                System.out.println("событие получено в selector");
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    try {
                        if (key.isAcceptable()) {
                            System.out.println("принимаем нового клиента");
                            acceptClient(serverSocket, selector);
                        } else if (key.isReadable()) {
                            System.out.println("читаем данные от клиента");
                            readClient(key);
                        }
                    } catch (IOException e) {
                        logger.error("ошибка обработки клиента: {}", e.getMessage(), e);
                        disconnectClient(key);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("ошибка сервера: {}", e.getMessage(), e);
        } finally {
            dbManager.closeDb();
        }
    }

    private void acceptClient(ServerSocketChannel serverSocket, Selector selector) throws IOException {
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        clientBuffers.put(client, new ByteArrayOutputStream());
        clientAddresses.put(client, client.getRemoteAddress());
        logger.info("новый клиент подключен: {}", clientAddresses.get(client));
    }

    private void readClient(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        readerPool.submit(() -> {
            try {
                Request request = readRequest(client);
                if (request != null) {
                    processClient(client, request, key);
                }
            } catch (IOException | ClassNotFoundException e) {
                SocketAddress address = clientAddresses.getOrDefault(client, null);
                logger.error("ошибка чтения от клиента {}: {}", address, e.getMessage(), e);
                disconnectClient(key);
            }
        });
    }

    private Request readRequest(SocketChannel client) throws IOException, ClassNotFoundException {
        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
        int bytesRead = client.read(lengthBuffer);
        if (bytesRead == -1) {
            disconnectClient(client);
            return null;
        }
        if (bytesRead < 4) {
            return null;
        }
        lengthBuffer.flip();
        int length = lengthBuffer.getInt();
        ByteBuffer dataBuffer = ByteBuffer.allocate(length);
        bytesRead = client.read(dataBuffer);
        if (bytesRead == -1) {
            disconnectClient(client);
            return null;
        }
        if (bytesRead < length) {
            clientBuffers.get(client).write(dataBuffer.array(), 0, bytesRead);
            return null;
        }
        dataBuffer.flip();
        byte[] data = new byte[length];
        dataBuffer.get(data);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (Request) ois.readObject();
        }
    }

    private void processClient(SocketChannel client, Request request, SelectionKey key) {
        responderPool.submit(() -> {
            try {
                Response response;
                boolean isAuthenticated = authenticatedUsers.containsKey(client);
                System.out.println("команда: '" + request.getCommand() + "' (длина: " + request.getCommand().length() + ")");
                if (request.getCommand().equals("login")) {
                    System.out.println("обработка login для " + request.getLogin());
                    if (dbManager.authenticateUser(request.getLogin(), request.getPassword())) {
                        authenticatedUsers.put(client, request.getLogin());
                        response = Response.success("авторизация успешна");
                    } else {
                        response = Response.error("неверный логин или пароль");
                    }
                } else if (request.getCommand().equals("register")) {
                    System.out.println("регистрация пользователя " + request.getLogin());
                    if (dbManager.registerUser(request.getLogin(), request.getPassword())) {
                        authenticatedUsers.put(client, request.getLogin());
                        response = Response.success("регистрация успешна");
                    } else {
                        response = Response.error("пользователь уже существует или ошибка регистрации");
                    }
                } else {
                    response = CommandManager.executeRequest(request, isAuthenticated);
                }
                System.out.println("отправляем ответ: " + response.getMessage());
                sendResponse(client, response, key);
            } catch (Exception e) {
                SocketAddress address = clientAddresses.getOrDefault(client, null);
                logger.error("ошибка обработки запроса от клиента {}: {}", address, e.getMessage(), e);
                try {
                    sendResponse(client, Response.error("внутренняя ошибка сервера"), key);
                } catch (IOException ex) {
                    logger.error("ошибка отправки ответа: {}", ex.getMessage(), ex);
                    disconnectClient(client);
                }
            }
        });
    }

    private void sendResponse(SocketChannel client, Response response, SelectionKey key) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(response);
        }
        byte[] data = baos.toByteArray();
        ByteBuffer buffer = ByteBuffer.allocate(4 + data.length);
        buffer.putInt(data.length);
        buffer.put(data);
        buffer.flip();
        while (buffer.hasRemaining()) {
            client.write(buffer);
        }
        // Перерегистрируем канал для чтения
        client.register(key.selector(), SelectionKey.OP_READ);
    }

    private void disconnectClient(SelectionKey key) {
        try {
            SocketChannel client = (SocketChannel) key.channel();
            SocketAddress address = clientAddresses.getOrDefault(client, null);
            authenticatedUsers.remove(client);
            clientBuffers.remove(client);
            clientAddresses.remove(client);
            client.close();
            key.cancel();
            logger.info("клиент {} отключен", address);
        } catch (IOException e) {
            logger.error("ошибка отключения клиента: {}", e.getMessage(), e);
        }
    }

    private void disconnectClient(SocketChannel client) {
        try {
            SocketAddress address = clientAddresses.getOrDefault(client, null);
            authenticatedUsers.remove(client);
            clientBuffers.remove(client);
            clientAddresses.remove(client);
            client.close();
            logger.info("клиент {} отключен", address);
        } catch (IOException e) {
            logger.error("ошибка отключения клиента: {}", e.getMessage(), e);
        }
    }
}