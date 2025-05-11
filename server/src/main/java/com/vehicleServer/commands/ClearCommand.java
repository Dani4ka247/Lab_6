package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;

public class ClearCommand implements Command {
    private final CollectionManager collectionManager;

    public ClearCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        if (collectionManager.isEmpty()) {
            return Response.success("Коллекция уже пуста.");
        }

        collectionManager.clear();
        return Response.success("Коллекция успешно очищена.");
    }

    @Override
    public String getDescription() {
        return "Очищает коллекцию, удаляя все элементы.";
    }
}