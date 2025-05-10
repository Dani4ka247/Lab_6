package com.vehicleServer.serveNetwork;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleServer.managers.CommandManager;
import com.vehicleShared.managers.CollectionManager;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class Server {
    private final int port;
    private CollectionManager collectionManager;

    public Server(int port) {
        this.port = port;
        initializeManagers();

    }

    private void initializeManagers() {
        collectionManager = new CollectionManager();
        CommandManager.initialize(collectionManager);
        CommandManager.executeRequest(new Request("load", null));
    }

    public void start() {
        System.out.println("Сервер инициализирован. Запуск...");//todo потоки а не каналы

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);

            Selector selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Сервер запущен и ожидает подключения на порту " + port);

            while (true) {
                selector.select(); // Ожидание событий
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        acceptClient(serverChannel, selector);
                    } else if (key.isReadable()) {
                        processClient(key);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка при запуске сервера: " + e.getMessage());
        }
    }

    private void acceptClient(ServerSocketChannel serverChannel, Selector selector) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("Клиент подключился: " + clientChannel.getRemoteAddress());
    }

    private void processClient(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        try {
            // Создаем буфер для чтения данных
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            int bytesRead = clientChannel.read(buffer);

            // Проверяем закрытие соединения
            if (bytesRead == -1) {
                clientChannel.close();
                System.out.println("Клиент отключился.");
                return;
            }

            // Декодируем запрос из буфера
            buffer.flip(); // Переходим в режим чтения
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            // Десериализация запроса
            Request request = deserialize(data);
            System.out.println("Получен запрос от клиента: " + request);

            // Создаем ответ
            Response response = Response.success("Запрос обработан успешно");
            sendResponse(clientChannel, CommandManager.executeRequest(request));

        } catch (IOException e) {
            e.printStackTrace();
            try {
                clientChannel.close();
            } catch (IOException ignored) {
            }
        }
    }

    // Десериализация байтов в объект
    private Request deserialize(byte[] data) throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (Request) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Ошибка при десериализации объекта", e);
        }
    }

    private void sendResponse(SocketChannel clientChannel, Response response) throws IOException {
        // Сериализация ответа в массив байтов
        byte[] data = serialize(response);

        // Создаем буфер и пишем данные в канал
        ByteBuffer buffer = ByteBuffer.wrap(data); // Оборачиваем данные в буфер
        clientChannel.write(buffer);
    }

    // Сериализация объекта в массив байтов
    private byte[] serialize(Response response) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(response);
            oos.flush();
            return baos.toByteArray();
        }
    }
}