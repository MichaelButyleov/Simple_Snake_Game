package com.snakegame.logic;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.snakegame.R;
import com.snakegame.snake.ActivitySwipeDetector;
import com.snakegame.snake.SwipeInterface;

import java.util.ArrayDeque;
import java.util.Random;

public class GamePanel extends SurfaceView implements SurfaceHolder.Callback, SwipeInterface {
    private static final String TAG = GamePanel.class.getSimpleName();

    private static final int RED_FOOD_PERCENTAGE = 20;
    private static final int YELLOW_FOOD_PERCENTAGE = 5;

    private static final int CLOCK_PERCENTAGE = 2;
    private static final int SHIELD_PERCENTAGE = 1;

    private Context context;
    private MainThread thread;
    private Paint paint;
    private int tickCounter;
    private ArrayDeque<Direction> directionsQueue;

    private Point fieldDimensions;
    private int cellsDiameter, cellsRadius;
    private Snake snake;
    private Food food;
    private SpecialElements specialElements;

    private String highScoreKey = "highScore";
    private long highScore;
    private boolean highScoreUpdated;

    private Bitmap borderCell, snakeCell, snakeShieldedCell;
    private Bitmap greenFoodCell, redFoodCell, yellowFoodCell;
    private Bitmap clockCell, shieldCell;

