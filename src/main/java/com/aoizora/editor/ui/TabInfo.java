package com.aoizora.editor.ui;

import com.aoizora.editor.document.DocumentManager;
import eu.mihosoft.monacofx.MonacoFX;
import javafx.scene.control.Tab;

public class TabInfo {

    private final Tab tab;
    private final MonacoFX editor;
    private final DocumentManager documentManager;

    public TabInfo(Tab tab, MonacoFX editor, DocumentManager documentManager) {
        this.tab = tab;
        this.editor = editor;
        this.documentManager = documentManager;
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
}