package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.model.Vehicle;

public class SumOfPower implements Command {
    private final CollectionManager collectionManager;

    public SumOfPower(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        if (collectionManager.isEmpty()) {
            return Response.success("Коллекция пуста, сумма мощности: 0.");
        }

        float powerSum = collectionManager.values()
                .stream()
                .map(Vehicle::getPower)
                .reduce(0f, Float::sum);
        return Response.success("Общая мощность : " + powerSum);
    }

    @Override
    public String getDescription() {
        return "Выводит сумму значений поля enginePower для всех элементов коллекции.";
    }
}