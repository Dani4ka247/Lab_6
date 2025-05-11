package com.vehicleServer.managers;


import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleServer.commands.*;

import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    private static final Map<String, Command> commands = new HashMap<>(); // Список всех команд
    private static CollectionManager collectionManager;
    private static String filePath;



    /**
     * Инициализация менеджера команд
     *
     * @param manager Менеджер коллекции (инициализируется заранее)
     */
    public static void initialize(CollectionManager manager) {
        collectionManager = manager;
        // Считывание FILE_PATH из переменной окружения
        filePath = System.getenv("FILE_PATH");

        // Если окружение не задано, устанавливаем значение по умолчанию
        if (filePath == null || filePath.trim().isEmpty()) {
            filePath = "data.json"; // Значение по умолчанию
            System.out.println("Переменная окружения FILE_PATH не задана. Использую значение по умолчанию: " + filePath);
        } else {
            System.out.println("Использую путь к файлу из окружения: " + filePath);
        }

        System.out.println("Инициализация CommandManager...");
        commands.put("help", new HelpCommand(commands));
        commands.put("show", new ShowCommand(collectionManager));
        commands.put("insert", new InsertCommand(collectionManager));
        commands.put("shutdown", new ExitCommand(collectionManager));
        commands.put("save", new SaveCommand(collectionManager,filePath));
        commands.put("load", new LoadCommand(collectionManager,filePath));
        commands.put("clear", new ClearCommand(collectionManager));
        commands.put("history", new HistoryCommand());
        commands.put("info", new InfoCommand(collectionManager));
        commands.put("remove_all_by_engine_power", new RemoveByPower(collectionManager));
        commands.put("remove_greater_key", new RemoveGreaterKey(collectionManager));
        commands.put("remove", new RemoveKeyCommand(collectionManager));
        commands.put("filter_greater_than_engine_power", new ShowByPower(collectionManager));
        commands.put("sum_of_engine_power", new SumOfPower(collectionManager));
        commands.put("replace_if_lower", new ReplaceIfLowerCommand(collectionManager));
        commands.put("update", new UpdateCommand(collectionManager));
        commands.put("execute_script", new ExecuteFileCommand(collectionManager));///Users/mac/scrypt.txt
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
        HistoryCommand.addToHistory(request.getCommand());
        return command.execute(request);
    }
}