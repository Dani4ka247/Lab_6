package com.vehicle.managers;

import com.vehicle.commands.*;
import com.vehicle.network.Request;
import com.vehicle.network.Response;

import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    private static final Map<String, Command> commands = new HashMap<>(); // Список всех команд
    private static CollectionManager collectionManager;

    /**
     * Инициализация менеджера команд
     *
     * @param manager Менеджер коллекции (инициализируется заранее)
     */
    public static void initialize(CollectionManager manager) {
        collectionManager = manager;

        System.out.println("Инициализация CommandManager...");
        commands.put("help", new HelpCommand(commands));
        commands.put("show", new ShowCommand(collectionManager));
        commands.put("insert", new InsertCommand(collectionManager));
        commands.put("shutdown", new ExitCommand(collectionManager));
        commands.put("", new PassCommand()); // Заглушка для пустого ввода
    }

    /**
     * Выполняет запрос пользователя, выбирая соответствующую команду
     *
     * @param request Объект запроса
     * @return Ответ, возвращённый соответствующей командой
     */
    public static Response executeRequest(Request request) {
        String commandName = request.getCommand();

        // Проверяем пустую команду
        if (commandName == null || commandName.trim().isEmpty()) {
            return Response.error("Ошибка: команда не может быть пустой. Введите 'help' для списка доступных команд.");
        }

        Command command = commands.get(commandName);

        // Проверяем отсутствие команды
        if (command == null) {
            return Response.error("Ошибка: команда '" + commandName + "' не найдена. Используйте 'help' для получения списка доступных команд.");
        }

        // Выполняем команду и возвращаем её результат
        return command.execute(request);
    }
}