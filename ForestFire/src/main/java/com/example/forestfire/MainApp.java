package com.example.forestfire;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class MainApp extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	// Different possible states for each cell in the grid
	enum CellState {
		TREE, FIRE, ASH
	}

	// Simulation parameters and grid variables
	private int gridWidth, gridHeight;
	private double propagationProbability;
	private CellState[][] grid;
	private int stepsElapsed = 0;

	// For drawing the grid
	private Canvas canvas;
	private final int cellSize = 20; // each cell is 20x20 pixels

	// List to store the initial positions where the fire starts.
	// Each element is an integer array of size 2: [row, column]
	private final List<int[]> initialFirePositions = new ArrayList<>();

	@Override
	public void start(Stage primaryStage) throws Exception {
		// Read simulation configuration
		readConfiguration();

		// Create the grid with all TREE states
		setGrid(new CellState[getGridHeight()][getGridWidth()]);
		for (int row = 0; row < getGridHeight(); row++) {
			for (int col = 0; col < getGridWidth(); col++) {
				getGrid()[row][col] = CellState.TREE;
			}
		}

		// Create the initial fire positions based on configuration
		for (int[] pos : getInitialFirePositions()) {
			int row = pos[0];
			int col = pos[1];
			if (isValidCell(row, col)) {
				getGrid()[row][col] = CellState.FIRE;
			}
		}

		// Create a Canvas to draw the grid
		canvas = new Canvas(getGridWidth() * cellSize, getGridHeight() * cellSize);
		drawGrid();

		// Create a Time-line to update the simulation at fixed intervals
		Timeline timeline = new Timeline();
		timeline.getKeyFrames().add(new KeyFrame(Duration.millis(500), e -> {
			simulateStep();
			drawGrid();

			if (noFireRemaining()) {
				timeline.stop();
				System.out.println("Simulation ended.");
				System.out.println("Steps elapsed: " + stepsElapsed);
				System.out.println("Cells turned to ash: " + countAshCells());
			}
		}));

		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.play();

		// Set up the scene and stage
		StackPane root = new StackPane();
		root.getChildren().add(canvas);
		Scene scene = new Scene(root);
		primaryStage.setTitle("Forest Fire Simulation");
		primaryStage.setScene(scene);
		primaryStage.show();

	}

	/**
	 * Reads the configuration file and sets up the simulation parameters. Uses
	 * default values if the configuration file is not found.
	 */
	public void readConfiguration() {
		Properties prop = new Properties();
		try (InputStream input = new FileInputStream("src/main/resources/config.properties")) {
			prop.load(input);
		} catch (Exception ex) {
			System.out.println("Config file not found, using default settings.");
		}
		setGridWidth(Integer.parseInt(prop.getProperty("width", "30")));
		setGridHeight(Integer.parseInt(prop.getProperty("height", "30")));
		propagationProbability = Double.parseDouble(prop.getProperty("probability", "0.5"));
		String fireStart = prop.getProperty("fire_start", "15,15");

		// Parse the initial fire positions (format: "row,column;row,column;...")
		String[] positions = fireStart.split(";");
		for (String posStr : positions) {
			String[] parts = posStr.split(",");
			if (parts.length == 2) {
				try {
					int row = Integer.parseInt(parts[0].trim());
					int col = Integer.parseInt(parts[1].trim());
					getInitialFirePositions().add(new int[] { row, col });
				} catch (NumberFormatException e) {
					// Skip any invalid positions
				}
			}
		}
	}

	/**
	 * Checks if the specified cell is within the grid bounds.
	 */
	public boolean isValidCell(int row, int col) {
		return row >= 0 && row < getGridHeight() && col >= 0 && col < getGridWidth();
	}

	/**
	 * Returns true if no cell is currently on fire.
	 */
	private boolean noFireRemaining() {
		for (int row = 0; row < getGridHeight(); row++) {
			for (int col = 0; col < getGridWidth(); col++) {
				if (getGrid()[row][col] == CellState.FIRE) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Simulates one step of the fire propagation. Burning cells turn to ash and may
	 * fire adjacent tree cells.
	 */
	private void simulateStep() {
		// Create a new grid to store the next state
		CellState[][] newGrid = new CellState[getGridHeight()][getGridWidth()];
		for (int row = 0; row < getGridHeight(); row++) {
			System.arraycopy(getGrid()[row], 0, newGrid[row], 0, getGridWidth());
		}

		Random random = new Random();
		// Iterate over every cell in the grid
		for (int row = 0; row < getGridHeight(); row++) {
			for (int col = 0; col < getGridWidth(); col++) {
				if (getGrid()[row][col] == CellState.FIRE) {
					// The cell that is burning becomes ash
					newGrid[row][col] = CellState.ASH;
					// Try to burn the 4 adjacent cells (up, down, left, right)
					int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
					for (int[] dir : directions) {
						int newRow = row + dir[0];
						int newCol = col + dir[1];
						if (isValidCell(newRow, newCol) && getGrid()[newRow][newCol] == CellState.TREE) {
							if (random.nextDouble() < propagationProbability) {
								newGrid[newRow][newCol] = CellState.FIRE;
							}
						}
					}
				}
			}
		}
		setGrid(newGrid);
		stepsElapsed++;
	}

	/**
	 * Counts the number of cells that have turned to ash.
	 */
	private int countAshCells() {
		int count = 0;
		for (int row = 0; row < getGridHeight(); row++) {
			for (int col = 0; col < getGridWidth(); col++) {
				if (getGrid()[row][col] == CellState.ASH) {
					count++;
				}
			}
		}
		return count;
	}

	/**
	 * Draws the grid on the Canvas. Colors: - Trees: Forest Green - Fire: Red -
	 * Ash: Gray
	 */
	private void drawGrid() {
		GraphicsContext gc = canvas.getGraphicsContext2D();
		for (int row = 0; row < getGridHeight(); row++) {
			for (int col = 0; col < getGridWidth(); col++) {
				switch (getGrid()[row][col]) {
				case TREE:
					gc.setFill(Color.FORESTGREEN);
					break;
				case FIRE:
					gc.setFill(Color.RED);
					break;
				case ASH:
					gc.setFill(Color.GRAY);
					break;
				}
				gc.fillRect(col * cellSize, row * cellSize, cellSize, cellSize);
				gc.setStroke(Color.BLACK);
				gc.strokeRect(col * cellSize, row * cellSize, cellSize, cellSize);
			}
		}
	}

	/**
	 * @return the gridHeight
	 */
	public int getGridHeight() {
		return gridHeight;
	}

	/**
	 * @param gridHeight the gridHeight to set
	 */
	public void setGridHeight(int gridHeight) {
		this.gridHeight = gridHeight;
	}

	/**
	 * @return the gridWidth
	 */
	public int getGridWidth() {
		return gridWidth;
	}

	/**
	 * @param gridWidth the gridWidth to set
	 */
	public void setGridWidth(int gridWidth) {
		this.gridWidth = gridWidth;
	}

	/**
	 * @return the grid
	 */
	public CellState[][] getGrid() {
		return grid;
	}

	/**
	 * @param grid the grid to set
	 */
	public void setGrid(CellState[][] grid) {
		this.grid = grid;
	}

	/**
	 * @return the initialFirePositions
	 */
	public List<int[]> getInitialFirePositions() {
		return initialFirePositions;
	}

}
