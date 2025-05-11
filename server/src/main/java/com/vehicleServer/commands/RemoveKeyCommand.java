package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;

public class RemoveKeyCommand implements Command {
    private final CollectionManager collectionManager;

    public RemoveKeyCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String argument = request.getArgument();
        if (argument == null || argument.isEmpty()) {
            return Response.error("Ошибка: команда 'remove_key' требует указания ключа.");
        }

        try {
            int key = Integer.parseInt(argument);
            if (!collectionManager.containsKey(key)) {
                return Response.error("Ошибка: ключ " + key + " не найден в коллекции.");
            }

            collectionManager.remove(key);
            return Response.success("Элемент с ключом " + key + " удален.");
        } catch (NumberFormatException e) {
            return Response.error("Ошибка: ключ должен быть целым числом.");
        }
    }

    @Override
    public String getDescription() {
        return "Удаляет элемент из коллекции по указанному ключу.";
    }
}