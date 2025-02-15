package com.example.forestfire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MainAppTest {
	private MainApp app;

	@BeforeEach
	void setUp() {
		app = new MainApp();
		app.readConfiguration(); // Ensure configuration is read
		app.setGrid(new MainApp.CellState[app.getGridHeight()][app.getGridWidth()]); // Manually initialize grid

		// Default all cells to TREE
		for (int row = 0; row < app.getGridHeight(); row++) {
			for (int col = 0; col < app.getGridWidth(); col++) {
				app.getGrid()[row][col] = MainApp.CellState.TREE;
			}
		}

		// Set fire to initial positions
		for (int[] pos : app.getInitialFirePositions()) {
			int row = pos[0];
			int col = pos[1];
			if (app.isValidCell(row, col)) {
				app.getGrid()[row][col] = MainApp.CellState.FIRE;
			}
		}
	}

	@Test
	void testGridInitialization() {
		app.readConfiguration();
		assertNotNull(app.getGrid(), "Grid should be initialized.");
		assertEquals(app.getGridHeight(), app.getGrid().length, "Grid height should match configuration.");
		assertEquals(app.getGridWidth(), app.getGrid()[0].length, "Grid width should match configuration.");
	}

	@Test
	void testFireStartPosition() {
		app.readConfiguration();
		boolean hasFire = false;
		for (int row = 0; row < app.getGridHeight(); row++) {
			for (int col = 0; col < app.getGridWidth(); col++) {
				if (app.getGrid()[row][col] == MainApp.CellState.FIRE) {
					hasFire = true;
					break;
				}
			}
		}
		assertTrue(hasFire, "At least one cell should be on fire at start.");
	}

}
