package com.vehicleServer.commands;

import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import java.util.stream.Collectors;

public class ShowCommand implements Command {
    private final CollectionManager collectionManager;

    public ShowCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String result = collectionManager.isEmpty()
                ? "коллекция пуста"
                : collectionManager.entrySet()
                .stream()
                .map(entry -> entry.getKey() + " : " + entry.getValue().toString())
                .collect(Collectors.joining("\n"));
        return Response.success(result);
    }

    @Override
    public String getDescription() {
        return "Отображает все элементы коллекции текущего пользователя.";
    }
}