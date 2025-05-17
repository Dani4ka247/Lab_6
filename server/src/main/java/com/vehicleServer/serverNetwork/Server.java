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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private final int port;
    private CollectionManager collectionManager;
    private final Map<SelectionKey, ByteArrayOutputStream> clientBuffers = new HashMap<>();
    private final Map<SelectionKey, Integer> expectedLengths = new HashMap<>();

    public Server(int port) {
        this.port = port;
        initializeManagers();
    }

    private void initializeManagers() {
        collectionManager = new CollectionManager();
        CommandManager.initialize(collectionManager, logger);
        CommandManager.executeRequest(new Request("load", null));
    }

    public void start() {
        logger.info("сервер инициализирован. запуск...");

        // запуск потока для консольного ввода
        Thread consoleThread = new Thread(this::handleConsoleInput);
        consoleThread.setDaemon(true);
        consoleThread.start();

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            File logDir = new File("logs");
            if (!logDir.exists()) {
                boolean created = logDir.mkdirs();
                if (created) {
                    logger.info("Создана папка для логов: {}", logDir.getAbsolutePath());
                } else {
                    logger.warn("Не удалось создать папку для логов");
                }
            }
            Selector selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            logger.info("сервер запущен и ожидает подключения на порту {}", port);
            while (true) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        acceptClient(serverChannel, selector);
                    } else if (key.isReadable()) {
                        processClient(key, selector);
                    } else if (key.isWritable()) {
                        sendPendingResponse(key);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("ошибка при запуске сервера: {}", e.getMessage());
        }
    }

    private void handleConsoleInput() {
        Scanner scanner = new Scanner(System.in);
        logger.info("консоль сервера готова. доступные команды: save, shutdown, exit, info, show");

        while (true) {
            System.out.print("сервер> ");
            if (scanner.hasNextLine()) {
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    System.out.println("введите команду (save, shutdown, exit, info, show)");
                    continue;
                }

                String[] parts = input.split(" ", 2);
                String command = parts[0];
                String argument = parts.length > 1 ? parts[1] : null;

                if ("exit".equalsIgnoreCase(command)) {
                    logger.info("сервер завершает работу через консоль");
                    System.exit(0);
                }

                Response response = CommandManager.executeRequest(new Request(command,argument));
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
        SelectionKey key = clientChannel.register(selector, SelectionKey.OP_READ);
        clientBuffers.put(key, new ByteArrayOutputStream());
        expectedLengths.put(key, -1);
        logger.info("клиент подключился: {}", clientChannel.getRemoteAddress());
    }

    private void processClient(SelectionKey key, Selector selector) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        try {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            int bytesRead = clientChannel.read(buffer);

            if (bytesRead == -1) {
                clientChannel.close();
                clientBuffers.remove(key);
                expectedLengths.remove(key);
                logger.info("клиент отключился: {}", clientChannel.getRemoteAddress());
                return;
            }

            ByteArrayOutputStream clientBuffer = clientBuffers.get(key);
            buffer.flip();
            clientBuffer.write(buffer.array(), buffer.position(), buffer.remaining());

            Integer expectedLength = expectedLengths.get(key);
            if (expectedLength == -1 && clientBuffer.size() >= 4) {
                byte[] data = clientBuffer.toByteArray();
                ByteBuffer lengthBuffer = ByteBuffer.wrap(data, 0, 4);
                expectedLength = lengthBuffer.getInt();
                expectedLengths.put(key, expectedLength);
                clientBuffer.reset();
                clientBuffer.write(data, 4, data.length - 4);
            }

            if (expectedLength != -1 && clientBuffer.size() >= expectedLength) {
                Request request = deserialize(clientBuffer.toByteArray());
                logger.info("получен запрос: {}", request);

                Response response = CommandManager.executeRequest(request);
                if (request.getCommand().equals("shutdown")) {
                    notifyAllClients(selector, response);
                    logger.info("сервер завершает работу");
                    System.exit(0);
                }

                key.attach(response);
                key.interestOps(SelectionKey.OP_WRITE);

                clientBuffer.reset();
                expectedLengths.put(key, -1);
            }
        } catch (IOException | ClassNotFoundException e) {
            logger.error("ошибка при обработке клиента");
            try {
                clientChannel.close();
                clientBuffers.remove(key);
                expectedLengths.remove(key);
            } catch (IOException ignored) {}
        }
    }

    private void notifyAllClients(Selector selector, Response exitResponse) {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                SocketChannel clientChannel = (SocketChannel) key.channel();
                try {
                    byte[] data = serialize(exitResponse);
                    ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
                    lengthBuffer.putInt(data.length);
                    lengthBuffer.flip();
                    clientChannel.write(lengthBuffer);
                    ByteBuffer dataBuffer = ByteBuffer.wrap(data);
                    clientChannel.write(dataBuffer);
                    logger.info("уведомление отправлено клиенту");
                } catch (IOException e) {
                    logger.error("ошибка отправки уведомления клиенту");
                }
            }
        }
    }

    private void sendPendingResponse(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        Response response = (Response) key.attachment();

        try {
            byte[] data = serialize(response);
            ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
            lengthBuffer.putInt(data.length);
            lengthBuffer.flip();
            clientChannel.write(lengthBuffer);
            ByteBuffer dataBuffer = ByteBuffer.wrap(data);
            clientChannel.write(dataBuffer);
            logger.info("отправлен ответ клиенту {}: {}", clientChannel.getRemoteAddress(), response);
            key.attach(null);
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            try {
                logger.error("ошибка при отправке ответа клиенту {}: {}", clientChannel.getRemoteAddress(), e.getMessage());
                clientChannel.close();
                clientBuffers.remove(key);
                expectedLengths.remove(key);
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