    public GamePanel(Context context) {
        super(context);

        // save context (necessary to save high score)
        this.context = context;

        // add the callback (this) to the surface holder to intercept events
        getHolder().addCallback(this);

        // set on touch listener
        setOnTouchListener(new ActivitySwipeDetector(this));

        // create paint
        paint = new Paint();

        // create directions queue
        directionsQueue = new ArrayDeque<Direction>();

        // load bitmaps
        loadBitmaps();

        // make the GamePanel focusable so it can handle events
        setFocusable(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        // initialize game
        initGame();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "Surface is being destroyed");

        // tell the thread to shut down and wait for it to finish. this is a clean shutdown
        boolean retry = true;
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                // try again shutting down the thread
            }
        }

        Log.d(TAG, "Thread was shut down cleanly");
    }

    private void loadBitmaps() {
        borderCell = BitmapFactory.decodeResource(context.getResources(), R.drawable.border_cell);
        snakeCell = BitmapFactory.decodeResource(getResources(), R.drawable.sn);
        snakeShieldedCell = BitmapFactory.decodeResource(getResources(), R.drawable.snake_shielded_cell);

        greenFoodCell = BitmapFactory.decodeResource(getResources(), R.drawable.green_food_cell);
        redFoodCell = BitmapFactory.decodeResource(getResources(), R.drawable.red_food_cell);
        yellowFoodCell = BitmapFactory.decodeResource(getResources(), R.drawable.yellow_food_cell);

        clockCell = BitmapFactory.decodeResource(getResources(), R.drawable.clock_cell);
        shieldCell = BitmapFactory.decodeResource(getResources(), R.drawable.shield_cell);
    }

    /**
     * Game initialize method.
     */
    public void initGame() {
        // reset tick counter
        tickCounter = 0;

        // reset directions queue
        directionsQueue.clear();

        Log.d("SnakeView", "View width: " + getWidth());
        Log.d("SnakeView", "View height: " + getHeight());

        // initialize game board and game elements radius
        int fieldWidth = 20;
        cellsDiameter = getWidth() / fieldWidth;
        cellsRadius = cellsDiameter / 2;
        int fieldHeight = getHeight() / cellsDiameter;
        fieldDimensions = new Point(fieldWidth, fieldHeight);

        Log.d("MainActivity", "Cell Diameter: " + cellsDiameter);
        Log.d("MainActivity", "Field Dimensions: " + fieldWidth + "x" + fieldHeight);

        // create snake
        snake = new Snake(cellsRadius, (borderCell != null && snakeCell != null && greenFoodCell != null));

        // create food
        generateNewFood();

        // reset highScoreUpdated flag
        highScoreUpdated = false;

        // load high score
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        highScore = sharedPref.getLong(highScoreKey, 0);

        // create and start the game loop thread
        thread = new MainThread(getHolder(), this);
        thread.start();
    }

    /**
     * Game update method.
     */
    public void update() {
        // increment tick counter
        tickCounter++;

        // update clock counter
        if (tickCounter % MainThread.getFps() == 0)
            snake.updateClock();

        // update snake
        if (tickCounter % snake.getMoveDelay() == 0) {
            // increase snake speed if needed
            if (snake.speedNeedsToBeIncremented())
                snake.increaseSpeed();

            // set snake direction
            boolean done = false;
            while (!directionsQueue.isEmpty() && !done) {
                Direction direction = directionsQueue.poll();

                switch (direction) {
                    case UP:
                    case DOWN:
                        if (snake.isMovingHorizontally()) {
                            snake.setDirection(direction);
                            Log.d(TAG, "Consumed direction " + direction.getString() + " from queue");
                            done = true;
                        }
                        break;
                    case RIGHT:
                    case LEFT:
                        if (snake.isMovingVertically()) {
                            snake.setDirection(direction);
                            Log.d(TAG, "Consumed direction " + direction.getString() + " from queue");
                            done = true;
                        }
                        break;
                }
            }

            // check if snake hit any wall
            checkIfSnakeHitAnyWall();

            // if snake is alive
            if (!snake.isDead()) {
                // move the snake
                snake.move();

                // check if snake ate apple
                checkIfSnakeAteFood();

                // update special element
                updateSpecialElement();
            } else {
                // if high score hasn't been updated
                if (!highScoreUpdated) {
                    Log.d(TAG, "Updating high score");

                    saveHighScore();
                    highScoreUpdated = true;
                }
            }
        }
    }

    @Override
    public void onClick(View v, int x, int y) {
        // if snake is dead
        if (snake.isDead()) {
            Log.d(TAG, "Starting new game");
            initGame();
        } else {
            Direction direction = directionsQueue.isEmpty() ? snake.getDirection() : directionsQueue.getLast();

            if (direction.isHorizontal()) {
                // if snake is moving horizontally

                // if touch anywhere above of the snake head
                if (y < snake.getHead().getLocation().y * cellsDiameter) {
                    // move snake up
                    direction = Direction.UP;
                    Log.d(TAG, "Added direction UP to queue");
                } else {
                    // move snake down
                    direction = Direction.DOWN;
                    Log.d(TAG, "Added direction DOWN to queue");
                }
            } else {
                // if snake is moving vertically
                // if touch anywhere left of the snake head
                if (x < snake.getHead().getLocation().x * cellsDiameter) {
                    // move snake left
                    direction = Direction.LEFT;
                    Log.d(TAG, "Added direction LEFT to queue");
                } else {
                    // move snake right
                    direction = Direction.RIGHT;
                    Log.d(TAG, "Added direction RIGHT to queue");
                }
            }
            // add direction to queue of directions to be applied to the snake
            directionsQueue.add(direction);
        }
    }

    private void generateNewFood() {
        Random random = new Random();
        int num = random.nextInt(100) + 1;

        if (num <= RED_FOOD_PERCENTAGE)
            food = new RedSuperFood(fieldDimensions, snake, cellsRadius);
        else if (RED_FOOD_PERCENTAGE < num && num <= RED_FOOD_PERCENTAGE + YELLOW_FOOD_PERCENTAGE)
            food = new YellowSuperFood(fieldDimensions, snake, cellsRadius);
        else
            food = new GreenSuperFood(fieldDimensions, snake, cellsRadius);
    }

    private void updateSpecialElement() {
        // if no special element exists
        if (specialElements == null) {
            Random random = new Random();
            int num = random.nextInt(100) + 1;

            if (num <= CLOCK_PERCENTAGE)
                specialElements = new Clock(fieldDimensions, snake, cellsRadius);
            else if (CLOCK_PERCENTAGE < num && num <= CLOCK_PERCENTAGE + SHIELD_PERCENTAGE)
                specialElements = new Shield(fieldDimensions, snake, cellsRadius);
        } else if (snake.ate(specialElements)) {
            switch (specialElements.getType()) {
                case CLOCK:
                    snake.startClock();
                    Log.i(TAG, "Snake got the clock");
                    break;
                case SHIELD:
                    snake.setHasShield(true);
                    Log.i(TAG, "Snake got the shield");
                    break;
            }

            // destroy element
            specialElements = null;
        } else {
            // inc duration counter
            specialElements.incCounter();

            // destroy element if it has expired
            if (specialElements.hasExpired())
                specialElements = null;
        }
    }

    private void checkIfSnakeHitAnyWall() {
        // get snake head location
        Point head = snake.getHead().getLocation();

        switch (snake.getDirection()) {
            case UP:
                if (head.y <= 1)
                    snake.kill();
                break;
            case DOWN:
                if (head.y >= fieldDimensions.y - 2)
                    snake.kill();
                break;
            case LEFT:
                if (head.x <= 1)
                    snake.kill();
                break;
            case RIGHT:
                if (head.x >= fieldDimensions.x - 2)
                    snake.kill();
                break;
        }

        if (snake.isDead() && snake.hasShield()) {
            snake.setHasShield(false);
            snake.revive();

            Log.i(TAG, "Shield lost");
        }
    }

    public void checkIfSnakeAteFood() {
        if (snake.ate(food)) {
            Log.d("Snake", "Food has been eaten");

            // increase snake size
            snake.incSize();

            // set speed needs to be incremented flag
            snake.enableSpeedNeedsToBeIncrementedFlag();

            // update score
            snake.incScore(food.getScore());

            // update high score
            if (snake.getScore() > highScore)
                highScore = snake.getScore();

            // generate new apple
            generateNewFood();
        }
    }

    private void saveHighScore() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(highScoreKey, highScore);
        editor.commit();
    }

    /**
     * Game draw method.
     */
    public void render(Canvas canvas) {
        // draw background
        drawBackground(canvas);

        // draw board limits
        drawBoardLimits(canvas);

        // draw apple
        drawFood(canvas);

        // draw special element
        drawSpecialElement(canvas);

        // draw snake
        drawSnake(canvas);

        // display score
        drawScore(canvas);
        //developer name

        // if snake is dead
        if (snake.isDead()){
            drawGameOverMessage(canvas);
            drawDev(canvas);

        }
    }

    private void drawDev(Canvas canvas) {
        paint.setColor(Color.YELLOW);
        String text= "Developed by: M.Butyleov";
        int textSize = 3 * cellsDiameter / 2;
        int leftPadding = cellsDiameter + textSize / 4;
        int topPadding = getHeight() / 2 - 2 * textSize;
        canvas.drawText(text,leftPadding,topPadding + 3 * textSize , paint);

    }

    private void drawBackground(Canvas canvas) {
        int bgColor = snake.isDead() ? Color.rgb( 204, 0, 0) : Color.rgb(252,228,236);
        paint.setColor(bgColor);

        canvas.drawRect(0, 0, fieldDimensions.x * cellsDiameter, fieldDimensions.y * cellsDiameter, paint);
    }

    private void drawCell(Canvas canvas, Point p, Bitmap bitmap) {
        Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        int x = p.x * cellsDiameter;
        int y = p.y * cellsDiameter;
        Rect dst = new Rect(x, y, x + cellsDiameter, y + cellsDiameter);

        // draw bitmap
        canvas.drawBitmap(bitmap, src, dst, paint);
    }

    private void drawBoardLimits(Canvas canvas) {
        paint.setColor(Color.DKGRAY);

        // draw top  and bottom border
        for (int i = 0; i < fieldDimensions.x; i++) {
            drawCell(canvas, new Point(i, 0), borderCell);
            drawCell(canvas, new Point(i, fieldDimensions.y - 1), borderCell);
        }

        // fill first and last column
        for (int i = 0; i < fieldDimensions.y; i++) {
            drawCell(canvas, new Point(0, i), borderCell);
            drawCell(canvas, new Point(fieldDimensions.x - 1, i), borderCell);
        }
    }

    private void drawFood(Canvas canvas) {
        Bitmap bitmap;

        switch (food.getColor()) {
            case Color.GREEN:
                bitmap = greenFoodCell;
                break;
            case Color.RED:
                bitmap = redFoodCell;
                break;
            case Color.YELLOW:
                bitmap = yellowFoodCell;
                break;
            default:
                bitmap = borderCell;
                break;
        }

        drawCell(canvas, food.getLocation(), bitmap);
    }

    private void drawSpecialElement(Canvas canvas) {
        if (specialElements != null) {
            if (specialElements.getType() == GameElements.GameElementType.CLOCK)
                // draw clock
                drawCell(canvas, specialElements.getLocation(), clockCell);
            else if (specialElements.getType() == GameElements.GameElementType.SHIELD)
                // draw shield
                drawCell(canvas, specialElements.getLocation(), shieldCell);
        }
    }

    private void drawSnake(Canvas canvas) {
        if (snake.isUsingBitmaps()) {
            for (Cell cell : snake.getCells())
                if (snake.hasShield())
                    drawCell(canvas, cell.getLocation(), snakeShieldedCell);
                else
                    drawCell(canvas, cell.getLocation(), snakeCell);
        } else {
            paint.setColor(Color.BLACK);

            for (Cell cell : snake.getCells()) {
                Point p = cell.getLocation();

                int x = cellsRadius + p.x * cellsDiameter;
                int y = cellsRadius + p.y * cellsDiameter;
                canvas.drawCircle(x, y, cellsRadius, paint);
            }
        }
    }

    private void drawScore(Canvas canvas) {
        String[] text;
        if (snake.getSlowedTimeRemaining() == 0)
            text = new String[]{"Best: " + highScore,
                    "Score: " + snake.getScore()};
        else
            text = new String[]{"Best: " + highScore,
                    "Score: " + snake.getScore(),
                    "Clock: " + snake.getSlowedTimeRemaining()};

        int textSize = 3 * cellsDiameter / 2;
        int leftPadding = cellsDiameter + textSize / 4;
        int topPadding = cellsDiameter;

        for (int i = 0; i < text.length; i++) {
            paint.setTextSize(textSize);

            if (snake.isDead())
                paint.setColor(Color.YELLOW);
            else
                paint.setColor(Color.rgb(0, 0, 0));

            canvas.drawText(text[i], leftPadding, topPadding + (i + 1) * textSize, paint);
        }
    }

    private void drawGameOverMessage(Canvas canvas) {
        String[] text = new String[]{"Game Over.", "Tap to restart."};

        int textSize = 3 * cellsDiameter / 2;
        int leftPadding = cellsDiameter + textSize / 4;
        int topPadding = getHeight() / 2 - text.length * textSize;

        for (int i = 0; i < text.length; i++) {
            paint.setTextSize(textSize);
            paint.setColor(Color.YELLOW);
            canvas.drawText(text[i], leftPadding, topPadding + (i + 1) * textSize, paint);
        }
    }
}
