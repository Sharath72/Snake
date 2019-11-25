package snake.engine;

import java.util.LinkedList;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
import snake.entities.Cell;
import snake.entities.Snake;
import snake.entities.factory.FruitFactory;
import snake.interfaces.IGameLogic;
import snake.interfaces.IGraphicInterface;
import snake.main.SnakeGame;
import snake.utils.Utils;

/**
 * 3.Таймер пропадания больших яблок 5.Поуберать циклические зависимости
 * bigApple spawn out of borders
 */
public class GameLogic implements IGameLogic {
	// =============== Constants ===============
	private final IGraphicInterface GUI;
	private Snake snake = Snake.getInstance();
	public static final List<Snake.PartOfSnake> PARTS = new LinkedList<>();
	
	// =============== Fields ===============
	private boolean isGameRunning = false;

	private Timeline timeline; // lishnee

	private Direction currentDirection = Direction.RIGHT;

	private Apple appleOnPane;
	private BigApple bigAppleOnPane;

	// private StringProperty score = new SimpleStringProperty();

	// =============== Constructors ===============
	public GameLogic(IGraphicInterface gui) {
		GUI = gui;
	}

	// =============== Methods ===============
	@Override
	public void initGame() {
		GUI.addObject(snake);
		snake.setPosition((SnakeGame.SIZE + (snake.getWidth()) / 2) / 2 - 5,
				(SnakeGame.SIZE + (snake.getWidth() / 2)) / 2 - 5);
		isGameRunning = true;
		plantApple();
		PARTS.clear();
		startMovingSnake();
		startGeneratingBigApples();
	}

	public void moveBody() {
		if (PARTS.size() != 0) {
			PARTS.get(0).follow(snake);
			for (int i = PARTS.size() - 1; i > 0; i--) {
				PARTS.get(i).follow(PARTS.get(i - 1));
			}
		}
	}

	public void updatePreviousPositions() {
		snake.updatePreviousPosition();
		if (PARTS.size() != 0) {
			PARTS.forEach(Cell::updatePreviousPosition);
		}
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
		switch (currentDirection) {
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
	}

	private void checkSnakePosition() {
		if (isOutOfBorders() || runIntoYourself()) {
			finishGame();
		}
		checkPositionRelativeToFruit();
	}

	private boolean isOutOfBorders() {
		return snake.getXCoordinate() <= -1 || snake.getXCoordinate() >= SnakeGame.SIZE + 1
				|| snake.getYCoordinate() <= -1 || snake.getYCoordinate() >= SnakeGame.SIZE + 1;
	}

	private boolean runIntoYourself() {
		if (PARTS.size() != 0) {
			for (Snake.PartOfSnake part : PARTS) {
				if (snake.getXCoordinate() == part.getXCoordinate()
						&& snake.getYCoordinate() == part.getYCoordinate()) {
					part.setFill(Color.RED);
					return true;
				}
			}
		}

		return false;
	}

	private void checkPositionRelativeToFruit() {
		if (snake.distanceTo(appleOnPane) < 10) {
			GUI.removeObject(appleOnPane);
			plantApple();

			addPartToSnake(Apple.getValue());
		}
		if (bigAppleOnPane != null && snake.distanceTo(bigAppleOnPane) < 15) {
			GUI.removeObject(bigAppleOnPane);
			addPartToSnake(BigApple.getValue());
		}
	}

	private void startGeneratingBigApples() {
		timeline = new Timeline(new KeyFrame(Duration.seconds(BigApple.TIME_TO_GENERATE), event -> {
			if (!isGameRunning) {
				timeline.stop(); // TODO
				return;
			}
			if (Math.random() >= 0.3) {
				if (bigAppleOnPane != null) {
					GUI.removeObject(bigAppleOnPane);
				}
				bigAppleOnPane = FruitFactory.INSTANCE.createBigAppleOnRandomPostion();
				GUI.addObject(bigAppleOnPane);
			}
		}));
		timeline.setCycleCount(Animation.INDEFINITE);
		timeline.play();
	}

	@Override
	public void changeDirection(Direction direction) {
		if (this.currentDirection != direction.getOppositeDirection()) {
			this.currentDirection = direction;
		}
	}

	private void plantApple() {
		appleOnPane = FruitFactory.INSTANCE.createAppleOnRandomPosition();
		GUI.addObject(appleOnPane);
	}

	private void addPartToSnake(int count) {
		for (int i = 0; i < count; i++) {
			Snake.PartOfSnake partOfSnake = new Snake.PartOfSnake();
			PARTS.add(partOfSnake);
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

			Label label = new Label();
			if (PARTS.size() < 624) {
				label.setText("Loose :(");
				label.setTextFill(Color.rgb(255, 0, 0));
			} else {
				label.setText("Win :D");
				label.setTextFill(Color.rgb(0, 255, 0));
			}

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
			default:
				return null;
			}
		}
	}
}