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
        if (argument == null || argument.isEmpty()) {
            return Response.error("нужен id");
        }
        try {
            long id = Long.parseLong(argument);
            if (!collectionManager.containsKey(id)) {
                return Response.error("vehicle с id " + id + " не найден");
            }
            if (!collectionManager.canModify(id, request.getLogin())) {
                return Response.error("это не твой vehicle");
            }
            if (collectionManager.removeVehicle(id, request.getLogin())) {
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
        return "Удаляет элемент из коллекции по указанному ключу.";
    }
}