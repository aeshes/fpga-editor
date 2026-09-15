package com.aoizora.editor;

import com.aoizora.editor.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Program extends Application {

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.UNDECORATED);
        for (int size : new int[]{16, 24, 32, 48, 64, 128, 256}) {
            try (var stream = getClass().getResourceAsStream("/images/app-icon-" + size + ".png")) {
                if (stream != null) {
                    stage.getIcons().add(new Image(stream));
                }
            } catch (Exception ignored) {
                // иконка не критична
            }
        }
        new MainWindow(stage).show();
    }
}