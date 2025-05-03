package com.vehicle.commands;

import com.vehicle.managers.CollectionManager;
import com.vehicle.managers.IdManager;
import com.vehicle.model.Vehicle;
import com.vehicle.network.Request;
import com.vehicle.network.Response;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class SaveCommand implements Command {

    private final CollectionManager collection;
    private final String filePath;

    public SaveCommand(CollectionManager collection, String filePath) {
        this.collection = collection;
        this.filePath = filePath;
    }

    @Override
    public Response execute(Request request) {
        try {
            IdManager.saveIdToFile();
        } catch (IOException e) {
            return new Response(false, "Ошибка сохранения ID: " + e.getMessage());
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            StringBuilder jsonString = new StringBuilder();
            for (Map.Entry<Integer, Vehicle> element : collection.entrySet()) {
                jsonString.append("   ")
                        .append(element.getKey())
                        .append(", ")
                        .append(element.getValue().toJson())
                        .append(",\n");
            }

            // Удаление лишней запятой в конце
            if (jsonString.length() > 0 && jsonString.toString().endsWith(",\n")) {
                jsonString.setLength(jsonString.length() - 2);
                jsonString.append("\n");
            }

            // Формирование JSON
            writer.write("{\n" +
                    "  \"name\": \"save\",\n" +
                    "  \"version\": \"1.0\",\n" +
                    "  \"collection\": [\n" +
                    jsonString +
                    "  ]\n" +
                    "}");
            writer.flush();

            return new Response(true, "Коллекция успешно сохранена!");
        } catch (IOException e) {
            return new Response(false, "Ошибка записи в файл: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "Сохраняет коллекцию в файл.";
    }
}