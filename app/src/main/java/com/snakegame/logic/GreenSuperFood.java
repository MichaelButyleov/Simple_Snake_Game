package com.snakegame.logic;

import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;

public class GreenSuperFood extends Food{
    private static final int SCORE = 10;

    public GreenSuperFood(Point fieldDimensions, Snake snake, int radius) {
        super(fieldDimensions, snake, radius, SCORE, Color.GREEN);
        Log.v("GreenSuperFood", "Green Super Food created");
    }
}
