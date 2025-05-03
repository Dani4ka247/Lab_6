package com.vehicle.managers;

import com.vehicle.commands.Command;
import com.vehicle.network.Request;
import com.vehicle.network.Response;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * FileManager предназначен для работы с файлами и скриптами. Функционал включает:
 * - Выполнение скриптов.
 * - Чтение команд из файла.
 * - Управление историей файлов для предотвращения рекурсии.
 */
public class FileManager {

    private static Set<String> fileHistory = new HashSet<>();
    private static String parameter;
    private static String command;

    /**
     * Выполняет команды из файла, обрабатывая их построчно.
     *
     * @param scanner   Сканнер файла для обработки строк.
     * @param filePath  Путь к выполняемому скрипту.
     * @param commands  Словарь доступных команд.
     * @param collection Менеджер коллекции.
     */
    public static void program(Scanner scanner, String filePath, Map<String, Command> commands, CollectionManager collection) {
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            executionCommand(line, commands, collection);
        }
        fileHistory.remove(filePath);
    }

    /**
     * Начинает выполнение скрипта из указанного файла.
     *
     * @param filePath  Путь к файлу.
     * @param collection Контроллер коллекции.
     * @param commands  Доступные команды.
     */
    public static void start(String filePath, CollectionManager collection, Map<String, Command> commands) {
        if (fileHistory.contains(filePath)) {
            System.out.println("Предотвращена рекурсия: файл уже выполняется.");
            return;
        }

        fileHistory.add(filePath);

        try (Scanner scanner = new Scanner(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            program(scanner, filePath, commands, collection);
        } catch (FileNotFoundException e) {
            System.out.println("Файл не найден: " + filePath);
        } catch (Exception e) {
            System.out.println("Ошибка выполнения скрипта: " + e.getMessage());
        }
    }

    /**
     * Парсит строку команды и выполняет её.
     *
     * @param text       Текст команды.
     * @param commands   Карта доступных команд.
     * @param collection Контроллер коллекции.
     */
    public static void executionCommand(String text, Map<String, Command> commands, CollectionManager collection) {
        String[] tokens = getTokens(text);
        Command command = commands.get(tokens[0]);
        if (command == null) {
            System.out.println("Неизвестная команда: " + tokens[0]);
            return;
        }

        try {
            Request request = new Request(tokens[0], tokens.length > 1 ? tokens[1] : null);
            Response response = command.execute(request);
            System.out.println(response.getMessage());
        } catch (Exception e) {
            System.out.println("Ошибка выполнения команды: " + e.getMessage());
        }
    }

    /**
     * Делит текст команды на токены.
     *
     * @param text Текст команды.
     * @return Массив из имени команды и аргумента.
     */
    public static String[] getTokens(String text) {
        String[] tokens = text.split(" ", 2);
        command = tokens[0];
        parameter = tokens.length > 1 ? tokens[1] : null;
        return tokens;
    }

    /**
     * Читает содержимое файла в строку.
     *
     * @param filePath Путь к файлу.
     * @return Содержимое файла в виде строки.
     * @throws IOException Ошибка при чтении.
     */
    public static String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString().trim();
    }

    /**
     * Возвращает последний аргумент команды.
     *
     * @return Аргумент команды.
     */
    public static String getParameter() {
        return parameter;
    }

    /**
     * Возвращает текущую команду.
     *
     * @return Текущая команда.
     */
    public static String getCommand() {
        return command;
    }

}