package com.snakegame.logic;

import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;

public class YellowSuperFood extends Food{
    private static final int SCORE = 30;

    public YellowSuperFood(Point fieldDimensions, Snake snake, int radius) {
        super(fieldDimensions, snake, radius, SCORE, Color.YELLOW);
        Log.v("YellowSuperFood", "Yellow Super Food created");
    }
}
