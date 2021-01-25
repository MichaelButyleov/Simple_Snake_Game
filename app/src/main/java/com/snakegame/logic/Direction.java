package com.snakegame.logic;

public enum Direction {
    RIGHT(0), DOWN(1), LEFT(2), UP(3);

    private final int value;

    Direction(int value) {
        this.value = value;
    }

    public boolean isHorizontal() {
        return value == 0 || value == 2;
    }

    public String getString() {
        switch (value) {
            case 0:
                return "RIGHT";
            case 1:
                return "DOWN";
            case 2:
                return "LEFT";
            case 3:
                return "UP";
        }

        return "";
    }
}
