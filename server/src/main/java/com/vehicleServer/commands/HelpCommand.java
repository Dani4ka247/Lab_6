package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;

import java.util.Map;

public class HelpCommand implements Command {
    private final Map<String, Command> commands;

    public HelpCommand(Map<String, Command> commands) {
        this.commands = commands;
    }

    @Override
    public Response execute(Request request) {
        StringBuilder helpMessage = new StringBuilder("Доступные команды:\n");
        commands.forEach((name, command) ->
                helpMessage.append(name).append(": ").append(command.getDescription()).append("\n"));
        return Response.success(helpMessage.toString());
    }

    @Override
    public String getDescription() {
        return "Выводит справку по всем доступным командам.";
    }
}