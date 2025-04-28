package com.vehicle.serveNetwork;

import com.vehicle.model.Coordinates;
import com.vehicle.model.FuelType;
import com.vehicle.model.Vehicle;
import com.vehicle.model.VehicleType;
import com.vehicle.network.Request;
import com.vehicle.network.Response;
import com.vehicle.managers.CommandManager; // Менеджер команд
import com.vehicle.managers.CollectionManager; // Менеджер коллекции

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final int port;

    public Server(int port) {
        this.port = port;

        // Инициализируем менеджер коллекции и команд при старте сервера
        initializeManagers();
    }

    private void initializeManagers() {
        // Создаём экземпляр CollectionManager
        Vehicle vehicle1 = new Vehicle(1,new Coordinates(23F,33),"sd",22F, VehicleType.BOAT, FuelType.ELECTRICITY);
        Vehicle vehicle2 = new Vehicle(2,new Coordinates(23F,33),"sd",242F, VehicleType.BOAT, FuelType.ELECTRICITY);
        Vehicle vehicle3 = new Vehicle(3,new Coordinates(23F,33),"sd",2F, VehicleType.BOAT, FuelType.ELECTRICITY);

        CollectionManager collectionManager = new CollectionManager(); // Предполагается, что он есть в проекте
        collectionManager.put(123,vehicle1);
        collectionManager.put(22342,vehicle2);
        collectionManager.put(1,vehicle3);

        // Инициализируем CommandManager с данным CollectionManager
        CommandManager.initialize(collectionManager);

        System.out.println("CommandManager успешно инициализирован.");
    }

    public void start() {
        System.out.println("Сервер запускается...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер ожидает подключения на порту " + port);

            while (true) {
                // Принимаем новое подключение от клиента
                Socket clientSocket = serverSocket.accept();
                System.out.println("Клиент подключился: " + clientSocket.getInetAddress());

                // Обработка клиента в отдельном потоке
                new Thread(() -> processClient(clientSocket)).start();
            }

        } catch (Exception e) {
            System.err.println("Ошибка работы сервера: " + e.getMessage());
        }
    }

    private void processClient(Socket clientSocket) {
        try (ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream())) {

            while (true) {
                try {
                    // Читаем запрос от клиента
                    Request request = (Request) input.readObject();

                    if (request == null) {
                        System.out.println("Клиент завершил соединение.");
                        break;
                    }

                    System.out.println("Получена команда: " + request.getCommand());

                    // Выполняем команду через CommandManager
                    Response response = executeCommand(request);

                    // Отправляем ответ клиенту
                    output.writeObject(response);
                    output.flush();

                    // Если команда "exit", разрываем соединение
                    if ("exit".equalsIgnoreCase(request.getCommand())) {
                        System.out.println("Клиент запросил завершение работы.");
                        break;
                    }
                } catch (EOFException eofException) {
                    // Клиент закрыл соединение
                    System.out.println("Клиент закрыл соединение.");
                    break; // Выходим из цикла
                } catch (Exception readException) {
                    System.err.println("Ошибка при обработке клиента: " + readException.getMessage());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при обработке клиента: " + e.getMessage());
        } finally {
            try {
                // Закрываем соединение
                clientSocket.close();
                System.out.println("Соединение с клиентом закрыто.");
            } catch (Exception ex) {
                System.err.println("Ошибка при закрытии соединения: " + ex.getMessage());
            }
        }
    }

    private Response executeCommand(Request request) {
        try {
            // Делегируем выполнение команды CommandManager
            return CommandManager.executeRequest(request);
        } catch (Exception e) {
            // Возвращаем ответ об ошибке, если команда вызвала исключение
            return Response.serverError(e);
        }
    }
}