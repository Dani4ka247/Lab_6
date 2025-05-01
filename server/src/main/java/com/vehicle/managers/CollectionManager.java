package com.vehicle.managers;




import com.vehicle.model.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;


public class CollectionManager extends HashMap<Integer, Vehicle> {

    public final LocalDateTime initializationDate;

    public CollectionManager() {
        this.initializationDate = LocalDateTime.now();
    }

    public String getSortedVehiclesByPower() {
        return this.values()
                .stream()
                .sorted((v1, v2) -> Float.compare(v1.getPower(), v2.getPower()))
                .map(Vehicle::toString)
                .collect(Collectors.joining("\n")); // Объединяем строки, разделяя переносами
    }

    public String getVehiclesByMinPower(float minimumPower) {
        return this.values()
                .stream()
                .filter(vehicle -> vehicle.getPower() >= minimumPower) // Фильтруем по мощности
                .map(Vehicle::toString)
                .collect(Collectors.joining("\n")); // Соединяем строки с переводом на новую строку
    }

    public static Vehicle requestVehicleInformation(Scanner scanner, long id) {
        String vehicleName = InputValidator.getValidInput(scanner, s -> s, "Введите название машины : ", "неужели так сложно название ввести?!");
        Coordinates coordinates = InputValidator.getValidInput(scanner, Coordinates::parser, "Введите координаты машины в формате x,y : ", "тебе надо ввести два числа через запятую(x<982,y<67). Например 22.8 , 7");
        Float enginePower = InputValidator.getValidInput(scanner, Float::parseFloat, "Введите мощность двигателя машины : ", "введи число");
        VehicleType vehicleType = InputValidator.getValidInput(scanner, s -> VehicleType.values()[Integer.parseInt(s.trim()) - 1], "Выберите тип машины {1:CAR, 2:BOAT, 3:HOVERBOARD} : ", "тебе стоит ввести номер нужного значения");
        FuelType fuelType = InputValidator.getValidInput(scanner, s -> FuelType.values()[Integer.parseInt(s.trim()) - 1], "Выберите тип машины {1:GASOLINE, 2:KEROSENE, 3:ELECTRICITY, 4:MANPOWER, 5:NUCLEAR} : ", "тебе стоит ввести номер нужного значения");
        return new Vehicle(id, coordinates, vehicleName, enginePower, vehicleType, fuelType);
    }
}
