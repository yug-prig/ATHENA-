package com.library.app;

import com.library.db.DatabaseManager;
import com.library.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main JavaFX Application Entrypoint for the Library Management System.
 */
public class MainApp extends Application {

    @Override
    public void init() throws Exception {
        super.init();
        // Initialize SQLite DB, generate tables if needed, and seed initial dataset
        DatabaseManager.initializeDatabase();
    }

    @Override
    public void start(Stage primaryStage) {
        MainView mainView;
        mainView = new MainView();

        Scene scene = new Scene(mainView, 1280, 800);
        try {
            String css = getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Could not load styles.css: " + e.getMessage());
        }

        primaryStage.setTitle("Athena — Modern Library Management System");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
