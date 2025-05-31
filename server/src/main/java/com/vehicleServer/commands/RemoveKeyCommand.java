package com.vehicleServer.commands;

import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;

public class RemoveKeyCommand implements Command {
    private final CollectionManager collectionManager;

    public RemoveKeyCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String argument = request.getArgument();
        String userId = request.getLogin();
        if (argument == null || argument.isEmpty()) {
            return Response.error("нужен id");
        }
        try {
            long id = Long.parseLong(argument);
            if (!collectionManager.containsKey(id)) {
                return Response.error("vehicle с id " + id + " не найден");
            }
            if (!collectionManager.getDbManager().canModify(id, userId)) {
                return Response.error("это не твой vehicle");
            }
            if (collectionManager.remove(id, userId) != null) {
                return Response.success("vehicle удалён");
            }
            return Response.error("ошибка удаления");
        } catch (NumberFormatException e) {
            return Response.error("id должен быть числом");
        } catch (Exception e) {
            return Response.error("ошибка: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "удаляет элемент из коллекции по указанному ключу";
    }
}