package com.vehicleClient;

import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.managers.IdManager;
import com.vehicleShared.model.Vehicle;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;

public class Client {
    private final String host;
    private final int port;
    private Selector selector;
    private SocketChannel socketChannel;
    private ByteArrayOutputStream buffer;
    private boolean isRunning = true;
    private Scanner scanner;
    private int expectedLength = -1;
    private boolean waitingForResponse = false;
    private String lastArgument = null;
    private String lastCommand = null;
    private String login = null;
    private String password = null;
    private boolean authenticated = false;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
        this.buffer = new ByteArrayOutputStream();
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(host, port));
            socketChannel.register(selector, SelectionKey.OP_CONNECT);

            System.out.println("попытка подключения к серверу...");

            while (isRunning) {
                selector.select(100);
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) continue;

                    if (key.isConnectable()) {
                        connect(key);
                    } else if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    }
                }

                if (socketChannel.isConnected() && !waitingForResponse && !authenticated) {
                    promptAuth();
                } else if (socketChannel.isConnected() && !waitingForResponse) {
                    promptUserInput();
                }
            }
        } catch (IOException e) {
            System.err.println("ошибка клиента: " + e.getMessage());
        } finally {
            try {
                if (socketChannel != null) socketChannel.close();
                if (selector != null) selector.close();
            } catch (IOException ignored) {}
        }
        System.out.println("клиент завершил работу");
    }

    private void connect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.finishConnect()) {
            System.out.println("успешно подключен к серверу");
            key.interestOps(SelectionKey.OP_READ);
            key.attach(buffer);
        }
    }

    private void read(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteArrayOutputStream clientBuffer = (ByteArrayOutputStream) key.attachment();

        synchronized (clientBuffer) {
            try {
                ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
                int bytesRead = channel.read(byteBuffer);

                if (bytesRead == -1) {
                    System.out.println("сервер закрыл соединение");
                    reconnect();
                    return;
                }

                byteBuffer.flip();
                byte[] readData = new byte[byteBuffer.remaining()];
                byteBuffer.get(readData);
                clientBuffer.write(readData);
                System.out.println("прочитано байт: " + readData.length);

                byte[] data = clientBuffer.toByteArray();
                if (expectedLength == -1 && data.length >= 4) {
                    ByteBuffer lengthBuffer = ByteBuffer.wrap(data, 0, 4);
                    expectedLength = lengthBuffer.getInt();
                    System.out.println("ожидаемая длина: " + expectedLength);
                    clientBuffer.reset();
                    clientBuffer.write(data, 4, data.length - 4);
                }

                if (expectedLength != -1 && clientBuffer.size() >= expectedLength) {
                    byte[] responseData = new byte[expectedLength];
                    System.arraycopy(clientBuffer.toByteArray(), 0, responseData, 0, expectedLength);
                    System.out.println("десериализация ответа, длина: " + responseData.length);
                    Response response = deserialize(responseData);
                    System.out.println("ответ сервера: " + response.getMessage());

                    if (response.hasData()) {
                        System.out.println("данные: " + response.getData());
                    }

                    if (response.getException() != null) {
                        System.out.println("ошибка на сервере: " + response.getException().getMessage());
                    }

                    if (response.isSuccess() && (response.getMessage().contains("авторизация успешна") || response.getMessage().contains("регистрация успешна"))) {
                        authenticated = true;
                        System.out.println("авторизация прошла успешно, вводите команды!");
                    }

                    if (response.requiresVehicle()) {
                        System.out.println("для выполнения команды нужен объект vehicle");
                        Vehicle vehicle = CollectionManager.requestVehicleInformation(scanner, IdManager.getUnicId());
                        Request vehicleRequest = new Request(lastCommand, lastArgument, login, password);
                        vehicleRequest.setVehicle(vehicle);
                        key.attach(vehicleRequest);
                        key.interestOps(SelectionKey.OP_WRITE);
                        waitingForResponse = true;
                    } else {
                        key.attach(clientBuffer);
                        key.interestOps(SelectionKey.OP_READ);
                        waitingForResponse = false;
                        lastArgument = null;
                        lastCommand = null;
                    }
                    clientBuffer.reset();
                    expectedLength = -1;
                }
                key.interestOps(SelectionKey.OP_READ);
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("ошибка при чтении: " + e.getMessage());
                e.printStackTrace();
                reconnect();
            }
        }
    }

    private void reconnect() {
        try {
            if (socketChannel != null) socketChannel.close();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(host, port));
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            System.out.println("попытка переподключения...");
            buffer.reset();
            expectedLength = -1;
        } catch (IOException e) {
            System.err.println("не удалось переподключиться: " + e.getMessage());
            isRunning = false;
        }
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        Request request = (Request) key.attachment();

        byte[] data = serialize(request);
        System.out.println("отправка запроса, длина: " + data.length + ", команда: " + request.getCommand());

        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
        lengthBuffer.putInt(data.length);
        lengthBuffer.flip();
        synchronized (channel) {
            while (lengthBuffer.hasRemaining()) {
                channel.write(lengthBuffer);
            }
            ByteBuffer dataBuffer = ByteBuffer.wrap(data);
            while (dataBuffer.hasRemaining()) {
                channel.write(dataBuffer);
            }
        }

        key.interestOps(SelectionKey.OP_READ);
        key.attach(buffer);
        waitingForResponse = true;
    }

    private void promptAuth() {
        System.out.print("введите команду (login/register): ");
        if (scanner.hasNextLine()) {
            String command = scanner.nextLine().trim();
            if (!command.equals("login") && !command.equals("register")) {
                System.out.println("неверная команда, используйте 'login' или 'register'");
                return;
            }
            System.out.print("логин: ");
            login = scanner.nextLine().trim();
            System.out.print("пароль: ");
            password = scanner.nextLine().trim();

            Request request = new Request(command, null, login, password);
            try {
                SelectionKey key = socketChannel.keyFor(selector);
                key.attach(request);
                key.interestOps(SelectionKey.OP_WRITE);
                selector.wakeup();
            } catch (Exception e) {
                System.err.println("ошибка при отправке запроса: " + e.getMessage());
            }
        }
    }

    private void promptUserInput() {
        System.out.print("введите команду (help/show/insert/update/remove/exit): ");
        if (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                if (!waitingForResponse) {
                    System.out.println("команда не может быть пустой. введите 'help' для помощи");
                }
                return;
            }

            if ("exit".equalsIgnoreCase(input)) {
                isRunning = false;
                return;
            }

            String[] parts = input.split(" ", 2);
            String command = parts[0];
            String argument = parts.length > 1 ? parts[1] : null;

            if ("save".equalsIgnoreCase(command)) {
                System.out.println("нет доступа, используй серверную консоль, бебебе!");
                return;
            }

            lastArgument = argument;
            lastCommand = command;
            Request request = new Request(command, argument, login, password);
            try {
                SelectionKey key = socketChannel.keyFor(selector);
                key.attach(request);
                key.interestOps(SelectionKey.OP_WRITE);
                selector.wakeup();
            } catch (Exception e) {
                System.err.println("ошибка при отправке запроса: " + e.getMessage());
            }
        }
    }

    private byte[] serialize(Serializable object) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            oos.flush();
            return baos.toByteArray();
        }
    }

    private Response deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (Response) ois.readObject();
        }
    }
}