package com.vehicle.network;

import java.io.Serializable;

public class Request implements Serializable {
    private String command; // Имя команды
    private String argument; // Аргумент команды (может быть null)

    public Request(String command, String argument) {
        this.command = command;
        this.argument = argument;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getArgument() {
        return argument;
    }

    public void setArgument(String argument) {
        this.argument = argument;
    }

    @Override
    public String toString() {
        return "Request{command='" + command + "', argument='" + argument + "'}";
    }
}