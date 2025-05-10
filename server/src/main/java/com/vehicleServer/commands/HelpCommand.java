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
        StringBuilder result = new StringBuilder("Доступные команды:\n");
        commands.forEach((name, command) -> result.append("- ").append(name).append(": ").append(command.getDescription()).append("\n"));

        return Response.success(result.toString());
    }

    @Override
    public String getDescription() {
        return "Отображает список доступных команд с их описаниями.";
    }
}