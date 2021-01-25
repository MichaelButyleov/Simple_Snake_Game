package com.snakegame.logic;

import android.graphics.Point;

public class Cell extends GameElements{
    public Cell(int x, int y, int radius) {
        super(radius);
        location = new Point(x, y);
    }

    public Cell(Cell cell) {
        super(cell.getRadius());
        location = new Point(cell.location);
    }
}
