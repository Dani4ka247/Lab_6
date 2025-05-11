package com.vehicleServer.commands;

import com.vehicleShared.managers.IdManager;
import com.vehicleShared.model.Vehicle;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;

public class InsertCommand implements Command {
    private final CollectionManager collectionManager;

    public InsertCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String argument = request.getArgument();
        if (argument == null || argument.isEmpty()) {
            return Response.error("Ошибка: команда 'insert' требует указания ключа.");
        }

        Vehicle vehicle = request.getVehicle();
        if (vehicle == null) {
            return new Response(true,
                    "Серверу требуется объект Vehicle для завершения команды.", true);
        }

        collectionManager.put(Integer.parseInt(argument), new Vehicle(IdManager.getUnicId(),vehicle));

        return Response.success("Объект успешно добавлен в коллекцию.");
    }

    @Override
    public String getDescription() {
        return "Добавляет новый элемент в коллекцию. Требуется указать аргумент.";
    }
}