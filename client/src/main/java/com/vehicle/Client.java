package com.vehicle;

import com.vehicle.network.Request;
import com.vehicle.network.Response;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Client {
    private final String host;
    private final int port;
    private boolean isConnected = false;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        // Флаг для контроля работы программы
        boolean isRunning = true;

        while (isRunning) {
            // Попытка установить соединение
            try (SocketChannel socketChannel = SocketChannel.open()) {
                socketChannel.connect(new InetSocketAddress(host, port));
                isConnected = true;
                System.out.println("Успешно подключен к серверу.");

                // Начинаем взаимодействие
                isRunning = interactWithServer(socketChannel); // Управляем завершением программы внутри метода
            } catch (IOException e) {
                System.err.println("Ошибка подключения к серверу: " + e.getMessage());
                isConnected = false;

                // Завершаем программу при ручном выходе (иначе пытаемся подключиться снова)
                if (!isRunning) {
                    break;
                }

                // Попытка переподключиться через несколько секунд
                System.out.println("Попытка повторного подключения через 5 секунд...");
                try {
                    Thread.sleep(5000); // Задержка между попытками подключения
                } catch (InterruptedException ie) {
                    System.err.println("Поток клиента прерван.");
                    break; // Завершаем программу, если поток был прерван
                }
            }
        }

        System.out.println("Клиент завершил свою работу.");
    }

    private boolean interactWithServer(SocketChannel socketChannel) {
        Scanner scanner = new Scanner(System.in);

        while (isConnected) {
            try {
                System.out.print("Введите команду (или 'exit' для выхода): ");
                String command = scanner.nextLine();

                if ("exit".equalsIgnoreCase(command)) {
                    System.out.println("Завершение работы клиента...");
                    return false; // Устанавливаем флаг завершения
                }

                Request request = new Request(command, null); // Аргументы можно добавить
                sendRequest(socketChannel, request);

                Response response = receiveResponse(socketChannel);

                if (response == null) {
                    System.err.println("Ошибка: сервер не отправил ответ.");
                    break;
                }

                System.out.println("Ответ от сервера: " + response.getMessage());
            } catch (IOException e) {
                System.err.println("Сервер разорвал соединение. Попытка подключения...");
                isConnected = false; // Соединение разорвано, выходим из внутреннего цикла
            }
        }
        return true; // По умолчанию продолжаем основную работу
    }

    private void sendRequest(SocketChannel socketChannel, Request request) throws IOException {
        if (!socketChannel.isConnected() || request == null) {
            throw new IOException("Сокет не подключен или некорректный запрос.");
        }

        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutputStream objectOut = new ObjectOutputStream(byteOut)) {

            objectOut.writeObject(request);
            objectOut.flush();

            ByteBuffer buffer = ByteBuffer.wrap(byteOut.toByteArray());
            while (buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }
        }
    }

    private Response receiveResponse(SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192); // Размер буфера 8 KB (или больше, если нужно)

        int bytesRead = socketChannel.read(buffer);
        if (bytesRead == -1) {
            throw new IOException("Соединение с сервером разорвано.");
        }

        buffer.flip();

        try (ObjectInputStream objectInputStream = new ObjectInputStream(
                new ByteArrayInputStream(buffer.array(), 0, bytesRead))) {
            return (Response) objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Не удалось десериализовать ответ от сервера.", e);
        }
    }
}