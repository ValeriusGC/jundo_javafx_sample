package com.gdetotut.samples.jundo.javafx;

import com.gdetotut.samples.jundo.javafx.v1.JUndoTab_V1;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Приложение для демонстрации библиотеки JUndo.
 */
public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("Undo libs comparing");
        TabPane tabPane = new TabPane();

        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(5.0));

        tabPane.getTabs().add(new JUndoTab_V1("JUndo_V1", tabPane));

        borderPane.setCenter(tabPane);
        Scene scene = new Scene(borderPane, 400, 600, Color.WHITE);

        primaryStage.setScene(scene);
        primaryStage.show();

    }

}
