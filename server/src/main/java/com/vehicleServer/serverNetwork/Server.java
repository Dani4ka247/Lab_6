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
import java.net.SocketTimeoutException;
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
    private final ExecutorService responderPool = Executors.newCachedThreadPool();
    private final Map<SocketChannel, String> authenticatedUsers = new ConcurrentHashMap<>();
    private final Map<SocketChannel, SocketAddress> clientAddresses = new ConcurrentHashMap<>();

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
        String dbUser = scanner.nextLine().trim();
        System.out.print("введите пароль базы данных: ");
        String dbPassword = scanner.nextLine().trim();
        // Сохраняем в поля класса
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public void start() {
        promptDbCredentials();
        String url = "jdbc:postgresql://pg:5432/studs";
        int maxRetries = 5;
        int retryDelayMs = 5000;

        // Подключение к БД
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (dbManager.initDb(url, dbUser, dbPassword)) {
                    logger.info("успешное подключение к базе с попытки {}", attempt);
                    break;
                } else {
                    logger.warn("попытка {}: не удалось подключиться к базе", attempt);
                    if (attempt == maxRetries) {
                        logger.error("не удалось подключиться после {} попыток", maxRetries);
                        return;
                    }
                    Thread.sleep(retryDelayMs);
                }
            } catch (InterruptedException e) {
                logger.error("ошибка при ожидании: {}", e.getMessage());
                Thread.currentThread().interrupt();
                return;
            }
        }

        // Загрузка коллекции
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (dbManager.isDbConnected()) {
                    collectionManager.loadFromDb(null); // Начальная загрузка
                    logger.info("коллекция загружена");
                    break;
                }
                logger.warn("попытка {}: база не готова", attempt);
                if (attempt == maxRetries) {
                    logger.error("не удалось загрузить коллекцию после {} попыток", maxRetries);
                    return;
                }
                Thread.sleep(retryDelayMs);
            } catch (SQLException | InterruptedException e) {
                logger.error("ошибка загрузки: {}", e.getMessage());
                if (attempt == maxRetries) return;
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        try (ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
            serverSocket.bind(new InetSocketAddress(port));
            serverSocket.configureBlocking(false);
            Selector selector = Selector.open();
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            logger.info("сервер запущен на порту {}", port);
            while (true) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    try {
                        if (key.isAcceptable()) {
                            acceptClient(serverSocket, selector);
                        } else if (key.isReadable()) {
                            readClient(key);
                        }
                    } catch (IOException e) {
                        logger.error("ошибка обработки клиента: {}", e.getMessage());
                        disconnectClient(key);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("ошибка сервера: {}", e.getMessage());
        } finally {
            dbManager.closeDb();
            responderPool.shutdown();
        }
    }

    private void acceptClient(ServerSocketChannel serverSocket, Selector selector) throws IOException {
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        clientAddresses.put(client, client.getRemoteAddress());
        logger.info("новый клиент подключен: {}", clientAddresses.get(client));
    }

    private void readClient(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        try {
            Request request = readRequest(client);
            if (request != null) {
                processClient(client, request, key);
            }
        } catch (IOException | ClassNotFoundException e) {
            SocketAddress address = clientAddresses.getOrDefault(client, null);
            logger.error("ошибка чтения от клиента {}: {}", address, e.getMessage());
            disconnectClient(key);
        }
    }

    private Request readRequest(SocketChannel client) throws IOException, ClassNotFoundException {
        SocketAddress address = clientAddresses.getOrDefault(client, null);
        try {
            if (!client.isOpen() || !client.isConnected()) {
                logger.info("readRequest: канал клиента {} закрыт", address);
                disconnectClient(client);
                return null;
            }
            client.socket().setSoTimeout(5000);

            ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
            int totalBytesRead = 0;
            while (totalBytesRead < 4) {
                int bytesRead = client.read(lengthBuffer);
                if (bytesRead == -1) {
                    logger.info("readRequest: клиент {} отключился при чтении длины", address);
                    disconnectClient(client);
                    return null;
                }
                totalBytesRead += bytesRead;
            }
            lengthBuffer.flip();
            int length = lengthBuffer.getInt();
            if (length <= 0 || length > 1_000_000) {
                logger.error("readRequest: некорректная длина {} от {}", length, address);
                disconnectClient(client);
                return null;
            }

            ByteBuffer dataBuffer = ByteBuffer.allocate(length);
            totalBytesRead = 0;
            while (totalBytesRead < length) {
                int bytesRead = client.read(dataBuffer);
                if (bytesRead == -1) {
                    logger.info("readRequest: клиент {} отключился при чтении данных", address);
                    disconnectClient(client);
                    return null;
                }
                totalBytesRead += bytesRead;
            }
            dataBuffer.flip();
            byte[] data = new byte[length];
            dataBuffer.get(data);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                Request request = (Request) ois.readObject();
                logger.info("readRequest: получен запрос {} от {}", request.getCommand(), address);
                return request;
            }
        } catch (SocketTimeoutException e) {
            logger.error("readRequest: таймаут от {}: {}", address, e.getMessage());
            disconnectClient(client);
            return null;
        } catch (IOException | ClassNotFoundException e) {
            logger.error("readRequest: ошибка от {}: {}", address, e.getMessage());
            disconnectClient(client);
            throw e;
        }
    }

    private void processClient(SocketChannel client, Request request, SelectionKey key) {
        responderPool.submit(() -> {
            try {
                Response response;
                String login = request.getLogin();
                boolean isAuthenticated = authenticatedUsers.containsKey(client);
                logger.info("обработка запроса: команда='{}', логин='{}', авторизован={}",
                        request.getCommand(), login, isAuthenticated);

                if (request.getCommand().equals("login") && login != null) {
                    for (Map.Entry<SocketChannel, String> entry : authenticatedUsers.entrySet()) {
                        if (entry.getValue().equals(login) && !entry.getKey().equals(client)) {
                            response = Response.error("пользователь " + login + " уже авторизован");
                            sendResponse(client, response, key);
                            return;
                        }
                    }
                }

                if (request.getCommand().equals("login")) {
                    logger.info("обработка login для {}", login);
                    if (dbManager.authenticateUser(login, request.getPassword())) {
                        authenticatedUsers.put(client, login);
                        collectionManager.loadFromDb(login);
                        response = Response.success("авторизация успешна");
                    } else {
                        response = Response.error("неверный логин или пароль");
                    }
                } else if (request.getCommand().equals("register")) {
                    logger.info("регистрация пользователя {}", login);
                    if (dbManager.registerUser(login, request.getPassword())) {
                        authenticatedUsers.put(client, login);
                        response = Response.success("регистрация успешна");
                    } else {
                        response = Response.error("пользователь уже существует или ошибка регистрации");
                    }
                } else {
                    logger.debug("передаём команду '{}' в CommandManager", request.getCommand());
                    response = CommandManager.executeRequest(request, isAuthenticated);
                }
                logger.info("отправляем ответ: {}", response.getMessage());
                sendResponse(client, response, key);
            } catch (Exception e) {
                SocketAddress address = clientAddresses.getOrDefault(client, null);
                logger.error("ошибка обработки запроса от {}: {}", address, e.getMessage());
                try {
                    sendResponse(client, Response.error("внутренняя ошибка сервера: " + e.getMessage()), key);
                } catch (IOException ex) {
                    logger.error("ошибка отправки ответа: {}", ex.getMessage());
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
        client.register(key.selector(), SelectionKey.OP_READ);
    }

    private void disconnectClient(SelectionKey key) {
        try {
            SocketChannel client = (SocketChannel) key.channel();
            SocketAddress address = clientAddresses.getOrDefault(client, null);
            authenticatedUsers.remove(client);
            clientAddresses.remove(client);
            client.close();
            key.cancel();
            logger.info("клиент {} отключен", address);
        } catch (IOException e) {
            logger.error("ошибка отключения клиента: {}", e.getMessage());
        }
    }

    private void disconnectClient(SocketChannel client) {
        try {
            SocketAddress address = clientAddresses.getOrDefault(client, null);
            authenticatedUsers.remove(client);
            clientAddresses.remove(client);
            client.close();
            logger.info("клиент {} отключен", address);
        } catch (IOException e) {
            logger.error("ошибка отключения клиента: {}", e.getMessage());
        }
    }

    private String dbUser;
    private String dbPassword;
}