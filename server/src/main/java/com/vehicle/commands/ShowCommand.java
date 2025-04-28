package com.vehicle.commands;

import com.vehicle.network.Request;
import com.vehicle.network.Response;
import com.vehicle.managers.CollectionManager;

public class ShowCommand implements Command {
    private final CollectionManager collectionManager;

    public ShowCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        return Response.success(collectionManager.getSortedVehiclesByPower());
    }

    @Override
    public String getDescription() {
        return "Отображает все элементы коллекции в порядке возрастания мощности.";
    }
}