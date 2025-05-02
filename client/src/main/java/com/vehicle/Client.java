package com.vehicle;

import com.vehicle.managers.CollectionManager;
import com.vehicle.model.Vehicle;
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

    private boolean interactWithServer(SocketChannel socketChannel) throws IOException {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Введите команду: ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                System.out.println("Команда не может быть пустой. Введите 'help' для помощи.");
                continue;
            }

            // Разделяем команду и аргумент
            String[] parts = input.split(" ", 2);
            String command = parts[0];
            String argument = parts.length > 1 ? parts[1] : null;

            // Отправляем запрос на сервер
            Request request = new Request(command, argument);
            sendRequest(socketChannel, request);

            // Получаем ответ от сервера
            Response response = receiveResponse(socketChannel);
            System.out.println("Ответ сервера: " + response.getMessage());

            // Если сервер требует объект, запрашиваем у пользователя
            if (response.requiresVehicle()) {
                System.out.println("Для выполнения команды нужен объект Vehicle.");

                // Используем CollectionManager.requestVehicleInformation для создания объекта
                Vehicle vehicle = CollectionManager.requestVehicleInformation(scanner, 0); // ID = 0 (например)

                // Отправляем новый запрос с объектом
                Request vehicleRequest = new Request("insert", argument);
                vehicleRequest.setVehicle(vehicle);
                sendRequest(socketChannel, vehicleRequest);

                // Получаем окончательный ответ
                Response finalResponse = receiveResponse(socketChannel);
                System.out.println("Ответ сервера: " + finalResponse.getMessage());
            }
        }
    }

    private void sendObject(SocketChannel socketChannel, Serializable object) throws IOException {
        // Сериализация объекта в массив байтов
        byte[] data = serialize(object);

        // Создаем буфер для отправки данных
        ByteBuffer buffer = ByteBuffer.wrap(data);
        socketChannel.write(buffer);
    }

    // Сериализация объекта в массив байтов
    private byte[] serialize(Serializable object) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            oos.flush();
            return baos.toByteArray();
        }
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
        // Создаем буфер для чтения
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        int bytesRead = socketChannel.read(buffer);

        // Проверяем, произошло ли закрытие соединения
        if (bytesRead == -1) {
            throw new EOFException("Соединение с сервером было закрыто.");
        }

        // Декодируем ответ из буфера
        buffer.flip(); // Переходим в режим чтения
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        // Десериализация ответа
        return deserialize(data);
    }

    // Десериализация байтов в объект
    private Response deserialize(byte[] data) throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (Response) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Ошибка при десериализации объекта", e);
        }
    }
}

