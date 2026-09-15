package com.aoizora.editor.ui;

import com.aoizora.editor.document.DocumentManager;
import com.aoizora.editor.language.VerilogCodeArea;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;

public class TabInfo {

    private final Tab tab;
    private final VerilogCodeArea editor;
    private final DocumentManager documentManager;
    private final Label titleLabel;

    public TabInfo(Tab tab, VerilogCodeArea editor, DocumentManager documentManager, Label titleLabel) {
        this.tab = tab;
        this.editor = editor;
        this.documentManager = documentManager;
        this.titleLabel = titleLabel;
    }

    public Tab getTab() {
        return tab;
    }

    public VerilogCodeArea getEditor() {
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