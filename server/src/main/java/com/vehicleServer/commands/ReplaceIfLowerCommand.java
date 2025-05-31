package com.vehicleServer.commands;

import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.model.Vehicle;

public class ReplaceIfLowerCommand implements Command {
    private final CollectionManager collectionManager;

    public ReplaceIfLowerCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String argument = request.getArgument();
        String userId = request.getLogin();
        if (argument == null || argument.isEmpty()) {
            return Response.error("нужен id");
        }
        Vehicle newVehicle = request.getVehicle();
        if (newVehicle == null) {
            return Response.success("нужен объект vehicle", true);
        }
        try {
            long id = Long.parseLong(argument);
            if (!collectionManager.containsKey(id)) {
                return Response.error("vehicle с id " + id + " не найден");
            }
            if (!collectionManager.getDbManager().canModify(id, userId)) {
                return Response.error("это не твой vehicle");
            }
            Vehicle oldVehicle = collectionManager.get(id);
            if (newVehicle.getPower() >= oldVehicle.getPower()) {
                return Response.success("новая мощность (" + newVehicle.getPower() + ") не меньше старой (" + oldVehicle.getPower() + ")");
            }
            if (collectionManager.update(id, newVehicle, userId)) {
                return Response.success("vehicle с id " + id + " заменён");
            }
            return Response.error("ошибка замены");
        } catch (NumberFormatException e) {
            return Response.error("id должен быть числом");
        } catch (Exception e) {
            return Response.error("ошибка: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "заменяет элемент с указанным id, если новая мощность меньше старой";
    }
}