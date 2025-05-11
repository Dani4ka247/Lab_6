package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;

import java.time.format.DateTimeFormatter;

public class InfoCommand implements Command {
    private final CollectionManager collectionManager;

    public InfoCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String info = String.format(
                "Тип коллекции: %s\nДата инициализации: %s\nКоличество элементов: %d",
                collectionManager.getClass().getSimpleName(),
                collectionManager.initializationDate.format(DateTimeFormatter.ofPattern("yyyy_MM_dd HH:mm")),
                collectionManager.size()
        );
        return Response.success(info);
    }

    @Override
    public String getDescription() {
        return "Выводит информацию о коллекции: тип, дату инициализации, количество элементов.";
    }
}