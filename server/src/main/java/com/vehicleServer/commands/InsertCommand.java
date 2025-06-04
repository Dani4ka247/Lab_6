package com.vehicleServer.commands;

import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.model.Vehicle;

public class InsertCommand implements Command {
    private final CollectionManager collectionManager;

    public InsertCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String argument = request.getArgument();
        String userId = request.getLogin();
        Vehicle vehicle = request.getVehicle();
        if (vehicle == null) {
            return Response.success("нужен объект vehicle", true);
        }
        if (argument == null || argument.isEmpty()) {
            return Response.error("нужен id");
        }
        try {
            long id = Long.parseLong(argument);
            vehicle.setId(id);
            collectionManager.put(id, vehicle, userId);
            return Response.success("vehicle добавлен");
        } catch (NumberFormatException e) {
            return Response.error("id должен быть числом");
        } catch (Exception e) {
            return Response.error("ошибка: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "добавляет новый элемент в коллекцию с указанным id";
    }
}