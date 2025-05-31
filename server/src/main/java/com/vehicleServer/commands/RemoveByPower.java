package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RemoveByPower implements Command {
    private final CollectionManager collectionManager;

    public RemoveByPower(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String argument = request.getArgument();
        String userId = request.getLogin();
        if (argument == null || argument.isEmpty()) {
            return Response.error("нужна мощность");
        }
        try {
            float power = Float.parseFloat(argument);
            List<Long> keysToRemove = collectionManager.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().getPower() == power)
                    .filter(entry -> {
                        try {
                            return collectionManager.getDbManager().canModify(entry.getKey(), userId);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (keysToRemove.isEmpty()) {
                return Response.success("нет элементов с мощностью " + power + " для удаления");
            }

            boolean allRemoved = true;
            for (Long id : keysToRemove) {
                if (collectionManager.remove(id, userId) == null) {
                    allRemoved = false;
                }
            }

            String removedKeys = String.join(", ", keysToRemove.stream().map(String::valueOf).collect(Collectors.toList()));
            return allRemoved
                    ? Response.success("удалены элементы с мощностью " + power + ", id: " + removedKeys)
                    : Response.error("не все элементы с мощностью " + power + " удалось удалить");
        } catch (NumberFormatException e) {
            return Response.error("мощность должна быть числом");
        } catch (Exception e) {
            return Response.error("ошибка: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "удаляет все элементы с указанной мощностью, принадлежащие пользователю";
    }
}