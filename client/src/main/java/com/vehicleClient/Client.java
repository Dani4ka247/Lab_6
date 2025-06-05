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

import static com.vehicleShared.managers.CollectionManager.requestVehicleInformation;

public class Client {
    private final String serverAddress;
    private final int serverPort;
    private String currentLogin;
    private String currentPassword;

    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void start() {
        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.connect(new InetSocketAddress(serverAddress, serverPort));
            Scanner scanner = new Scanner(System.in);
            System.out.print("Введите login/register/exit: ");
            while (true) {
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
                    System.out.println("Ответ сервера: " + response.getMessage());
                    if (response.isSuccess() && command.equals("login")) {
                        currentLogin = login;
                        currentPassword = password;
                        handleAuthenticatedCommands(scanner, socketChannel);
                    }
                } else {
                    System.out.println("Ответ сервера: используйте login или register");
                    System.out.print("Введите login/register/exit: ");
                }
            }
        } catch (IOException e) {
            System.out.println("ошибка подключения: " + e.getMessage());
        }
    }

    private void handleAuthenticatedCommands(Scanner scanner, SocketChannel socketChannel) {
        System.out.print("Введите команду: ");
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equals("exit")) {
                System.out.print("Введите login/register/exit: ");
                break;
            }
            String[] parts = input.split("\\s+", 2);
            String command = parts[0];
            String argument = parts.length > 1 ? parts[1] : null;
            Vehicle vehicle = null;
            if (command.equals("update") || command.equals("replace_if_lower")) {
                if (argument == null) {
                    System.out.println("Ответ сервера: нужен id");
                    System.out.print("Введите команду: ");
                    continue;
                }
                vehicle = requestVehicleInformation(scanner, Long.parseLong(argument));
            } else if (command.equals("insert")) {
                vehicle = requestVehicleInformation(scanner);
            }
            Request request = new Request(command, argument, currentLogin, currentPassword);
            request.setVehicle(vehicle);
            Response response = sendRequest(socketChannel, request);
            System.out.println("Ответ сервера: " + response.getMessage());
            System.out.print("Введите команду: ");
        }
    }

    private Response sendRequest(SocketChannel socketChannel, Request request) {
        try {
            if (!socketChannel.isOpen() || !socketChannel.isConnected()) {
                return Response.error("канал закрыт");
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
            while (buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }

            ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
            int totalBytesRead = 0;
            while (totalBytesRead < 4) {
                int bytesRead = socketChannel.read(lengthBuffer);
                if (bytesRead == -1) {
                    return Response.error("сервер отключился");
                }
                totalBytesRead += bytesRead;
            }
            lengthBuffer.flip();
            int length = lengthBuffer.getInt();
            if (length <= 0 || length > 1_000_000) {
                return Response.error("некорректная длина ответа");
            }

            ByteBuffer dataBuffer = ByteBuffer.allocate(length);
            totalBytesRead = 0;
            while (totalBytesRead < length) {
                int bytesRead = socketChannel.read(dataBuffer);
                if (bytesRead == -1) {
                    return Response.error("сервер отключился");
                }
                totalBytesRead += bytesRead;
            }
            dataBuffer.flip();
            byte[] responseData = new byte[length];
            dataBuffer.get(responseData);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(responseData);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                return (Response) ois.readObject();
            }
        } catch (SocketTimeoutException e) {
            return Response.error("таймаут связи");
        } catch (IOException | ClassNotFoundException e) {
            return Response.error("ошибка связи: " + e.getMessage());
        }
    }
}