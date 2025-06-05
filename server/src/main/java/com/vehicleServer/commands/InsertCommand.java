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
        String userId = request.getLogin();
        Vehicle vehicle = request.getVehicle();
        if (vehicle == null) {
            return Response.success("нужен объект vehicle", true);
        }
        try {
            if (collectionManager.put(vehicle, userId)) {
                return Response.success("vehicle добавлен, id=" + vehicle.getId());
            } else {
                return Response.error("ошибка добавления: невалидные данные");
            }
        } catch (Exception e) {
            return Response.error("ошибка: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "добавляет новый элемент в коллекцию";
    }
}