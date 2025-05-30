package com.vehicleServer.commands;

import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.model.Vehicle;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;

public class UpdateCommand implements Command {
    private final CollectionManager collectionManager;

    public UpdateCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String argument = request.getArgument();
        if (argument == null || argument.isEmpty()) {
            return Response.error("нужен id");
        }
        Vehicle vehicle = request.getVehicle();
        if (vehicle == null) {
            return Response.success("нужен объект vehicle", true);
        }
        try {
            long id = Long.parseLong(argument);
            if (!collectionManager.containsKey(id)) {
                return Response.error("vehicle с id " + id + " не найден");
            }
            if (!collectionManager.canModify(id, request.getLogin())) {
                return Response.error("это не твой vehicle");
            }
            if (collectionManager.updateVehicle(id, vehicle, request.getLogin())) {
                return Response.success("vehicle обновлён");
            }
            return Response.error("ошибка обновления");
        } catch (NumberFormatException e) {
            return Response.error("id должен быть числом");
        } catch (Exception e) {
            return Response.error("ошибка: " + e.getMessage());
        }
    }
    @Override
    public String getDescription() {
        return "Обновляет элемент коллекции с указанным id.";
    }
}