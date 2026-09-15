package com.aoizora.editor.ui;

import com.aoizora.editor.document.DocumentManager;
import com.aoizora.editor.language.VerilogCodeArea;
import com.aoizora.editor.project.Project;
import com.aoizora.editor.project.ProjectParser;
import com.aoizora.editor.tools.*;
import com.github.mouse0w0.darculafx.DarculaFX;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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
    private final ProjectTreeView projectTree;
    private final List<TabInfo> openTabs = new ArrayList<>();
    private Project currentProject;
    private int untitledCounter = 1;
    private MenuBar menuBar;
    private Button burgerButton;

    public MainWindow(Stage stage) {
        this.stage = stage;
        this.toolRunner = new ToolRunner();
        this.tabPane = buildTabPane();
        this.projectTree = new ProjectTreeView();
        this.projectTree.setOnOpenFile(this::openFileInTab);
    }

    public void show() {
        setupTools();

        TextArea outputArea = createOutputArea();
        OutputSink outputSink = new TextAreaOutputSink(outputArea);
        menuBar = buildMenuBar(outputSink);
        burgerButton = buildBurgerMenu(menuBar);
        HBox toolbar = buildToolbar(outputSink);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(burgerButton, menuBar, spacer, toolbar);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(2, 4, 2, 6));

        collapseMenu();

        VBox projectPanel = buildProjectPanel();
        SplitPane centerSplit = new SplitPane(projectPanel, tabPane);
        centerSplit.setDividerPositions(0.22);
        centerSplit.getStyleClass().add("main-split");
        SplitPane.setResizableWithParent(projectPanel, false);

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(centerSplit);
        root.setBottom(outputArea);
        BorderPane.setMargin(outputArea, new Insets(4));
        BorderPane.setMargin(centerSplit, new Insets(0));

        Scene scene = new Scene(root, 1100, 700);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, this::onMainScenePressed);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::onMainSceneKeyPressed);
        DarculaFX.applyDarculaStyle(scene);
        scene.getStylesheets().addAll(
                getClass().getResource("/styles/tabs.css").toExternalForm(),
                getClass().getResource("/styles/project.css").toExternalForm(),
                getClass().getResource("/styles/editor.css").toExternalForm(),
                getClass().getResource("/styles/menu.css").toExternalForm()
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

    private VBox buildProjectPanel() {
        Label header = new Label("ОБОЗРЕВАТЕЛЬ ПРОЕКТА");
        header.getStyleClass().add("project-panel-header");
        header.setMaxWidth(Double.MAX_VALUE);

        VBox panel = new VBox(header, projectTree);
        panel.getStyleClass().add("project-panel");
        panel.setMinWidth(160);
        panel.setPrefWidth(240);
        VBox.setVgrow(projectTree, Priority.ALWAYS);
        return panel;
    }

    private HBox buildToolbar(OutputSink outputSink) {
        HBox toolbar = new HBox(8);
        for (Tool tool : toolRunner.getTools()) {
            Button btn = new Button(tool.getName());
            btn.setOnAction(e -> runTool(tool, outputSink));
            toolbar.getChildren().add(btn);
        }
        return toolbar;
    }

    /**
     * Бургер-кнопка: показывает/скрывает главное меню.
     * Бургер и полное меню занимают одно место: при открытом меню бургер скрыт.
     */
    private Button buildBurgerMenu(MenuBar menuBar) {
        Button burger = new Button();
        burger.getStyleClass().add("burger-menu");
        burger.setGraphic(createBurgerIcon());
        burger.setFocusTraversable(false);
        burger.setTooltip(new Tooltip("Главное меню"));
        burger.setOnAction(e -> {
            if (menuBar.isVisible()) {
                collapseMenu();
            } else {
                expandMenu();
            }
        });
        return burger;
    }

    private void expandMenu() {
        burgerButton.setVisible(false);
        burgerButton.setManaged(false);
        menuBar.setVisible(true);
        menuBar.setManaged(true);
    }

    private void collapseMenu() {
        burgerButton.setVisible(true);
        burgerButton.setManaged(true);
        menuBar.setVisible(false);
        menuBar.setManaged(false);
    }

    private void onMainScenePressed(MouseEvent event) {
        if (menuBar.isVisible() && !isInsideMenuBar(event)) {
            collapseMenu();
        }
    }

    private void onMainSceneKeyPressed(KeyEvent event) {
        if (menuBar.isVisible() && !isInsideMenuBar(event)) {
            collapseMenu();
        }
    }

    private boolean isInsideMenuBar(InputEvent event) {
        if (!(event.getTarget() instanceof Node node)) {
            return false;
        }
        Node current = node;
        while (current != null) {
            if (current == menuBar) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * Главное меню: «Файл» и «Run».
     * По умолчанию скрыто, отображается по клику на бургер-кнопку.
     */
    private MenuBar buildMenuBar(OutputSink outputSink) {
        MenuBar menuBar = new MenuBar();
        menuBar.useSystemMenuBarProperty().set(false);

        Menu fileMenu = new Menu("File");

        MenuItem newItem = new MenuItem("New");
        newItem.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));
        newItem.setOnAction(e -> fileNew());

        MenuItem openItem = new MenuItem("Open...");
        openItem.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
        openItem.setOnAction(e -> fileOpen());

        MenuItem saveItem = new MenuItem("Save");
        saveItem.setAccelerator(KeyCombination.keyCombination("Ctrl+S"));
        saveItem.setOnAction(e -> fileSave());

        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+S"));
        saveAsItem.setOnAction(e -> fileSaveAs());

        MenuItem openProjectItem = new MenuItem("Open Project...");
        openProjectItem.setOnAction(e -> fileOpenProject());

        MenuItem closeTabItem = new MenuItem("Close Tab");
        closeTabItem.setAccelerator(KeyCombination.keyCombination("Ctrl+W"));
        closeTabItem.setOnAction(e -> closeActiveTab());

        MenuItem closeOthersItem = new MenuItem("Close Others");
        closeOthersItem.setOnAction(e -> closeOtherTabs());

        MenuItem closeAllItem = new MenuItem("Close All Tabs");
        closeAllItem.setOnAction(e -> closeAllTabs());

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Q"));
        exitItem.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(
                newItem, openItem, saveItem, saveAsItem,
                new SeparatorMenuItem(),
                openProjectItem,
                new SeparatorMenuItem(),
                closeTabItem, closeOthersItem, closeAllItem,
                new SeparatorMenuItem(),
                exitItem
        );

        Menu runMenu = new Menu("Run");

        MenuItem lintItem = new MenuItem("Verilator Lint");
        lintItem.setOnAction(e -> runTool(findTool(VerilatorLintTool.class), outputSink));

        MenuItem simulationItem = new MenuItem("Simulation");
        simulationItem.setOnAction(e -> runTool(findTool(IcarusSimulationTool.class), outputSink));

        runMenu.getItems().addAll(lintItem, simulationItem);

        menuBar.getMenus().addAll(fileMenu, runMenu);
        return menuBar;
    }

    private void runTool(Tool tool, OutputSink outputSink) {
        collapseMenu();
        TabInfo info = getActiveTabInfo();
        if (info == null || tool == null) {
            return;
        }
        syncEditorFromTab(info);
        String filePath = info.getDocumentManager().getAbsolutePath();
        tool.execute(outputSink, filePath);
    }

    private Tool findTool(Class<? extends Tool> type) {
        for (Tool tool : toolRunner.getTools()) {
            if (type.isInstance(tool)) {
                return tool;
            }
        }
        return null;
    }

    private Node createBurgerIcon() {
        VBox lines = new VBox(4);
        lines.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Region line = new Region();
            line.getStyleClass().add("burger-line");
            lines.getChildren().add(line);
        }
        return lines;
    }

    // ================== Вкладки ==================

    private TabInfo createTab(DocumentManager documentManager, VerilogCodeArea editor, String label) {
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
        VerilogCodeArea editor = createConfiguredEditor();
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

    private VerilogCodeArea createConfiguredEditor() {
        return new VerilogCodeArea();
    }

    private boolean closeTab(TabInfo info) {
        collapseMenu();
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
        String editorContent = info.getEditor().getText();
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
        collapseMenu();
        openTabWithContent(DEFAULT_CONTENT, null);
        updateTitle();
    }

    private void fileOpen() {
        collapseMenu();
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

    private void fileOpenProject() {
        collapseMenu();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Открыть проект");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Проект SystemVerilog (*.proj)", "*.proj"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            currentProject = new ProjectParser().parse(file);
            projectTree.loadProject(currentProject);
        } catch (Exception ex) {
            showError("Не удалось открыть проект: " + ex.getMessage());
        }
    }

    private void openFileInTab(File file) {
        collapseMenu();
        for (TabInfo info : openTabs) {
            File openFile = info.getDocumentManager().getCurrentFile();
            if (openFile != null && openFile.equals(file)) {
                tabPane.getSelectionModel().select(info.getTab());
                return;
            }
        }
        try {
            openTabWithContent(java.nio.file.Files.readString(file.toPath()), file);
            updateTitle();
        } catch (IOException ex) {
            showError("Не удалось открыть файл: " + ex.getMessage());
        }
    }

    private void fileSave() {
        collapseMenu();
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
        collapseMenu();
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
        info.getEditor().replaceText(
                info.getDocumentManager().getContent()
        );
    }

    private void syncTabFromEditor(TabInfo info) {
        info.getDocumentManager().setContent(
                info.getEditor().getText()
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