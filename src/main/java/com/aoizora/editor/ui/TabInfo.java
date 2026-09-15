package com.aoizora.editor.ui;

import com.aoizora.editor.document.DocumentManager;
import eu.mihosoft.monacofx.MonacoFX;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;

public class TabInfo {

    private final Tab tab;
    private final MonacoFX editor;
    private final DocumentManager documentManager;
    private final Label titleLabel;

    public TabInfo(Tab tab, MonacoFX editor, DocumentManager documentManager, Label titleLabel) {
        this.tab = tab;
        this.editor = editor;
        this.documentManager = documentManager;
        this.titleLabel = titleLabel;
    }

    public Tab getTab() {
        return tab;
    }

    public MonacoFX getEditor() {
        return editor;
    }

    public DocumentManager getDocumentManager() {
        return documentManager;
    }

    public String getTitle() {
        return titleLabel.getText();
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setPathTooltip(String path) {
        tab.setTooltip(path != null ? new Tooltip(path) : null);
    }
}