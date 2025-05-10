package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;

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