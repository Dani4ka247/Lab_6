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
        commands.put("help", new HelpCommand(commands));
        commands.put("show", new ShowCommand(collectionManager));
        commands.put("insert", new InsertCommand(collectionManager));
        commands.put("shutdown", new ExitCommand(collectionManager));
        commands.put("clear", new ClearCommand(collectionManager));
        commands.put("history", new HistoryCommand());
        commands.put("info", new InfoCommand(collectionManager));
        commands.put("remove_all_by_engine_power", new RemoveByPower(collectionManager));
        commands.put("remove_greater_key", new RemoveGreaterKey(collectionManager));
        commands.put("remove", new RemoveKeyCommand(collectionManager));
        commands.put("filter_greater_than_engine_power", new ShowByPower(collectionManager));
        commands.put("sum_of_engine_power", new SumOfPower(collectionManager));
        commands.put("replace_if_lower", new ReplaceIfLowerCommand(collectionManager));
        commands.put("execute_script", new ExecuteFileCommand(collectionManager));
        commands.put("show_sorted_by_power", new ShowByPower(collectionManager));
        commands.put("update", new UpdateCommand(collectionManager));
        commands.put("", new PassCommand());
    }

    public static Response executeRequest(Request request, boolean isAuthenticated) {
        String commandName = request.getCommand();
        if (commandName == null || commandName.trim().isEmpty()) {
            return Response.error("команда не может быть пустой. введите 'help' для помощи");
        }
        Command command = commands.get(commandName);
        if (command == null) {
            return Response.error("команда '" + commandName + "' не найдена. используйте 'help'");
        }
        // Для login, register и help не нужна проверка авторизации
        if (!commandName.equals("login") && !commandName.equals("register") && !commandName.equals("help")) {
            if (!isAuthenticated || request.getLogin() == null || request.getLogin().isEmpty()) {
                return Response.error("требуется авторизация: логин не указан");
            }
        }
        HistoryCommand.addToHistory(request.getCommand());
        return command.execute(request);
    }
}