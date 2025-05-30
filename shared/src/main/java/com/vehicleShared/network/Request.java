package com.vehicleShared.network;

import com.vehicleShared.model.Vehicle;
import java.io.Serializable;

public class Request implements Serializable {
    private String command;
    private String argument;
    private Vehicle vehicle;
    private String login;
    private String password;

    public Request(String command, String argument, String login, String password) {
        this.command = command;
        this.argument = argument;
        this.login = login;
        this.password = password;
    }

    public void setLogin(String login){
        this.login=login;
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

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "Request{command='" + command + "', argument='" + argument + "', vehicle=" + vehicle + ", login='" + login + "'}";
    }
}