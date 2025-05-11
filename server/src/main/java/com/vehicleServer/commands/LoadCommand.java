package com.vehicleServer.commands;

import com.vehicleShared.managers.CollectionManager;
import com.vehicleServer.managers.FileManager;
import com.vehicleShared.managers.IdManager;
import com.vehicleShared.model.*;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import java.time.ZonedDateTime;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

public class LoadCommand implements Command {

    private final CollectionManager collection;
    private final String filePath;

    public LoadCommand(CollectionManager collection, String filePath) {
        this.collection = collection;
        this.filePath = filePath;
    }

    @Override
    public Response execute(Request request) {
        StringBuilder file = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                file.append(line).append("\n");
            }
        } catch (IOException e) {
            return new Response(false, "Ошибка загрузки файла: " + e.getMessage());
        }

        // Обработка содержимого файла
        String content = file.toString();
        content = content.split("\\[\n")[1].split("\\]\n")[0];
        if (content.endsWith("\n  ")) {
            content = content.substring(0, content.length() - 3);
        }

        for (String line : content.split("\n  ")) {
            try {
                String[] tokens = line.split(",");
                if (tokens.length != 9) {
                    return new Response(false, "Некорректный формат данных в файле.");
                }

                int key = Integer.parseInt(tokens[0].trim());
                long id = Long.parseLong(getValue(tokens[1]));
                String name = getValue(tokens[2]);
                Coordinates coordinates = new Coordinates(
                        Float.parseFloat(getValue(tokens[3])),
                        Integer.parseInt(getValue(tokens[4]))
                );
                ZonedDateTime creationDate = ZonedDateTime.parse(
                        tokens[5].replace(" \"creationDate\" : ", "").replace("\"", "").replace("|", "")
                );
                Float enginePower = Float.parseFloat(getValue(tokens[6]));
                VehicleType vehicleType = VehicleType.valueOf(getValue(tokens[7]));
                FuelType fuelType = FuelType.valueOf(getValue(tokens[8]).replace("}", ""));

                if (IdManager.getId() <= id) {
                    IdManager.setId(id + 1);
                }

                collection.put(key, new Vehicle(
                        id, coordinates, creationDate, name, enginePower, vehicleType, fuelType
                ));
            } catch (Exception e) {
                return new Response(false, "Ошибка обработки строки: " + line);
            }
        }

        return new Response(true, "Коллекция успешно загружена.");
    }

    private String getValue(String value) {
        return value.split(":")[1].trim().replace("\"", "");
    }

    @Override
    public String getDescription() {
        return "Загружает коллекцию из файла.";
    }
}