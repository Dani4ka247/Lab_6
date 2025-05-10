package com.vehicleClient;

public class ClientMain {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 6969;
        new Client(host, port).start();
    }
}