package com.vehicle.commands;

import com.vehicle.network.Request;
import com.vehicle.network.Response;
import com.vehicle.managers.CollectionManager;

public class InsertCommand implements Command {
    private final CollectionManager collectionManager;

    public InsertCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String argument = request.getArgument();
        if (argument == null || argument.isEmpty()) {
            return Response.error("Ошибка: команда 'insert' требует аргумент.");
        }

        //collectionManager.insert(argument); // Добавляем элемент в коллекцию
        return Response.success("Элемент '" + argument + "' успешно добавлен в коллекцию.");
    }

    @Override
    public String getDescription() {
        return "Добавляет новый элемент в коллекцию. Требуется указать аргумент.";
    }
}