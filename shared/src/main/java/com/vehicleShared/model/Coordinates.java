package com.vehicleShared.model;

import java.io.Serializable;

public class Coordinates implements Serializable {
    private static final long serialVersionUID = 1L;

    private Float x;
    private int y;

    public Coordinates(Float x, int y) throws IllegalArgumentException {
        if (x >= 982 | y >= 67) {
            throw new IllegalArgumentException();
        }

        this.x = x;
        this.y = y;
    }

    public static Coordinates parser(String input) throws IllegalArgumentException {
        String[] result = input.split(",");
        if (result.length != 2) {
            throw new IllegalArgumentException();
        }
        return new Coordinates(Float.parseFloat(result[0].trim()), Integer.parseInt(result[1].trim()));
    }

    @Override
    public String toString() {
        return "{x=" + x +
                ", y=" + y + "}";
    }

    public float getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}