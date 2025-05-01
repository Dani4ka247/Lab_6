package com.vehicle.serveNetwork;

import com.vehicle.network.Request;
import com.vehicle.network.Response;
import com.vehicle.managers.CommandManager;
import com.vehicle.managers.CollectionManager;

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
    }

    public void start() {
        System.out.println("Сервер запускается...");

        try (Selector selector = Selector.open();
             ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Сервер ожидает подключения на порту " + port);

            while (true) {
                selector.select(); // Ожидаем событий

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (key.isAcceptable()) {
                        acceptClient(serverChannel, selector);
                    } else if (key.isReadable()) {
                        processClient(key);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка работы сервера: " + e.getMessage());
        }
    }

    private void acceptClient(ServerSocketChannel serverChannel, Selector selector) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("Клиент подключился: " + clientChannel.getRemoteAddress());
    }

    private void processClient(SelectionKey key) {
        try {
            SocketChannel clientChannel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(8192); // Размер буфера (8 KB, может быть скорректирован)
            int bytesRead = clientChannel.read(buffer);

            // Проверяем, что данные были прочитаны
            if (bytesRead == -1) {
                System.out.println("Клиент отсоединился.");
                clientChannel.close();
                key.cancel();
                return;
            }

            buffer.flip();

            // Попытаемся десериализовать объект Request
            try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(buffer.array()))) {
                Request request = (Request) objectInputStream.readObject();
                System.out.println("Получен запрос: " + request);

                // Выполняем команду
                Response response = CommandManager.executeRequest(request);

                // Отправляем клиенту ответ
                sendResponse(clientChannel, response);
            } catch (ClassNotFoundException | InvalidClassException e) {
                System.err.println("Ошибка десериализации объекта: " + e.getMessage());
                sendResponse(clientChannel, Response.error("Некорректные данные запроса."));
            }
        } catch (IOException e) {
            System.err.println("Ошибка при обработке клиента: " + e.getMessage());
        }
    }

    private void sendResponse(SocketChannel clientChannel, Response response) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutputStream objectOut = new ObjectOutputStream(byteOut)) {

            objectOut.writeObject(response);
            objectOut.flush();

            byte[] responseData = byteOut.toByteArray();
            ByteBuffer buffer = ByteBuffer.wrap(responseData);

            while (buffer.hasRemaining()) {
                clientChannel.write(buffer);
            }
        } catch (IOException e) {
            System.err.println("Ошибка отправки ответа клиенту: " + e.getMessage());
        }
    }
}