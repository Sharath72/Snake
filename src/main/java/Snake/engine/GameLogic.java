package Snake.engine;

import Snake.Entities.Apple;
import Snake.Entities.BigApple;
import Snake.Entities.Snake;
import Snake.Interfaces.IGameLogic;
import Snake.Interfaces.IGraphicInterface;
import Snake.Utils.Utils;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class GameLogic implements IGameLogic {
    // =============== Fields ===============
    private final IGraphicInterface gui;

    private boolean isGameRunning = true;

    private Snake snake = Snake.getInstance();
    private Direction direction = Direction.UP;

    private Apple apple;
    private BigApple bigApple;

    public enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    public GameLogic(IGraphicInterface gui) {
        this.gui = gui;
    }

    @Override
    public void initGame() {
        createContent();
        startSnake();
        startGenerateApples();
    }

    private void createContent() {
        gui.addObject(Snake.getInstance());
        plantApple();
    }

    private void startSnake() {
        new Thread(() -> {
            while (isGameRunning) {
                if (snake.isAlive()) {
                    switch (direction) {
                        case UP:
                            snake.moveUp();
                            break;
                        case LEFT:
                            snake.moveLeft();
                            break;
                        case DOWN:
                            snake.moveDown();
                            break;
                        case RIGHT:
                            snake.moveRight();
                            break;
                    }
                    checkSnakePosition();
                    Utils.sleep(100);
                }
            }
        }).start();
    }

    private void startGenerateApples() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(BigApple.TIME_TO_LIVE), event -> {
            if (Math.random() > 0.8) {
                if (bigApple != null) {
                    removeFruit(bigApple);
                }
                bigApple = BigApple.generateRandomBigApple();
                gui.addObject(bigApple);
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    // =============== Methods ===============
    private void checkSnakePosition() {
        if (snake.isAlive()) {
            checkPositionWithinBorders();
            checkPositionRelativeToFruit();
        }
    }

    private void checkPositionWithinBorders() {
        if (snake.getXCoordinate() < 10 || snake.getXCoordinate() > 490) {
            snake.setAlive(false);
        }
        if (snake.getYCoordinate() < 10 || snake.getYCoordinate() > 490) {
            snake.setAlive(false);
        }
    }

    private void checkPositionRelativeToFruit() {
        if (snake.distanceTo(apple) < 12) { // FIXME прописать нормальную дистанцию
            snake.eatFruit(bigApple);
            removeFruit(apple);
            //addPart(); // TODO
            plantApple();
        }
        if (bigApple != null && snake.distanceTo(bigApple) < 15) {
            snake.eatFruit(bigApple);
            //addPart(); // TODO
            removeFruit(bigApple);
        }
    }

    @Override
    public void refocusDirection(Direction direction) {
        this.direction = direction;
    }

    private void plantApple() {
        apple = Apple.generateRandomApple();
        gui.addObject(apple);
    }

    private void removeFruit(Apple apple) {
        gui.removeObject(apple);
        this.apple = null;
    }

    private void removeFruit(BigApple bigApple) {
        gui.removeObject(bigApple);
        this.bigApple = null;
    }

    private void addPart() { // TODO
        gui.addObject(snake.new PartOfBody());
    }

    @Override
    public void finishGame() {
        isGameRunning = false;
    }
}
