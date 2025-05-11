package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.model.Vehicle;

import java.util.Map;

public class ReplaceIfLowerCommand implements Command {
    private final CollectionManager collectionManager;

    public ReplaceIfLowerCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String argument = request.getArgument();
        if (argument == null || argument.isEmpty()) {
            return Response.error("Ошибка: команда 'replace_if_lower' требует указания id.");
        }

        try {
            int id = Integer.parseInt(argument);
            // ищем ключ элемента с заданным id
            Map.Entry<Integer, Vehicle> targetEntry = collectionManager.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().getId() == id)
                    .findFirst()
                    .orElse(null);

            if (targetEntry == null) {
                return Response.error("Ошибка: элемент с id " + id + " не найден.");
            }

            Vehicle newVehicle = request.getVehicle();
            if (newVehicle == null) {
                return new Response(true, "Серверу требуется объект Vehicle для замены элемента с id " + id + ".", true);
            }

            float oldPower = targetEntry.getValue().getPower();
            float newPower = newVehicle.getPower();
            if (newPower >= oldPower) {
                return Response.success("Новый элемент с мощностью " + newPower + " не меньше старого (" + oldPower + "). Замена не выполнена.");
            }

            collectionManager.put(targetEntry.getKey(), new Vehicle(Long.parseLong(argument),newVehicle));
            return Response.success("Элемент с id " + id + " заменен, так как новая мощность (" + newPower + ") меньше старой (" + oldPower + ").");
        } catch (NumberFormatException e) {
            return Response.error("Ошибка: id должен быть целым числом.");
        }
    }

    @Override
    public String getDescription() {
        return "Заменяет элемент с указанным id, если новая мощность меньше старой.";
    }
}