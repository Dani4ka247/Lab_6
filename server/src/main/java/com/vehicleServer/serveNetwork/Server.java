package com.vehicleServer.serveNetwork;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleServer.managers.CommandManager;
import com.vehicleShared.managers.CollectionManager;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Server {
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
        CommandManager.initialize(collectionManager);
        CommandManager.executeRequest(new Request("load", null));
    }

    public void start() {
        System.out.println("сервер инициализирован. запуск...");

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);

            Selector selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("сервер запущен и ожидает подключения на порту " + port);

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
                        processClient(key);
                    } else if (key.isWritable()) {
                        sendPendingResponse(key);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("ошибка при запуске сервера: " + e.getMessage());
        }
    }

    private void acceptClient(ServerSocketChannel serverChannel, Selector selector) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        SelectionKey key = clientChannel.register(selector, SelectionKey.OP_READ);
        clientBuffers.put(key, new ByteArrayOutputStream());
        expectedLengths.put(key, -1);
        System.out.println("клиент подключился: " + clientChannel.getRemoteAddress());
    }

    private void processClient(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        try {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            int bytesRead = clientChannel.read(buffer);

            if (bytesRead == -1) {
                clientChannel.close();
                clientBuffers.remove(key);
                expectedLengths.remove(key);
                System.out.println("клиент отключился");
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
                System.out.println("получен запрос: " + request);

                Response response = CommandManager.executeRequest(request);
                key.attach(response);
                key.interestOps(SelectionKey.OP_WRITE);

                clientBuffer.reset();
                expectedLengths.put(key, -1);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("ошибка при обработке клиента: " + e.getMessage());
            try {
                clientChannel.close();
                clientBuffers.remove(key);
                expectedLengths.remove(key);
            } catch (IOException ignored) {}
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
            key.attach(null);
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            System.err.println("ошибка при отправке ответа: " + e.getMessage());
            try {
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