package com.aoizora.editor.ui;

import com.aoizora.editor.document.DocumentManager;
import com.aoizora.editor.language.VerilogCodeArea;
import com.aoizora.editor.project.Project;
import com.aoizora.editor.project.ProjectParser;
import com.aoizora.editor.tools.*;
import com.github.mouse0w0.darculafx.DarculaFX;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
    private Button maxButton;
    private boolean maximized;
    private double windowX, windowY, windowWidth, windowHeight;

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
        HBox windowControls = buildWindowControls();
        HBox topBar = new HBox(burgerButton, menuBar, spacer, toolbar, windowControls);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(2, 6, 2, 6));
        topBar.getStyleClass().add("top-bar");

        collapseMenu();

        VBox projectPanel = buildProjectPanel();
        SplitPane centerSplit = new SplitPane(projectPanel, tabPane);
        centerSplit.setDividerPositions(0.22);
        centerSplit.getStyleClass().add("main-split");
        SplitPane.setResizableWithParent(projectPanel, false);

        StackPane centerCard = new StackPane(centerSplit);
        centerCard.getStyleClass().add("center-card");
        centerCard.setPadding(new Insets(6));

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(centerCard);
        root.setBottom(outputArea);
        BorderPane.setMargin(topBar, new Insets(6, 6, 0, 6));
        BorderPane.setMargin(centerCard, new Insets(0, 6, 0, 6));
        BorderPane.setMargin(outputArea, new Insets(4, 6, 6, 6));

        BorderPane toolRoom = new BorderPane();
        toolRoom.getStyleClass().add("tool-room");
        toolRoom.setCenter(root);
        toolRoom.setLeft(buildToolStrip(46, 0, "tool-strip-left"));
        toolRoom.setRight(buildToolStrip(46, 0, "tool-strip-right"));
        toolRoom.setBottom(buildToolStrip(0, 26, "tool-strip-bottom"));

        Scene scene = new Scene(toolRoom, 1100, 700);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, this::onMainScenePressed);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::onMainSceneKeyPressed);
        DarculaFX.applyDarculaStyle(scene);
        scene.getStylesheets().addAll(
                getClass().getResource("/styles/tabs.css").toExternalForm(),
                getClass().getResource("/styles/project.css").toExternalForm(),
                getClass().getResource("/styles/editor.css").toExternalForm(),
                getClass().getResource("/styles/menu.css").toExternalForm(),
                getClass().getResource("/styles/layout.css").toExternalForm()
        );
        installWindowResizing(scene, topBar);

        topBar.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !isInsideControl(e.getTarget())) {
                toggleMaximize();
            }
        });

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
        outputArea.getStyleClass().add("output-area");
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

    private Region buildToolStrip(double minWidth, double minHeight, String sideClass) {
        Region strip = new Region();
        strip.getStyleClass().addAll("tool-strip", sideClass);
        if (minWidth > 0) {
            strip.setMinWidth(minWidth);
            strip.setPrefWidth(minWidth);
        }
        if (minHeight > 0) {
            strip.setMinHeight(minHeight);
            strip.setPrefHeight(minHeight);
        }
        return strip;
    }

    private HBox buildToolbar(OutputSink outputSink) {
        HBox toolbar = new HBox(8);
        for (Tool tool : toolRunner.getTools()) {
            Button btn = new Button(tool.getName());
            btn.getStyleClass().add("tool-button");
            btn.setOnAction(e -> runTool(tool, outputSink));
            toolbar.getChildren().add(btn);
        }
        return toolbar;
    }

    // ================== Управление безрамочным окном ==================

    private HBox buildWindowControls() {
        Button minButton = new Button("\u2013");
        minButton.getStyleClass().addAll("window-button", "window-min");
        minButton.setTooltip(new Tooltip("Свернуть"));
        minButton.setOnAction(e -> stage.setIconified(true));

        maxButton = new Button("\u25A1");
        maxButton.getStyleClass().addAll("window-button", "window-max");
        maxButton.setTooltip(new Tooltip("Развернуть"));
        maxButton.setOnAction(e -> toggleMaximize());

        Button closeButton = new Button("\u2715");
        closeButton.getStyleClass().addAll("window-button", "window-close");
        closeButton.setTooltip(new Tooltip("Закрыть"));
        closeButton.setOnAction(e -> stage.close());

        HBox controls = new HBox(2, minButton, maxButton, closeButton);
        controls.setAlignment(Pos.CENTER);
        controls.getStyleClass().add("window-controls");
        return controls;
    }

    private void toggleMaximize() {
        if (maximized) {
            stage.setX(windowX);
            stage.setY(windowY);
            stage.setWidth(windowWidth);
            stage.setHeight(windowHeight);
            maximized = false;
            updateMaxButton();
            return;
        }
        windowX = stage.getX();
        windowY = stage.getY();
        windowWidth = stage.getWidth();
        windowHeight = stage.getHeight();
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        maximized = true;
        updateMaxButton();
    }

    private void updateMaxButton() {
        if (maximized) {
            maxButton.setText("\u2750");
            maxButton.setTooltip(new Tooltip("Свернуть в окно"));
        } else {
            maxButton.setText("\u25A1");
            maxButton.setTooltip(new Tooltip("Развернуть"));
        }
    }

    private boolean isInsideControl(Object target) {
        Node node = target instanceof Node ? (Node) target : null;
        while (node != null) {
            if (node instanceof Control) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private boolean isInsideNode(Object target, Node container) {
        Node node = target instanceof Node ? (Node) target : null;
        while (node != null) {
            if (node == container) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private void installWindowResizing(Scene scene, HBox topBar) {
        double threshold = 4;
        AtomicReference<ResizeDir> dir = new AtomicReference<>(ResizeDir.NONE);
        AtomicReference<ResizeDir> prevDir = new AtomicReference<>(ResizeDir.NONE);
        AtomicReference<double[]> start = new AtomicReference<>();
        AtomicReference<double[]> bounds = new AtomicReference<>();
        AtomicReference<double[]> dragOffset = new AtomicReference<>();

        scene.setOnMouseMoved(e -> {
            ResizeDir d = edgeAt(e.getSceneX(), e.getSceneY(), threshold, maximized);
            if (d == ResizeDir.NONE) {
                if (prevDir.get() != ResizeDir.NONE) {
                    scene.setCursor(Cursor.DEFAULT);
                    prevDir.set(d);
                }
                dir.set(d);
                return;
            }
            dir.set(d);
            if (prevDir.get() != d) {
                scene.setCursor(cursorFor(d));
            }
            prevDir.set(d);
        });

        scene.setOnMousePressed(e -> {
            ResizeDir d = edgeAt(e.getSceneX(), e.getSceneY(), threshold, maximized);
            if (d != ResizeDir.NONE) {
                start.set(new double[]{e.getSceneX(), e.getSceneY()});
                bounds.set(new double[]{stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()});
                dir.set(d);
                dragOffset.set(null);
                e.consume();
                return;
            }
            if (!maximized && isInsideNode(e.getTarget(), topBar)) {
                dragOffset.set(new double[]{
                        e.getScreenX() - stage.getX(),
                        e.getScreenY() - stage.getY()
                });
            } else {
                dragOffset.set(null);
            }
        });

        scene.setOnMouseDragged(e -> {
            ResizeDir d = dir.get();
            if (d != ResizeDir.NONE && start.get() != null && bounds.get() != null) {
                double dx = e.getSceneX() - start.get()[0];
                double dy = e.getSceneY() - start.get()[1];
                double[] b = bounds.get();
                double x = b[0], y = b[1], w = b[2], h = b[3];
                double MIN_W = 640, MIN_H = 420;
                if (d.left) {
                    double nw = w - dx;
                    if (nw >= MIN_W) {
                        x = b[0] + dx;
                        w = nw;
                    }
                } else if (d.right) {
                    double nw = w + dx;
                    if (nw >= MIN_W) {
                        w = nw;
                    }
                }
                if (d.top) {
                    double nh = h - dy;
                    if (nh >= MIN_H) {
                        y = b[1] + dy;
                        h = nh;
                    }
                } else if (d.bottom) {
                    double nh = h + dy;
                    if (nh >= MIN_H) {
                        h = nh;
                    }
                }
                stage.setX(x);
                stage.setY(y);
                stage.setWidth(w);
                stage.setHeight(h);
                return;
            }
            double[] grab = dragOffset.get();
            if (grab != null && !maximized) {
                stage.setX(e.getScreenX() - grab[0]);
                stage.setY(e.getScreenY() - grab[1]);
            }
        });

        scene.setOnMouseReleased(e -> {
            dir.set(ResizeDir.NONE);
            prevDir.set(ResizeDir.NONE);
            start.set(null);
            bounds.set(null);
            dragOffset.set(null);
            scene.setCursor(Cursor.DEFAULT);
        });
    }

    private ResizeDir edgeAt(double x, double y, double threshold, boolean disabled) {
        if (disabled) {
            return ResizeDir.NONE;
        }
        double w = stage.getWidth();
        double h = stage.getHeight();
        boolean left = x <= threshold;
        boolean right = x >= w - threshold;
        boolean top = y <= threshold;
        boolean bottom = y >= h - threshold;
        if (!left && !right && !top && !bottom) {
            return ResizeDir.NONE;
        }
        return new ResizeDir(left, right, top, bottom);
    }

    private Cursor cursorFor(ResizeDir d) {
        if (d.left && d.top || d.right && d.bottom) {
            return Cursor.NW_RESIZE;
        }
        if (d.right && d.top || d.left && d.bottom) {
            return Cursor.NE_RESIZE;
        }
        if (d.left || d.right) {
            return Cursor.H_RESIZE;
        }
        if (d.top || d.bottom) {
            return Cursor.V_RESIZE;
        }
        return Cursor.DEFAULT;
    }

    private static final class ResizeDir {
        static final ResizeDir NONE = new ResizeDir(false, false, false, false);
        final boolean left;
        final boolean right;
        final boolean top;
        final boolean bottom;

        ResizeDir(boolean left, boolean right, boolean top, boolean bottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
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