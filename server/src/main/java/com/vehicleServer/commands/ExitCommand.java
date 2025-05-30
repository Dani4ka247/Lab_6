package com.vehicleServer.commands;

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
        Response response = Response.success("сервер завершает работу");
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
        return "завершает работу сервера";
    }
}