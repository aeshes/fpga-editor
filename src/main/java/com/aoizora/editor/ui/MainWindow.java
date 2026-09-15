package com.aoizora.editor.ui;

import com.aoizora.editor.document.DocumentManager;
import com.aoizora.editor.tools.*;
import com.github.mouse0w0.darculafx.DarculaFX;
import eu.mihosoft.monacofx.MonacoFX;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainWindow {

    private static final String APP_TITLE = "SystemVerilog FPGA Editor";
    private static final String DEFAULT_CONTENT =
            "module alu(input logic [7:0] a, b, output logic [7:0] y);\n" +
            "    assign y = a + b;\n" +
            "endmodule\n";

    private final Stage stage;
    private final ToolRunner toolRunner;
    private final TabPane tabPane;
    private final List<TabInfo> openTabs = new ArrayList<>();
    private int untitledCounter = 1;

    public MainWindow(Stage stage) {
        this.stage = stage;
        this.toolRunner = new ToolRunner();
        this.tabPane = buildTabPane();
    }

    public void show() {
        setupTools();

        TextArea outputArea = createOutputArea();
        OutputSink outputSink = new TextAreaOutputSink(outputArea);
        MenuBar menuBar = buildMenuBar();
        HBox toolbar = buildToolbar(outputSink);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(menuBar, spacer, toolbar);
        topBar.setAlignment(Pos.CENTER_LEFT);

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabPane);
        root.setBottom(outputArea);
        BorderPane.setMargin(outputArea, new Insets(4));
        BorderPane.setMargin(tabPane, new Insets(0, 0, 0, 0));

        Scene scene = new Scene(root, 1100, 700);
        DarculaFX.applyDarculaStyle(scene);
        scene.getStylesheets().add(
                getClass().getResource("/styles/tabs.css").toExternalForm()
        );

        openTabWithContent(DEFAULT_CONTENT, null);
        updateTitle();
        stage.setScene(scene);
        stage.show();
    }

    private TabPane buildTabPane() {
        TabPane pane = new TabPane();
        pane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        pane.getStyleClass().add("hard-tab-pane");
        pane.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldTab, newTab) -> updateTitle()
        );
        return pane;
    }

    private void setupTools() {
        toolRunner.register(new VerilatorLintTool());
        toolRunner.register(new IcarusSimulationTool());
        toolRunner.register(new GtkwaveTool());
    }

    private TextArea createOutputArea() {
        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPromptText("Вывод Verilator / Icarus будет отображаться здесь...");
        return outputArea;
    }

    private HBox buildToolbar(OutputSink outputSink) {
        HBox toolbar = new HBox(8);
        for (Tool tool : toolRunner.getTools()) {
            Button btn = new Button(tool.getName());
            btn.setOnAction(e -> {
                TabInfo info = getActiveTabInfo();
                if (info == null) {
                    return;
                }
                syncEditorFromTab(info);
                String filePath = info.getDocumentManager().getAbsolutePath();
                tool.execute(outputSink, filePath);
            });
            toolbar.getChildren().add(btn);
        }
        return toolbar;
    }

    private MenuBar buildMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.useSystemMenuBarProperty().set(false);

        Menu fileMenu = new Menu("Файл");

        MenuItem newItem = new MenuItem("Новый");
        newItem.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));
        newItem.setOnAction(e -> fileNew());

        MenuItem openItem = new MenuItem("Открыть...");
        openItem.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
        openItem.setOnAction(e -> fileOpen());

        MenuItem saveItem = new MenuItem("Сохранить");
        saveItem.setAccelerator(KeyCombination.keyCombination("Ctrl+S"));
        saveItem.setOnAction(e -> fileSave());

        MenuItem saveAsItem = new MenuItem("Сохранить как...");
        saveAsItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+S"));
        saveAsItem.setOnAction(e -> fileSaveAs());

        SeparatorMenuItem sep1 = new SeparatorMenuItem();

        MenuItem closeTabItem = new MenuItem("Закрыть вкладку");
        closeTabItem.setAccelerator(KeyCombination.keyCombination("Ctrl+W"));
        closeTabItem.setOnAction(e -> closeActiveTab());

        MenuItem closeOthersItem = new MenuItem("Закрыть другие");
        closeOthersItem.setOnAction(e -> closeOtherTabs());

        MenuItem closeAllItem = new MenuItem("Закрыть все вкладки");
        closeAllItem.setOnAction(e -> closeAllTabs());

        SeparatorMenuItem sep2 = new SeparatorMenuItem();

        MenuItem exitItem = new MenuItem("Выход");
        exitItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Q"));
        exitItem.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(
                newItem, openItem, saveItem, saveAsItem,
                sep1, closeTabItem, closeOthersItem, closeAllItem,
                sep2, exitItem
        );
        menuBar.getMenus().add(fileMenu);

        return menuBar;
    }

    // ================== Вкладки ==================

    private TabInfo createTab(DocumentManager documentManager, MonacoFX editor, String label) {
        Tab tab = new Tab();
        tab.setClosable(false);
        tab.setContent(editor);

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
            if (e.getButton() == javafx.scene.input.MouseButton.MIDDLE) {
                if (!closeTab(getInfoForTab(tab))) {
                    e.consume();
                }
            }
        });

        tab.setOnSelectionChanged(e -> updateTitle());
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        TabInfo info = new TabInfo(tab, editor, documentManager, title);
        openTabs.add(info);
        tab.setOnClosed(e -> openTabs.remove(info));
        return info;
    }

    private TabInfo getInfoForTab(Tab tab) {
        for (TabInfo info : openTabs) {
            if (info.getTab() == tab) {
                return info;
            }
        }
        return null;
    }

    private TabInfo getActiveTabInfo() {
        return getInfoForTab(tabPane.getSelectionModel().getSelectedItem());
    }

    private void openTabWithContent(String content, File file) {
        MonacoFX editor = createConfiguredEditor();
        DocumentManager dm = new DocumentManager();
        dm.setContent(content);
        if (file != null) {
            dm.setCurrentFile(file);
        }
        String label = file != null ? file.getName() : "Untitled-" + untitledCounter++;
        TabInfo info = createTab(dm, editor, label);
        info.setPathTooltip(file != null ? file.getAbsolutePath() : "Безымянный документ");
        syncEditorFromTab(info);
    }

    private MonacoFX createConfiguredEditor() {
        MonacoFX editor = new MonacoFX();
        editor.getEditor().setCurrentTheme("vs-dark");
        editor.getEditor().setCurrentLanguage("verilog");
        return editor;
    }

    private boolean closeTab(TabInfo info) {
        if (!confirmDiscardIfNeeded(info)) {
            return false;
        }
        openTabs.remove(info);
        tabPane.getTabs().remove(info.getTab());
        updateTitle();
        return true;
    }

    private void closeActiveTab() {
        TabInfo info = getActiveTabInfo();
        if (info != null) {
            closeTab(info);
        }
    }

    private void closeOtherTabs() {
        TabInfo active = getActiveTabInfo();
        for (TabInfo info : new ArrayList<>(openTabs)) {
            if (info != active) {
                closeTab(info);
            }
        }
    }

    private void closeAllTabs() {
        for (TabInfo info : new ArrayList<>(openTabs)) {
            closeTab(info);
        }
    }

    private boolean confirmDiscardIfNeeded(TabInfo info) {
        String docContent = info.getDocumentManager().getContent();
        String editorContent = info.getEditor().getEditor().getDocument().getText();
        boolean dirty = !docContent.equals(editorContent);
        if (!dirty) {
            return true;
        }
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Вкладка содержит несохранённые изменения. Закрыть без сохранения?",
                ButtonType.YES, ButtonType.NO
        );
        alert.setHeaderText("Несохранённые изменения");
        alert.showAndWait();
        return alert.getResult() == ButtonType.YES;
    }

    // ================== Файл ==================

    private void fileNew() {
        openTabWithContent(DEFAULT_CONTENT, null);
        updateTitle();
    }

    private void fileOpen() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Открыть файл");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SystemVerilog", "*.sv", "*.svh", "*.v"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            try {
                openTabWithContent(
                        java.nio.file.Files.readString(file.toPath()),
                        file
                );
                updateTitle();
            } catch (IOException ex) {
                showError("Не удалось открыть файл: " + ex.getMessage());
            }
        }
    }

    private void fileSave() {
        TabInfo info = getActiveTabInfo();
        if (info == null) {
            return;
        }
        syncEditorFromTab(info);
        DocumentManager dm = info.getDocumentManager();
        if (dm.hasFile()) {
            try {
                dm.save(dm.getCurrentFile());
            } catch (IOException ex) {
                showError("Не удалось сохранить файл: " + ex.getMessage());
            }
        } else {
            fileSaveAs();
        }
    }

    private void fileSaveAs() {
        TabInfo info = getActiveTabInfo();
        if (info == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить файл");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SystemVerilog", "*.sv"),
                new FileChooser.ExtensionFilter("SystemVerilog Header", "*.svh"),
                new FileChooser.ExtensionFilter("Verilog", "*.v"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );
        DocumentManager dm = info.getDocumentManager();
        if (dm.hasFile()) {
            chooser.setInitialDirectory(dm.getCurrentFile().getParentFile());
            chooser.setInitialFileName(dm.getCurrentFile().getName());
        }
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            syncEditorFromTab(info);
            try {
                dm.save(file);
                info.setTitle(file.getName());
                info.setPathTooltip(file.getAbsolutePath());
                updateTitle();
            } catch (IOException ex) {
                showError("Не удалось сохранить файл: " + ex.getMessage());
            }
        }
    }

    // ================== Синхронизация ==================

    private void syncEditorFromTab(TabInfo info) {
        info.getEditor().getEditor().getDocument().setText(
                info.getDocumentManager().getContent()
        );
    }

    private void syncTabFromEditor(TabInfo info) {
        info.getDocumentManager().setContent(
                info.getEditor().getEditor().getDocument().getText()
        );
    }

    private void updateTitle() {
        TabInfo info = getActiveTabInfo();
        String name = info != null ? info.getTitle() : "Нет открытых файлов";
        stage.setTitle(name + " — " + APP_TITLE);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Ошибка");
        alert.showAndWait();
    }
}