package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;

import java.sql.SQLException;

public class ShowByPower implements Command {
    private final CollectionManager collectionManager;

    public ShowByPower(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        try {
            collectionManager.getDbManager().loadFromDb(request.getLogin(),true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        String argument = request.getArgument();
        if (argument == null || argument.isEmpty()) {
            return Response.error("Ошибка: команда 'show_by_power' требует указания мощности.");
        }

        try {
            float minPower = Float.parseFloat(argument);
            String result = collectionManager.getVehiclesByMinPower(minPower);
            if (result.isEmpty()) {
                return Response.success("Не найдено элементов с мощностью, большей или равной " + minPower + ".");
            }
            return Response.success(result);
        } catch (NumberFormatException e) {
            return Response.error("Ошибка: мощность должна быть числом.");
        }
    }

    @Override
    public String getDescription() {
        return "Выводит элементы, у которых enginePower больше или равен заданному значению.";
    }
}