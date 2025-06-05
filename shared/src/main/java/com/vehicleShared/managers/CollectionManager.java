package com.vehicleShared.managers;

import com.vehicleShared.model.*;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.sql.SQLException;

public class CollectionManager extends ConcurrentHashMap<Long, Vehicle> {
    public final LocalDateTime initializationDate;
    private final DbManager dbManager;

    public CollectionManager(DbManager dbManager) {
        this.initializationDate = LocalDateTime.now();
        this.dbManager = dbManager;
    }

    public synchronized void loadFromDb(String userId, boolean loadAll) throws SQLException {
        clear();
        for (Vehicle vehicle : dbManager.loadFromDb(userId, loadAll)) {
            super.put(vehicle.getId(), vehicle);
        }
    }

    public boolean put(Vehicle vehicle, String userId) {
        try {
            if (vehicle.getName() == null || vehicle.getName().isEmpty() ||
                    vehicle.getCoordinates() == null || vehicle.getPower() <= 0 ||
                    vehicle.getType() == null || vehicle.getFuelType() == null ||
                    userId == null) {
                return false; // невалидные данные
            }
            if (dbManager.addVehicle(vehicle, userId)) {
                super.put(vehicle.getId(), vehicle);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("ошибка добавления: " + e.getMessage());
        }
        return false;
    }

    public boolean update(long id, Vehicle vehicle, String userId) {
        try {
            if (vehicle.getName() == null || vehicle.getName().isEmpty() ||
                    vehicle.getCoordinates() == null || vehicle.getPower() <= 0 ||
                    vehicle.getType() == null || vehicle.getFuelType() == null ||
                    userId == null) {
                return false;
            }
            if (dbManager.canModify(id, userId) && dbManager.updateVehicle(id, vehicle, userId)) {
                super.put(id, vehicle);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("ошибка обновления в базе: " + e.getMessage());
        }
        return false;
    }

    public Vehicle remove(Object key, String userId) {
        try {
            Long id = (Long) key;
            if (dbManager.canModify(id, userId) && dbManager.removeVehicle(id, userId)) {
                return super.remove(key);
            }
        } catch (SQLException e) {
            System.err.println("ошибка удаления из базы: " + e.getMessage());
        }
        return null;
    }

    public String getVehiclesByMinPower(float minimumPower) {
        return entrySet()
                .stream()
                .filter(entry -> entry.getValue().getPower() >= minimumPower)
                .map(entry -> entry.getKey() + " : " + entry.getValue().toString())
                .collect(Collectors.joining("\n"));
    }

    public static Vehicle requestVehicleInformation(Scanner scanner, long id) {
        String vehicleName = InputValidator.getValidInput(scanner, s -> s, "введите название машины: ", "название не может быть пустым!");
        Coordinates coordinates = InputValidator.getValidInput(scanner, Coordinates::parser, "введите координаты машины в формате x,y: ", "введи два числа через запятую (x<982,y<67), например 22.8,7");
        Float enginePower = InputValidator.getValidInput(scanner, s -> {
            float power = Float.parseFloat(s);
            if (power <= 0) {
                throw new IllegalArgumentException("мощность должна быть больше 0");
            }
            return power;
        }, "введите мощность двигателя машины: ", "мощность должна быть числом больше 0");
        VehicleType vehicleType = InputValidator.getValidInput(scanner, s -> VehicleType.values()[Integer.parseInt(s.trim()) - 1], "выберите тип машины {1:CAR, 2:BOAT, 3:HOVERBOARD}: ", "введи номер типа");
        FuelType fuelType = InputValidator.getValidInput(scanner, s -> FuelType.values()[Integer.parseInt(s.trim()) - 1], "выберите тип топлива {1:GASOLINE, 2:KEROSENE, 3:ELECTRICITY, 4:MANPOWER, 5:NUCLEAR}: ", "введи номер топлива");
        return new Vehicle(id, coordinates, vehicleName, enginePower, vehicleType, fuelType);
    }

    public static Vehicle requestVehicleInformation(Scanner scanner) {
        return requestVehicleInformation(scanner, 0L); // id игнорируется, база задаёт
    }

    public DbManager getDbManager() {
        return dbManager;
    }
}