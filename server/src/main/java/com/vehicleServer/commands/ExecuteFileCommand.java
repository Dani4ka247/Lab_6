package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;
import com.vehicleServer.managers.FileManager;
import java.util.List;
import java.util.stream.Collectors;

public class ExecuteFileCommand implements Command {
    private final CollectionManager collectionManager;

    public ExecuteFileCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String filePath = request.getArgument();
        if (filePath == null || filePath.trim().isEmpty()) {
            return Response.error("нужен путь к файлу");
        }
        List<Response> responses = FileManager.executeScript(filePath, request.getLogin(), request.getPassword(), collectionManager);
        String result = responses.stream()
                .map(Response::getMessage)
                .collect(Collectors.joining("\n"));
        return responses.stream().allMatch(Response::isSuccess)
                ? Response.success(result)
                : Response.error(result);
    }

    @Override
    public String getDescription() {
        return "выполняет команды из указанного файла";
    }
}