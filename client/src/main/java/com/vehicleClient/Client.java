package com.vehicleClient;

import com.vehicleShared.model.*;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Client {
    private final String serverAddress;
    private final int serverPort;
    private String currentLogin;

    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void start() {
        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.connect(new InetSocketAddress(serverAddress, serverPort));
            System.out.println("Успешно подключен к серверу");
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Введите команду (login/register): ");
                String command = scanner.nextLine().trim();
                if (command.equals("exit")) {
                    break;
                }
                if (command.equals("login") || command.equals("register")) {
                    System.out.print("Логин: ");
                    String login = scanner.nextLine().trim();
                    System.out.print("Пароль: ");
                    String password = scanner.nextLine().trim();
                    Request request = new Request(command, null, login, password);
                    Response response = sendRequest(socketChannel, request);
                    System.out.println(response.getMessage());
                    if (response.isSuccess() && command.equals("login")) {
                        currentLogin = login;
                        handleAuthenticatedCommands(scanner, socketChannel);
                    }
                } else {
                    System.out.println("Неизвестная команда. Используйте login или register");
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка подключения: " + e.getMessage());
        }
    }

    private void handleAuthenticatedCommands(Scanner scanner, SocketChannel socketChannel) {
        while (true) {
            System.out.print("Введите команду (help/show/insert/update/remove/exit): ");
            String input = scanner.nextLine().trim();
            if (input.equals("exit")) {
                break;
            }
            String[] parts = input.split("\\s+", 2);
            String command = parts[0];
            String argument = parts.length > 1 ? parts[1] : null;
            Vehicle vehicle = null;
            if (command.equals("insert") || command.equals("update") || command.equals("replace_if_lower")) {
                if (argument == null) {
                    System.out.println("Нужен id");
                    continue;
                }
                vehicle = createVehicle(scanner, Long.parseLong(argument));
            }
            Request request = new Request(command, argument, currentLogin, null);
            request.setVehicle(vehicle);
            System.out.println("отправляем запрос: " + command);
            Response response = sendRequest(socketChannel, request);
            if (response.requiresVehicle()) {
                System.out.println("Введите данные для vehicle:");
                vehicle = createVehicle(scanner, argument != null ? Long.parseLong(argument) : 0);
                request = new Request(command, argument, currentLogin, null);
                request.setVehicle(vehicle);
                response = sendRequest(socketChannel, request);
            }
        }
    }

    private Vehicle createVehicle(Scanner scanner, long id) {
        System.out.print("Введите название машины: ");
        String name = scanner.nextLine().trim();
        System.out.print("Введите координаты (x,y): ");
        String[] coords = scanner.nextLine().trim().split(",");
        float x = Float.parseFloat(coords[0]);
        int y = Integer.parseInt(coords[1]);
        System.out.print("Введите мощность двигателя: ");
        float power = Float.parseFloat(scanner.nextLine().trim());
        System.out.print("Выберите тип машины (1:CAR, 2:BOAT, 3:HOVERBOARD): ");
        VehicleType type = VehicleType.values()[Integer.parseInt(scanner.nextLine().trim()) - 1];
        System.out.print("Выберите тип топлива (1:GASOLINE, 2:KEROSENE, 3:ELECTRICITY, 4:MANPOWER, 5:NUCLEAR): ");
        FuelType fuelType = FuelType.values()[Integer.parseInt(scanner.nextLine().trim()) - 1];
        return new Vehicle(id, new Coordinates(x, y), name, power, type, fuelType);
    }

    private Response sendRequest(SocketChannel socketChannel, Request request) {
        try {
            if (!socketChannel.isOpen() || !socketChannel.isConnected()) {
                System.out.println("sendRequest: канал закрыт");
                return Response.error("Канал закрыт");
            }
            socketChannel.socket().setSoTimeout(5000);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(request);
            }
            byte[] data = baos.toByteArray();
            ByteBuffer buffer = ByteBuffer.allocate(4 + data.length);
            buffer.putInt(data.length);
            buffer.put(data);
            buffer.flip();
            int totalBytesWritten = 0;
            while (buffer.hasRemaining()) {
                totalBytesWritten += socketChannel.write(buffer);
            }
            System.out.println("отправлен запрос: " + request.getCommand() + ", " + totalBytesWritten + " байт");

            ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
            int totalBytesRead = 0;
            while (totalBytesRead < 4) {
                int bytesRead = socketChannel.read(lengthBuffer);
                if (bytesRead == -1) {
                    System.out.println("sendRequest: сервер отключился");
                    return Response.error("Сервер отключился");
                }
                totalBytesRead += bytesRead;
            }
            lengthBuffer.flip();
            int length = lengthBuffer.getInt();
            if (length <= 0 || length > 1_000_000) {
                System.out.println("sendRequest: некорректная длина ответа: " + length);
                return Response.error("Некорректная длина ответа");
            }

            ByteBuffer dataBuffer = ByteBuffer.allocate(length);
            totalBytesRead = 0;
            while (totalBytesRead < length) {
                int bytesRead = socketChannel.read(dataBuffer);
                if (bytesRead == -1) {
                    System.out.println("sendRequest: сервер отключился при чтении данных");
                    return Response.error("Сервер отключился");
                }
                totalBytesRead += bytesRead;
            }
            dataBuffer.flip();
            byte[] responseData = new byte[length];
            dataBuffer.get(responseData);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(responseData);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                Response response = (Response) ois.readObject();
                System.out.println("получен ответ: \n" + response.getMessage());
                return response;
            }
        } catch (SocketTimeoutException e) {
            System.out.println("sendRequest: таймаут: " + e.getMessage());
            return Response.error("Таймаут связи с сервером");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("sendRequest: ошибка: " + e.getMessage());
            return Response.error("Ошибка связи: " + e.getMessage());
        }
    }
}