package com.vehicleServer.commands;

import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.model.Vehicle;

public class UpdateCommand implements Command {
    private final CollectionManager collectionManager;

    public UpdateCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String argument = request.getArgument();
        String userId = request.getLogin();
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
            if (!collectionManager.getDbManager().canModify(id, userId)) {
                return Response.error("это не твой vehicle");
            }
            if (collectionManager.update(id, vehicle, userId)) {
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
        return "обновляет элемент коллекции с указанным id";
    }
}