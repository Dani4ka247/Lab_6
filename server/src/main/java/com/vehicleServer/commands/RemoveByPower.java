package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RemoveByPower implements Command {
    private final CollectionManager collectionManager;

    public RemoveByPower(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String argument = request.getArgument();
        if (argument == null || argument.isEmpty()) {
            return Response.error("Ошибка: команда 'remove_by_power' требует указания мощности.");
        }

        try {
            float power = Float.parseFloat(argument);
            List<Integer> keysToRemove = collectionManager.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().getPower() == power)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (keysToRemove.isEmpty()) {
                return Response.success("Не найдено элементов с мощностью " + power + ".");
            }

            keysToRemove.forEach(collectionManager::remove);
            String removedKeys = String.join(", ", keysToRemove.stream().map(String::valueOf).collect(Collectors.toList()));
            return Response.success("Удалены элементы с мощностью " + power + ", ключи: " + removedKeys + ".");
        } catch (NumberFormatException e) {
            return Response.error("Ошибка: мощность должна быть числом.");
        }
    }

    @Override
    public String getDescription() {
        return "Удаляет из коллекции все элементы, у которых enginePower равен заданному значению.";
    }
}