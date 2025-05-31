package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;
import java.util.List;
import java.util.stream.Collectors;

public class RemoveGreaterKey implements Command {
    private final CollectionManager collectionManager;

    public RemoveGreaterKey(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String argument = request.getArgument();
        String userId = request.getLogin();
        if (argument == null || argument.isEmpty()) {
            return Response.error("нужен id");
        }
        try {
            long keyThreshold = Long.parseLong(argument);
            List<Long> keysToRemove = collectionManager.keySet()
                    .stream()
                    .filter(key -> key > keyThreshold)
                    .filter(key -> {
                        try {
                            return collectionManager.getDbManager().canModify(key, userId);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            if (keysToRemove.isEmpty()) {
                return Response.success("нет элементов с id больше " + keyThreshold);
            }

            boolean allRemoved = true;
            for (Long id : keysToRemove) {
                if (collectionManager.remove(id, userId) == null) {
                    allRemoved = false;
                }
            }

            String removedKeys = String.join(", ", keysToRemove.stream().map(String::valueOf).collect(Collectors.toList()));
            return allRemoved
                    ? Response.success("удалены элементы с id больше " + keyThreshold + ": " + removedKeys)
                    : Response.error("не все элементы удалось удалить");
        } catch (NumberFormatException e) {
            return Response.error("id должен быть числом");
        } catch (Exception e) {
            return Response.error("ошибка: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "удаляет все элементы с id больше заданного, принадлежащие пользователю";
    }
}