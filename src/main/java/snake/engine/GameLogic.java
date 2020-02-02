package snake.engine;

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
import snake.entities.Orange;
import snake.entities.Snake;
import snake.entities.factory.FruitFactory;
import snake.interfaces.IGameLogic;
import snake.interfaces.IGraphicInterface;
import snake.main.SnakeGame;
import snake.results.Results;
import snake.results.ResultsSerializer;
import snake.utils.Utils;

public class GameLogic implements IGameLogic {
	// =============== Constants ===============
	private final IGraphicInterface GUI;
	private Snake snake = Snake.getInstance();

	// =============== Fields ===============
	private boolean isGameRunning = false;

	private Direction currentDirection = Direction.RIGHT;

	private Apple appleOnPane;
	private Orange orangeOnPane;

	private Stopwatch stopwatch = new Stopwatch();

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
		startMovingSnake();
		stopwatch.start();
		startGeneratingOranges();
	}

	private void startMovingSnake() {
		new Thread(() -> {
			while (isGameRunning) {
				snake.updatePreviousPositions();
				moveHead();
				snake.moveBody();
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
		if (isOutOfBorders() || snake.runIntoYourself()) {
			finishGame();
		}
		checkPositionRelativeToFruit();
	}

	private boolean isOutOfBorders() {
		return snake.getXCoordinate() <= -1 || snake.getXCoordinate() >= SnakeGame.SIZE + 1
				|| snake.getYCoordinate() <= -1 || snake.getYCoordinate() >= SnakeGame.SIZE + 1;
	}

	private void checkPositionRelativeToFruit() {
		if (snake.distanceTo(appleOnPane) < 10) {
			GUI.removeObject(appleOnPane);
			addPartToSnake(Apple.getValue());
			plantApple();
			snake.incrementAppleEatenCount();
		}
		if (orangeOnPane != null && snake.distanceTo(orangeOnPane) < 10) {
			GUI.removeObject(orangeOnPane);
			addPartToSnake(Orange.getValue());
			snake.incrementOrangesEatenCount();
		}
	}

	private void startGeneratingOranges() {
		final Timeline[] array = new Timeline[1];
		Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(Orange.TIME_TO_GENERATE), event -> {
			if (!isGameRunning) {
				array[0].stop();
				return;
			}
			if (Math.random() >= 0.9) {
				if (orangeOnPane != null) {
					GUI.removeObject(orangeOnPane);
				}
				orangeOnPane = FruitFactory.INSTANCE.createOrangeOnRandomPosition();
				GUI.addObject(orangeOnPane);
			}
		}));
		array[0] = timeline;
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
			snake.getPartsOfSnake().add(partOfSnake);
			GUI.addObject(partOfSnake);
		}
	}

	@Override
	public void finishGame() {
		isGameRunning = false;
		stopwatch.stop();
		updateBestResultsIfNeeded();
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
			if (snake.getPartsOfSnake().size() < 624) {
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
				clearData();
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

	private void clearData() {
		GUI.clear();
		snake.dropData();
		snake.getPartsOfSnake().clear();
	}

	private Results getCurrentResults() {
		Results results = new Results();
		results.setCountOfEatenApples(snake.getCountOfEatenApples());
		results.setCountOfEatenOranges(snake.getCountOfEatenOranges());
		results.setLength(snake.getLength());
		results.setTimeMinutes(stopwatch.getTimeMinutes());
		return results;
	}

	// отдать другому классу
	private void updateBestResultsIfNeeded() {
		Results bestResults = ResultsSerializer.deserializeResults();
		Results currentResults = getCurrentResults();
		if (bestResults == null || getCurrentResults().compareTo(bestResults) > 0) {
			ResultsSerializer.serializeResults(currentResults);
		}
	}

	public enum Direction {
		UP, DOWN, LEFT, RIGHT;

		public Direction getOppositeDirection() {
			switch (this) {
			case UP:
				return DOWN;
			case LEFT:
				return RIGHT;
			case DOWN:
				return UP;
			case RIGHT:
				return LEFT;
			default:
				return null;
			}
		}
	}
}