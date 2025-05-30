package com.vehicleServer.managers;

import com.vehicleShared.managers.CollectionManager;
import com.vehicleShared.network.Request;
import com.vehicleShared.network.Response;
import com.vehicleServer.commands.*;
import org.slf4j.Logger;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    private static final Map<String, Command> commands = new HashMap<>();
    private static CollectionManager collectionManager;
    private static Logger logger;

    public static void initialize(CollectionManager manager, Logger log) {
        collectionManager = manager;
        logger = log;
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
        commands.put("register", new RegisterCommand(collectionManager));
        commands.put("", new PassCommand());
    }

    public static Response executeRequest(Request request) {
        String commandName = request.getCommand();
        if (commandName == null || commandName.trim().isEmpty()) {
            return Response.error("команда не может быть пустой. введите 'help' для помощи");
        }
        Command command = commands.get(commandName);
        if (command == null) {
            return Response.error("команда '" + commandName + "' не найдена. используйте 'help'");
        }
        if (!commandName.equals("register") && !commandName.equals("help") && !request.getLogin().equals("console")) {
            try {
                if (!collectionManager.registerUser(request.getLogin(), md5(request.getPassword()))) {
                    return Response.error("неверный логин или пароль");
                }
            } catch (Exception e) {
                return Response.error("ошибка авторизации: " + e.getMessage());
            }
        }
        HistoryCommand.addToHistory(request.getCommand());
        return command.execute(request);
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}