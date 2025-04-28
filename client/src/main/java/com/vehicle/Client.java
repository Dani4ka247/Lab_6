package com.vehicle;

import com.vehicle.network.Request;
import com.vehicle.network.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private final String host;
    private final int port;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        System.out.println("Клиент запускается...");
        Scanner scanner = new Scanner(System.in);

        try (Socket serverSocket = new Socket(host, port)) {
            System.out.println("Подключение к серверу установлено.");

            // Поток для отправки запросов
            ObjectOutputStream output = new ObjectOutputStream(serverSocket.getOutputStream());
            // Поток для чтения ответов
            ObjectInputStream input = new ObjectInputStream(serverSocket.getInputStream());

            // Основной цикл для взаимодействия с сервером
            while (true) {
                System.out.print("Введите команду: ");
                String commandLine = scanner.nextLine().trim(); // Убираем пробелы в начале и в конце строки

                if (commandLine.isEmpty()) {
                    System.out.println("Пустая команда. Попробуйте снова.");
                    continue;
                }

                // Разбиение строки на команду и аргумент, если это возможно
                String[] parts = commandLine.split("\\s+", 2); // Сначала убираем лишние пробелы между словами
                String command = parts[0]; // Команда
                String argument = parts.length > 1 ? parts[1] : null; // Аргумент (может быть null)

                if (command.equalsIgnoreCase("exit")) {
                    System.out.println("Завершение работы клиента.");
                    break;
                }

                try {
                    // Создаём запрос
                    Request request = new Request(command, argument);

                    // Отправляем запрос на сервер
                    output.writeObject(request);
                    output.flush();

                    // Получаем ответ от сервера
                    Response response = (Response) input.readObject();
                    System.out.println("Ответ сервера: ");
                    System.out.println(response.getMessage());

                    if (response.hasData()) {
                        System.out.println("Дополнительные данные:");
                        response.getData().forEach(System.out::println);
                    }
                } catch (Exception ex) {
                    System.err.println("Ошибка при обработке команды: " + ex.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Ошибка работы клиента: " + e.getMessage());
        }
    }
}