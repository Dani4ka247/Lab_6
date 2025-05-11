package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;
import com.vehicleServer.managers.CommandManager;
import com.vehicleServer.managers.FileManager;

import java.util.List;

public class ExecuteFileCommand implements Command {
    private final CollectionManager collectionManager;

    public ExecuteFileCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String filePath = request.getArgument();
        if (filePath == null || filePath.isEmpty()) {
            return Response.error("Ошибка: команда 'execute_script' требует указания пути к файлу.");
        }

        List<Response> responses = FileManager.executeScript(filePath, collectionManager);
        StringBuilder result = new StringBuilder("Результат выполнения скрипта " + filePath + ":\n");
        for (Response response : responses) {
            result.append(response.getMessage()).append("\n");
        }
        return Response.success(result.toString());
    }

    @Override
    public String getDescription() {
        return "Считывает и выполняет скрипт из указанного файла.";
    }
}