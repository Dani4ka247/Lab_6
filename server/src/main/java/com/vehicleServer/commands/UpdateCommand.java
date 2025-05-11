package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.model.Vehicle;

import java.util.Map;

public class UpdateCommand implements Command {
    private final CollectionManager collectionManager;

    public UpdateCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String argument = request.getArgument();
        if (argument == null || argument.isEmpty()) {
            return Response.error("Ошибка: команда 'update' требует указания id.");
        }

        try {
            int id = Integer.parseInt(argument);
            // ищем ключ элемента с заданным id
            Integer key = collectionManager.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().getId() == id)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);



            if (key == null) {
                return Response.error("Ошибка: элемент с id " + id + " не найден.");
            }

            Vehicle newVehicle = request.getVehicle();
            if (newVehicle == null) {
                return new Response(true, "Серверу требуется объект Vehicle для обновления элемента с id " + id + ".", true);
            }

            collectionManager.replace(key, new Vehicle(Long.parseLong(argument),newVehicle));
            return Response.success("Элемент с id " + id + " успешно обновлен.");
        } catch (NumberFormatException e) {
            return Response.error("Ошибка: id должен быть целым числом.");
        }
    }

    @Override
    public String getDescription() {
        return "Обновляет элемент коллекции с указанным id.";
    }
}