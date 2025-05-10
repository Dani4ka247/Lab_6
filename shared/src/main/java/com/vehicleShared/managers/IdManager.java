package com.vehicleShared.managers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class IdManager {

    private static final String filePath = "id.txt";
    private static long currentId = 0;

    public static void loadIdFromFile() throws IOException {
        File file = new File(filePath);
        if (file.exists() && file.canRead()) {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            String line = reader.readLine();
            if (line != null) {
                currentId = Long.parseLong(line);
            } else {
                currentId = 0;
            }
        } else {
            file.createNewFile();
            saveIdToFile();
        }
    }


    public static long getUnicId() {

        return ++currentId - 1;
    }

    public static void saveIdToFile() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        writer.write(String.valueOf(currentId));
        writer.close();
    }

    public static void setId(long id) {
        currentId = id;
    }

    public static long getId() {
        return currentId;
    }
}
