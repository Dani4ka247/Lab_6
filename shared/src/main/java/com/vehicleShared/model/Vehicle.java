package com.vehicleShared.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Vehicle implements Serializable,Comparable<Vehicle> {
    private static final long serialVersionUID = 1L; // Уникальный идентификатор версии
    private long id; //Значение поля должно быть больше 0, Значение этого поля должно быть уникальным, Значение этого поля должно генерироваться автоматически
    private String name; //Поле не может быть null, Строка не может быть пустой
    private Coordinates coordinates; //Поле не может быть null
    private ZonedDateTime creationDate; //Поле не может быть null, Значение этого поля должно генерироваться автоматически
    private Float enginePower; //Поле не может быть null, Значение поля должно быть больше 0
    private VehicleType type; //Поле не может быть null
    private FuelType fuelType; //Поле не может быть null

    public Vehicle(long id, Coordinates coordinates, ZonedDateTime creationDate, String name, Float enginePower, VehicleType type, FuelType fuelType) {
        this.id = id;
        this.name = name;
        this.coordinates = coordinates;
        this.creationDate = creationDate;
        this.enginePower = (enginePower > 0 ? enginePower : 0);
        this.type = type;
        this.fuelType = fuelType;
    }

    public Vehicle(long id, Coordinates coordinates, String name, Float enginePower, VehicleType type, FuelType fuelType) {
        this.id = id;
        this.name = name;
        this.coordinates = coordinates;
        this.creationDate = ZonedDateTime.now();
        this.enginePower = (enginePower > 0 ? enginePower : 0);
        this.type = type;
        this.fuelType = fuelType;
    }
    public Vehicle(long id, Vehicle vehicle) {
        this.id = id;
        this.name = vehicle.name;
        this.coordinates = vehicle.coordinates;
        this.creationDate = ZonedDateTime.now();
        this.enginePower = (vehicle.enginePower > 0 ? vehicle.enginePower : 0);
        this.type = vehicle.type;
        this.fuelType = vehicle.fuelType;
    }

    @Override
    public int compareTo(Vehicle other) {
        return Long.compare(this.id, other.id);
    }


    @Override
    public String toString() {
        return "{id=" + id +
                ", name=" + name +
                ", coordinates=" + coordinates +
                ", creationDate=" + creationDate.format(DateTimeFormatter.ofPattern("yyyy_MM_dd HH:mm"))+
                ", enginePower=" + enginePower +
                ", type=" + type +
                ", fuelType=" + fuelType + "}";
    }

    public String toJson() {
        return "{\"id\" : " + id +
                ", \"name\" : \"" + name +
                "\", \"x\" : " + coordinates.getX() +
                ", \"y\" : " + coordinates.getY() +
                ", \"creationDate\" : \"" + creationDate+
                "\", \"enginePower\" : " + enginePower +
                ", \"type\" : \"" + type +
                "\", \"fuelType\" : \"" + fuelType + "\"}";
    }

    public float getPower() {
        return enginePower;
    }

    public long getId() {
        return id;
    }
}

