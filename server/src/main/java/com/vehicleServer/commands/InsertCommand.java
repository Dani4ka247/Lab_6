package com.vehicleServer.commands;

import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.model.Vehicle;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;

public class InsertCommand implements Command {
    private final CollectionManager collectionManager;

    public InsertCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        Vehicle vehicle = request.getVehicle();
        if (vehicle == null) {
            return Response.success("нужен объект vehicle", true);
        }
        try {
            if (collectionManager.addVehicle(vehicle, request.getLogin())) {
                return Response.success("vehicle добавлен");
            }
            return Response.error("ошибка добавления");
        } catch (Exception e) {
            return Response.error("ошибка: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "Добавляет новый элемент в коллекцию. Требуется указать аргумент.";
    }
}