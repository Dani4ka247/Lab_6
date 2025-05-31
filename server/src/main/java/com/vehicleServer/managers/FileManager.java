package com.vehicleServer.managers;

import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.model.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FileManager {
    private static final Set<String> fileHistory = new HashSet<>();
    private static final int MAX_VEHICLE_LINES = 5; // name, coordinates, enginePower, vehicleType, fuelType
    private static final Set<String> VEHICLE_COMMANDS = Set.of("insert", "update", "replace_if_lower");

    public static List<Response> executeScript(String filePath, String login, String password, CollectionManager collectionManager) {
        List<Response> responses = new ArrayList<>();
        if (fileHistory.contains(filePath)) {
            responses.add(Response.error("рекурсия в файле " + filePath));
            return responses;
        }

        fileHistory.add(filePath);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            List<String> vehicleLines = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] tokens = line.split(" ", 2);
                String commandName = tokens[0];
                String argument = tokens.length > 1 ? tokens[1] : null;

                if (VEHICLE_COMMANDS.contains(commandName)) {
                    vehicleLines.clear();
                    for (int i = 0; i < MAX_VEHICLE_LINES && (line = reader.readLine()) != null; i++) {
                        vehicleLines.add(line.trim());
                    }
                    Vehicle vehicle = parseVehicle(vehicleLines);
                    if (vehicle == null) {
                        responses.add(Response.error("неверный формат vehicle для команды " + commandName));
                        continue;
                    }
                    Request request = new Request(commandName, argument, login, password);
                    request.setVehicle(vehicle);
                    responses.add(CommandManager.executeRequest(request,true));
                } else {
                    Request request = new Request(commandName, argument, login, password);
                    responses.add(CommandManager.executeRequest(request,true));
                }
            }
        } catch (IOException e) {
            responses.add(Response.error("ошибка чтения файла " + filePath + ": " + e.getMessage()));
        } finally {
            fileHistory.remove(filePath);
        }
        return responses;
    }

    private static Vehicle parseVehicle(List<String> lines) {
        if (lines.size() != MAX_VEHICLE_LINES) {
            return null;
        }
        try {
            String name = lines.get(0);
            String[] coordParts = lines.get(1).split("\\(");
            String[] parts = coordParts[1].split(",");
            Coordinates coordinates = new Coordinates(
                    Float.parseFloat(parts[0].trim()),
                    Integer.parseInt(parts[1].trim().split("\\)")[0])
            );
            float enginePower = Float.parseFloat(lines.get(2));
            VehicleType vehicleType = VehicleType.valueOf(lines.get(3).trim().toUpperCase());
            FuelType fuelType = FuelType.valueOf(lines.get(4));
            return new Vehicle(0, coordinates, name, enginePower, vehicleType, fuelType);
        } catch (Exception e) {
            return null;
        }
    }
}