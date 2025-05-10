package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;

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