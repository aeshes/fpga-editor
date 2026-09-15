package com.aoizora.editor.ui;

import com.aoizora.editor.tools.OutputSink;
import com.aoizora.editor.tools.VerilatorLintTool;
import com.aoizora.editor.tools.IcarusSimulationTool;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TopBarController {

    private final FileController fileCtrl;
    private final TabWorkspace tabs;
    private final Button burgerButton;
    private final MenuBar menuBar;
    private Runnable onActionPerformed = () -> {};

    public TopBarController(Stage stage, FileController fileCtrl, TabWorkspace tabs,
                            OutputSink output, Runnable onExit) {
        this.fileCtrl = fileCtrl;
        this.tabs = tabs;
        this.menuBar = buildMenuBar(output, onExit);
        this.burgerButton = buildBurgerButton();
    }

    public void setOnActionPerformed(Runnable action) {
        this.onActionPerformed = action;
    }

    public void installCollapseHooks(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (menuBar.isVisible() && !isInsideMenuBar(e.getTarget())) collapseMenu();
        });
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (menuBar.isVisible() && !isInsideMenuBar(e.getTarget())) collapseMenu();
        });
    }

    public HBox build(HBox toolbar, HBox windowControls) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(burgerButton, menuBar, spacer, toolbar, windowControls);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(2, 6, 2, 6));
        topBar.getStyleClass().add("top-bar");
        collapseMenu();
        return topBar;
    }

    public void collapseMenu() {
        burgerButton.setVisible(true);
        burgerButton.setManaged(true);
        menuBar.setVisible(false);
        menuBar.setManaged(false);
    }

    private void expandMenu() {
        burgerButton.setVisible(false);
        burgerButton.setManaged(false);
        menuBar.setVisible(true);
        menuBar.setManaged(true);
    }

    private Button buildBurgerButton() {
        Button burger = new Button();
        burger.getStyleClass().add("burger-menu");
        burger.setGraphic(createBurgerIcon());
        burger.setFocusTraversable(false);
        burger.setTooltip(new Tooltip("Главное меню"));
        burger.setOnAction(e -> {
            if (menuBar.isVisible()) collapseMenu(); else expandMenu();
        });
        return burger;
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

    private MenuBar buildMenuBar(OutputSink output, Runnable onExit) {
        MenuBar bar = new MenuBar();
        bar.useSystemMenuBarProperty().set(false);

        Runnable after = () -> { collapseMenu(); onActionPerformed.run(); };

        Menu fileMenu = new Menu("File");

        MenuItem newItem = new MenuItem("New");
        newItem.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));
        newItem.setOnAction(e -> { fileCtrl.fileNew(); after.run(); });

        MenuItem openItem = new MenuItem("Open...");
        openItem.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
        openItem.setOnAction(e -> { fileCtrl.fileOpen(); after.run(); });

        MenuItem saveItem = new MenuItem("Save");
        saveItem.setAccelerator(KeyCombination.keyCombination("Ctrl+S"));
        saveItem.setOnAction(e -> { fileCtrl.fileSave(); after.run(); });

        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+S"));
        saveAsItem.setOnAction(e -> { fileCtrl.fileSaveAs(); after.run(); });

        MenuItem openProjectItem = new MenuItem("Open Project...");
        openProjectItem.setOnAction(e -> { fileCtrl.fileOpenProject(); after.run(); });

        MenuItem closeTabItem = new MenuItem("Close Tab");
        closeTabItem.setAccelerator(KeyCombination.keyCombination("Ctrl+W"));
        closeTabItem.setOnAction(e -> { tabs.closeActiveTab(); after.run(); });

        MenuItem closeOthersItem = new MenuItem("Close Others");
        closeOthersItem.setOnAction(e -> { tabs.closeOtherTabs(); after.run(); });

        MenuItem closeAllItem = new MenuItem("Close All Tabs");
        closeAllItem.setOnAction(e -> { tabs.closeAllTabs(); after.run(); });

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setAccelerator(KeyCombination.keyCombination("Ctrl+Q"));
        exitItem.setOnAction(e -> onExit.run());

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
        lintItem.setOnAction(e -> {
            fileCtrl.runTool(fileCtrl.findTool(VerilatorLintTool.class), output);
            after.run();
        });

        MenuItem simulationItem = new MenuItem("Simulation");
        simulationItem.setOnAction(e -> {
            fileCtrl.runTool(fileCtrl.findTool(IcarusSimulationTool.class), output);
            after.run();
        });

        runMenu.getItems().addAll(lintItem, simulationItem);
        bar.getMenus().addAll(fileMenu, runMenu);
        return bar;
    }

    private boolean isInsideMenuBar(Object target) {
        Node node = target instanceof Node n ? n : null;
        while (node != null) {
            if (node == menuBar) return true;
            node = node.getParent();
        }
        return false;
    }
}
