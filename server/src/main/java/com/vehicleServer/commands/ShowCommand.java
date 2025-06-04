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
        String userId = request.getLogin();
        if (userId == null || userId.isEmpty()) {
            return Response.error("требуется авторизация");
        }
        String result = collectionManager.isEmpty()
                ? "ваша коллекция пуста"
                : collectionManager.entrySet()
                .stream()
                .map(entry -> entry.getKey() + " : " + entry.getValue().toString())
                .collect(Collectors.joining("\n"));
        return Response.success(result);
    }

    @Override
    public String getDescription() {
        return "отображает все элементы коллекции текущего пользователя";
    }
}