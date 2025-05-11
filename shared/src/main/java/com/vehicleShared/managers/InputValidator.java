package com.vehicleShared.managers;

import java.util.Scanner;
import java.util.function.Function;

public class InputValidator {

    public static <T> T getValidInput(Scanner scanner, Function<String, T> parser, String message, String errorMessage) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine();

            if (input.isEmpty()) {
                System.out.println("Странно, ничего не увидел, напиши ка еще раз");
                continue;
            }
            try {
                return parser.apply(input);
            } catch (Exception e) {
                System.out.println(errorMessage);
            }
        }
    }
}
