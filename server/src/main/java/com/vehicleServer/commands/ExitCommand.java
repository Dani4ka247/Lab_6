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
        CommandManager.executeRequest(new Request("save",null));

        Response response = Response.success("Сервер успешно завершил работу. Коллекция сохранена.");

        new Thread(() -> {
            try {
                Thread.sleep(100);
                System.exit(0);
            } catch (InterruptedException ignored) {}
        }).start();

        return response;
    }

    @Override
    public String getDescription() {
        return "Завершает работу сервера. Сохраняет текущую коллекцию.";
    }
}