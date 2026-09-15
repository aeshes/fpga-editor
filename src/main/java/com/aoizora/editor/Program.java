package com.aoizora.editor;

import com.aoizora.editor.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public class Program extends Application {

    @Override
    public void start(Stage stage) {
        new MainWindow(stage).show();
    }
}