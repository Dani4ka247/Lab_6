package com.vehicleServer.commands;

import com.vehicleServer.managers.CommandManager;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;

public class ExitCommand implements Command {
    private final CollectionManager collectionManager;

    public ExitCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        // Здесь сохраняем коллекцию, если это необходимо
        CommandManager.executeRequest(new Request("save",null));

        // Отправляем клиенту сообщение о завершении работы сервера
        Response response = Response.success("Сервер успешно завершил работу. Коллекция сохранена.");

        // Запускаем завершение работы сервера в отдельном потоке
        new Thread(() -> {
            try {
                Thread.sleep(100); // Ждем, чтобы ответ клиенту успел отправиться
                System.exit(0); // Завершаем программу
            } catch (InterruptedException ignored) {}
        }).start();

        return response;
    }

    @Override
    public String getDescription() {
        return "Завершает работу сервера. Сохраняет текущую коллекцию.";
    }
}