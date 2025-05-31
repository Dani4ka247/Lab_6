package com.vehicleShared.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Vehicle implements Serializable, Comparable<Vehicle> {
    private static final long serialVersionUID = 1L;
    private long id;
    private String name;
    private Coordinates coordinates;
    private ZonedDateTime creationDate;
    private Float enginePower;
    private VehicleType type;
    private FuelType fuelType;

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
                ", creationDate=" + creationDate.format(DateTimeFormatter.ofPattern("yyyy_MM_dd HH:mm")) +
                ", enginePower=" + enginePower +
                ", type=" + type +
                ", fuelType=" + fuelType + "}";
    }

    public String toJson() {
        return "{\"id\" : " + id +
                ", \"name\" : \"" + name +
                "\", \"x\" : " + coordinates.getX() +
                ", \"y\" : " + coordinates.getY() +
                ", \"creationDate\" : \"" + creationDate +
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

    public String getName() {
        return name;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public ZonedDateTime getCreationDate() {
        return creationDate;
    }

    public VehicleType getType() {
        return type;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setCreationDate(ZonedDateTime creationDate) {
        this.creationDate = creationDate;
    }
}