package com.aoizora.editor.tools;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class TextAreaOutputSink implements OutputSink {

    private final TextArea textArea;

    public TextAreaOutputSink(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void appendLine(String text) {
        Platform.runLater(() -> textArea.appendText(text + "\n"));
    }

    @Override
    public void clear() {
        Platform.runLater(textArea::clear);
    }
}
