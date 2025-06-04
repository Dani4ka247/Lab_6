package com.vehicleServer.commands;

import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;

import java.sql.SQLException;
import java.util.stream.Collectors;

public class ShowCommand implements Command {
    private final CollectionManager collectionManager;

    public ShowCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String userId = request.getLogin();
        try {
            collectionManager.loadFromDb(userId); // Загружаем только машины пользователя
            String result = collectionManager.isEmpty()
                    ? "ваша коллекция пуста"
                    : collectionManager.entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + " : " + entry.getValue().toString())
                    .collect(Collectors.joining("\n"));
            return Response.success(result);
        } catch (SQLException e) {
            return Response.error("ошибка загрузки коллекции: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "отображает все элементы коллекции текущего пользователя";
    }
}