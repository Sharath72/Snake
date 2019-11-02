package snake.engine;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import snake.entities.Apple;
import snake.entities.BigApple;
import snake.entities.Snake;
import snake.interfaces.IGameLogic;
import snake.interfaces.IGraphicInterface;
import snake.noname.Cell;
import snake.noname.FruitFactory;
import snake.pane.SnakeGame;
import snake.utils.Utils;

/**
 * 1.Прописать нормальные спавны яблокам 2.Прописать норм дистанцию при кушании
 * яблок 3.Таймер пропадания больших яблок 5.Поуберать циклические зависимости
 */
public class GameLogic implements IGameLogic {
	// =============== Constants ===============
	private final IGraphicInterface GUI;
	private final Snake SNAKE = Snake.getInstance();
	public static final List<Snake.PartOfSnake> PARTS = new ArrayList<>(10);

	// =============== Fields ===============
	private boolean isGameRunning = false;

	private Timeline timeline; // lishnee

	private Direction direction = Direction.UP;

	private Apple apple;
	private BigApple bigApple;

	// =============== Constructors ===============
	public GameLogic(IGraphicInterface gui) {
		GUI = gui;
	}

	// =============== Methods ===============
	public final void add(Snake.PartOfSnake partOfSnake) {
		PARTS.add(partOfSnake);
	}

	public void moveBody() {
		if (PARTS.size() != 0) {
			PARTS.get(0).follow(SNAKE);
			for (int i = PARTS.size() - 1; i > 0; i--) {
				PARTS.get(i).follow(PARTS.get(i - 1));
			}
		}
	}

	public void updatePreviousPositions() {
		SNAKE.updatePreviousPosition();
		if (PARTS.size() != 0) {
			PARTS.forEach(Cell::updatePreviousPosition);
		}
	}

	@Override
	public void initGame() {
		GUI.addObject(SNAKE);
		SNAKE.setPosition((SnakeGame.SIZE + (SNAKE.getWidth()) / 2) / 2 - 5, (SnakeGame.SIZE + (SNAKE.getWidth() / 2)) / 2 - 5);
		isGameRunning = true;
		plantApple();
		PARTS.clear();
		startMovingSnake();
		startGeneratingBigApples();
	}

	private void startMovingSnake() {
		new Thread(() -> {
			while (isGameRunning) {
				updatePreviousPositions();
				moveHead();
				moveBody();
				checkSnakePosition();
				Utils.sleep(100);
			}
		}).start();
	}

	private void moveHead() {
		switch (direction) {
		case UP:
			SNAKE.moveUp();
			break;
		case LEFT:
			SNAKE.moveLeft();
			break;
		case DOWN:
			SNAKE.moveDown();
			break;
		case RIGHT:
			SNAKE.moveRight();
			break;
		}
	}

	private void checkSnakePosition() {
		if (isOutOfBorders() || runIntoYourself()) {
			finishGame();
		}
		checkPositionRelativeToFruit();
	}

	private boolean isOutOfBorders() {
		return SNAKE.getXCoordinate() <= -1 || SNAKE.getXCoordinate() >= SnakeGame.SIZE + 1
				|| SNAKE.getYCoordinate() <= -1 || SNAKE.getYCoordinate() >= SnakeGame.SIZE + 1;
	}

	private boolean runIntoYourself() {
		if (PARTS.size() != 0) {
			for (Snake.PartOfSnake part : PARTS) {
				if (SNAKE.getXCoordinate() == part.getXCoordinate()
						&& SNAKE.getYCoordinate() == part.getYCoordinate()) {
					part.setFill(Color.RED);
					return true;
				}
			}
		}

		return false;
	}

	private void checkPositionRelativeToFruit() {// TODO refactor, adaptive
		if (SNAKE.distanceTo(apple) < 12) {
			GUI.removeObject(apple);
			plantApple();
			addPartToSnake(Apple.getValue());
		}
		if (bigApple != null && SNAKE.distanceTo(bigApple) < 15) {
			GUI.removeObject(bigApple);
			addPartToSnake(BigApple.getValue());
		}
	}

	private void startGeneratingBigApples() {
		timeline = new Timeline(new KeyFrame(Duration.seconds(BigApple.TIME_TO_GENERATE), event -> {
			if (!isGameRunning) {
				timeline.stop(); // TODO
				return;
			}
			if (Math.random() >= 0.90) {
				bigApple = FruitFactory.INSTANCE.createBigAppleOnRandomPostion();
				GUI.addObject(bigApple);
			}
		}));
		timeline.setCycleCount(Animation.INDEFINITE);
		timeline.play();
	}

	@Override
	public void changeDirection(Direction direction) {
		if (this.direction != direction.getOppositeDirection()) {
			this.direction = direction;
		}
	}

	private void plantApple() {
		apple = FruitFactory.INSTANCE.createAppleOnRandomPosition();
		GUI.addObject(apple);
	}

	private void addPartToSnake(int count) {
		for (int i = 0; i < count; i++) {
			Snake.PartOfSnake partOfSnake = new Snake.PartOfSnake();
			add(partOfSnake);
			GUI.addObject(partOfSnake);
		}
	}

	@Override
	public void finishGame() {
		isGameRunning = false;
		restartWindow();
	}

	private void restartWindow() {
		Platform.runLater(() -> {
			Stage stage = new Stage();
			stage.setTitle("Restart");
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setResizable(false);

			Pane root = new Pane();
			Scene scene = new Scene(root, 300, 100);

			Label label = new Label("Loose :(");
			label.setStyle("-fx-font-size: 20; -fx-font-weight: bold");

			Button restartButton = new Button("Restart");
			restartButton.setDefaultButton(true);
			restartButton.setOnAction(e -> {
				GUI.clear();
				stage.hide();
				initGame();
			});

			Button exitButton = new Button("Exit");
			exitButton.setCancelButton(true);
			exitButton.setOnAction(e -> System.exit(1));

			restartButton.setLayoutX(230);
			restartButton.setLayoutY(70);
			exitButton.setLayoutX(170);
			exitButton.setLayoutY(70);
			label.setLayoutX(125);
			label.setLayoutY(30);

			root.getChildren().addAll(restartButton, exitButton, label);

			stage.setScene(scene);
			stage.show();
		});
	}

	// =============== Enum ===============
	public enum Direction {
		UP, DOWN, LEFT, RIGHT;

		public Direction getOppositeDirection() {
			switch (this) {
			case UP:
				return Direction.DOWN;
			case LEFT:
				return Direction.RIGHT;
			case DOWN:
				return Direction.UP;
			case RIGHT:
				return Direction.LEFT;
			}
			return null;
		}
	}
}