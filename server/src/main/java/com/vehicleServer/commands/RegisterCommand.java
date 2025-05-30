package com.vehicleServer.commands;

import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleShared.managers.CollectionManager;

public class RegisterCommand implements Command {
    private final CollectionManager collectionManager;

    public RegisterCommand(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String login = request.getLogin();
        String password = request.getPassword();
        if (login == null || login.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return Response.error("логин и пароль не могут быть пустыми");
        }
        try {
            if (collectionManager.registerUser(login, password)) {
                return Response.success("пользователь " + login + " успешно зарегистрирован");
            }
            return Response.error("логин " + login + " уже занят");
        } catch (Exception e) {
            return Response.error("ошибка регистрации: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "регистрирует нового пользователя с указанным логином и паролем";
    }
}