package com.vehicleServer;

import com.vehicleServer.serverNetwork.Server;

public class ServerMain {
    public static void main(String[] args) {
        // Порт сервера
        int port = 6969;
        new Server(port).start();
    }
}