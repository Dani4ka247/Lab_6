package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;

import java.util.List;
import java.util.stream.Collectors;

public class RemoveGreaterKey implements Command {
    private final CollectionManager collectionManager;

    public RemoveGreaterKey(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String argument = request.getArgument();
        if (argument == null || argument.isEmpty()) {
            return Response.error("Ошибка: команда 'remove_greater_key' требует указания ключа.");
        }

        try {
            int keyThreshold = Integer.parseInt(argument);
            List<Integer> keysToRemove = collectionManager.keySet()
                    .stream()
                    .filter(key -> key > keyThreshold)
                    .collect(Collectors.toList());

            if (keysToRemove.isEmpty()) {
                return Response.success("Не найдено элементов с ключом, превышающим " + keyThreshold + ".");
            }

            // удаляем элементы
            keysToRemove.forEach(collectionManager::remove);
            String removedKeys = String.join(", ", keysToRemove.stream().map(String::valueOf).collect(Collectors.toList()));
            return Response.success("Удалены элементы с ключами, превышающими " + keyThreshold + ": " + removedKeys + ".");
        } catch (NumberFormatException e) {
            return Response.error("Ошибка: ключ должен быть целым числом.");
        }
    }

    @Override
    public String getDescription() {
        return "Удаляет из коллекции все элементы, ключ которых превышает заданный.";
    }
}