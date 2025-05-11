package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;

import java.util.LinkedList;
import java.util.Queue;

public class HistoryCommand implements Command {
    private static final Queue<String> commandHistory = new LinkedList<>();
    private static final int MAX_HISTORY_SIZE = 10;

    public HistoryCommand() {
    }

    public static void addToHistory(String command) {
        commandHistory.add(command);
        if (commandHistory.size() > MAX_HISTORY_SIZE) {
            commandHistory.poll();
        }
    }

    @Override
    public Response execute(Request request) {
        if (commandHistory.isEmpty()) {
            return Response.success("История команд пуста.");
        }

        String history = String.join("\n", commandHistory);
        return Response.success("Последние выполненные команды:\n" + history);
    }

    @Override
    public String getDescription() {
        return "Выводит последние 10 выполненных команд.";
    }
}