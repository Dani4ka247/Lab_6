package com.vehicle.network;

import com.vehicle.model.Vehicle;
import java.io.Serializable;

public class Request implements Serializable {
    private String command; // Имя команды
    private String argument; // Аргумент команды (может быть null)
    private Vehicle vehicle; // Объект Vehicle (может быть null)

    public Request(String command, String argument) {
        this.command = command;
        this.argument = argument;
    }

    public String getCommand() {
        return command;
    }

    public String getArgument() {
        return argument;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    @Override
    public String toString() {
        return "Request{command='" + command + "', argument='" + argument + "', vehicle=" + vehicle + '}';
    }
}