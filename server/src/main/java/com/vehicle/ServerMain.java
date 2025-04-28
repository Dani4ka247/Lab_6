package com.vehicle;

import com.vehicle.serveNetwork.Server;

public class ServerMain {
    public static void main(String[] args) {
        // Порт сервера
        int port = 6969;
        new Server(port).start();
    }
}