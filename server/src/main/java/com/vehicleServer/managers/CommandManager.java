package com.vehicleServer.managers;

import com.vehicleServer.commands.*;
import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.managers.DbManager;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import org.slf4j.Logger;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    private static final Map<String, Command> commands = new HashMap<>();
    private static CollectionManager collectionManager;
    private static DbManager dbManager;
    private static Logger logger;

    public static void initialize(CollectionManager collectionManager, DbManager dbManager, Logger logger) {
        CommandManager.collectionManager = collectionManager;
        CommandManager.dbManager = dbManager;
        CommandManager.logger = logger;
        logger.debug("инициализация команд");
        commands.put("help", new HelpCommand(commands));
        commands.put("show", new ShowCommand(collectionManager));
        commands.put("insert", new InsertCommand(collectionManager));
        commands.put("shutdown", new ExitCommand(collectionManager));
        commands.put("clear", new ClearCommand(collectionManager));
        commands.put("history", new HistoryCommand());
        commands.put("info", new InfoCommand(collectionManager));
        commands.put("remove_all_by_power", new RemoveByPower(collectionManager));
        commands.put("remove_greater_key", new RemoveGreaterKey(collectionManager));
        commands.put("remove", new RemoveKeyCommand(collectionManager));
        commands.put("sum_of_engine_power", new SumOfPower(collectionManager));
        commands.put("replace_if_lower", new ReplaceIfLowerCommand(collectionManager));
        commands.put("execute_script", new ExecuteFileCommand(collectionManager));
        commands.put("show_sorted_by_power", new ShowByPower(collectionManager));
        commands.put("update", new UpdateCommand(collectionManager));
        commands.put("", new PassCommand());
        logger.info("загружено {} команд: {}", commands.size(), commands.keySet());
    }

    public static Response executeRequest(Request request, boolean isAuthenticated) {
        String commandName = request.getCommand();
        logger.info("обработка команды: '{}', авторизован: {}, логин: {}", commandName, isAuthenticated, request.getLogin());
        if (commandName == null || commandName.trim().isEmpty()) {
            logger.warn("пустая команда");
            return Response.error("команда не может быть пустой. введите 'help' для помощи");
        }
        Command command = commands.get(commandName);
        if (command == null) {
            logger.warn("команда '{}' не найдена", commandName);
            return Response.error("команда '" + commandName + "' не найдена. используйте 'help'");
        }
        if (!commandName.equals("login") && !commandName.equals("register") && !commandName.equals("help")) {
            if (!isAuthenticated || request.getLogin() == null || request.getLogin().isEmpty()) {
                logger.warn("требуется авторизация для команды '{}'", commandName);
                return Response.error("требуется авторизация: логин не указан");
            }
        }
        logger.debug("выполняем команду '{}'", commandName);
        HistoryCommand.addToHistory(commandName);
        try {
            Response response = command.execute(request);
            logger.info("команда '{}' выполнена: {}", commandName, response.getMessage());
            return response;
        } catch (Exception e) {
            logger.error("ошибка выполнения команды '{}': {}", commandName, e.getMessage());
            return Response.error("ошибка выполнения команды: " + e.getMessage());
        }
    }
}