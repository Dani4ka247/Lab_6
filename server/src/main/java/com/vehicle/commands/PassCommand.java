package com.vehicle.commands;

import com.vehicle.network.Request;
import com.vehicle.network.Response;

public class PassCommand implements Command {

    @Override
    public Response execute(Request request) {
        return Response.success("Команда не указана. Ничего не выполняется.");
    }

    @Override
    public String getDescription() {
        return "Ничего не делает. Заглушка для пустого ввода.";
    }
}