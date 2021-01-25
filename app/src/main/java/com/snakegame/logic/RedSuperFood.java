package com.snakegame.logic;

import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;

public class RedSuperFood extends Food{
    private static final int SCORE = 20;

    public RedSuperFood(Point fieldDimensions, Snake snake, int radius) {
        super(fieldDimensions, snake, radius, SCORE, Color.RED);
        Log.v("RedApple", "Red apple created");
    }
}
