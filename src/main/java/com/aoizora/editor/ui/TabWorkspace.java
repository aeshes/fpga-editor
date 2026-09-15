package com.aoizora.editor.ui;

import com.aoizora.editor.document.DocumentManager;
import com.aoizora.editor.language.VerilogCodeArea;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TabWorkspace {

    private final TabPane tabPane;
    private final List<TabInfo> openTabs = new ArrayList<>();
    private int untitledCounter = 1;

    public TabWorkspace(Stage stage) {
        this.tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        tabPane.getStyleClass().add("hard-tab-pane");
    }

    public TabPane getTabPane() {
        return tabPane;
    }

    public List<TabInfo> getOpenTabs() {
        return openTabs;
    }

    public TabInfo getActiveTabInfo() {
        return getInfoForTab(tabPane.getSelectionModel().getSelectedItem());
    }

    public TabInfo getInfoForTab(Tab tab) {
        for (TabInfo info : openTabs) {
            if (info.getTab() == tab) return info;
        }
        return null;
    }

    public void openTabWithContent(String content, File file) {
        VerilogCodeArea editor = new VerilogCodeArea();
        DocumentManager dm = new DocumentManager();
        dm.setContent(content);
        if (file != null) dm.setCurrentFile(file);
        String label = file != null ? file.getName() : "Untitled-" + untitledCounter++;
        TabInfo info = createTab(dm, editor, label);
        info.setPathTooltip(file != null ? file.getAbsolutePath() : "Безымянный документ");
        syncEditorFromTab(info);
    }

    public void openFileInTab(File file) throws IOException {
        for (TabInfo info : openTabs) {
            File openFile = info.getDocumentManager().getCurrentFile();
            if (openFile != null && openFile.equals(file)) {
                tabPane.getSelectionModel().select(info.getTab());
                return;
            }
        }
        openTabWithContent(Files.readString(file.toPath()), file);
    }

    public boolean closeTab(TabInfo info) {
        if (!confirmDiscardIfNeeded(info)) return false;
        openTabs.remove(info);
        tabPane.getTabs().remove(info.getTab());
        return true;
    }

    public boolean closeActiveTab() {
        TabInfo info = getActiveTabInfo();
        return info != null && closeTab(info);
    }

    public void closeOtherTabs() {
        TabInfo active = getActiveTabInfo();
        for (TabInfo info : new ArrayList<>(openTabs)) {
            if (info != active) closeTab(info);
        }
    }

    public void closeAllTabs() {
        for (TabInfo info : new ArrayList<>(openTabs)) {
            closeTab(info);
        }
    }

    public void syncEditorFromTab(TabInfo info) {
        info.getEditor().replaceText(info.getDocumentManager().getContent());
    }

    public void syncTabFromEditor(TabInfo info) {
        info.getDocumentManager().setContent(info.getEditor().getText());
    }

    private TabInfo createTab(DocumentManager dm, VerilogCodeArea editor, String label) {
        Tab tab = new Tab();
        tab.setClosable(false);
        tab.setContent(editor.getNode());

        Label title = new Label(label);
        title.getStyleClass().add("hard-tab-title");

        Label close = new Label("\u2715");
        close.getStyleClass().add("hard-tab-close");
        close.setOnMouseClicked(e -> {
            e.consume();
            closeTab(getInfoForTab(tab));
        });

        HBox header = new HBox(8, title, close);
        header.setAlignment(Pos.CENTER);
        tab.setGraphic(header);

        header.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                closeTab(getInfoForTab(tab));
            }
        });

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        TabInfo info = new TabInfo(tab, editor, dm, title);
        openTabs.add(info);
        tab.setOnClosed(e -> openTabs.remove(info));
        return info;
    }

    private boolean confirmDiscardIfNeeded(TabInfo info) {
        String docContent = info.getDocumentManager().getContent();
        String editorContent = info.getEditor().getText();
        if (docContent.equals(editorContent)) return true;
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Вкладка содержит несохранённые изменения. Закрыть без сохранения?",
                ButtonType.YES, ButtonType.NO
        );
        alert.setHeaderText("Несохранённые изменения");
        alert.showAndWait();
        return alert.getResult() == ButtonType.YES;
    }
}
