package com.aoizora.editor.ui;

import com.aoizora.editor.tools.*;
import com.github.mouse0w0.darculafx.DarculaFX;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;

public class MainWindow {

    private static final String APP_TITLE = "SystemVerilog FPGA Editor";

    private final Stage stage;
    private final ToolRunner toolRunner = new ToolRunner();
    private final TabWorkspace tabWorkspace;
    private final ProjectTreeView projectTree;
    private final FileController fileController;
    private final WindowChrome windowChrome;
    private TextArea outputArea;

    public MainWindow(Stage stage) {
        this.stage = stage;
        this.tabWorkspace = new TabWorkspace(stage);
        this.projectTree = new ProjectTreeView();
        this.windowChrome = new WindowChrome(stage);
        this.fileController = new FileController(stage, tabWorkspace, projectTree, toolRunner);
        setupTools();
        wireProjectTree();
        tabWorkspace.getTabPane().getSelectionModel().selectedItemProperty()
                .addListener((obs, old, tab) -> updateTitle());
    }

    public void show() {
        outputArea = createOutputArea();
        OutputSink outputSink = new TextAreaOutputSink(outputArea);

        HBox toolbar = buildToolbar(outputSink);
        HBox windowControls = windowChrome.buildControls();

        TopBarController topCtrl = new TopBarController(
                stage, fileController, tabWorkspace, outputSink, () -> stage.close());
        topCtrl.setOnActionPerformed(this::updateTitle);
        HBox topBar = topCtrl.build(toolbar, windowControls);

        VBox projectPanel = buildProjectPanel();
        SplitPane centerSplit = new SplitPane(projectPanel, tabWorkspace.getTabPane());
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
        BorderPane.setMargin(topBar, Insets.EMPTY);
        BorderPane.setMargin(centerCard, new Insets(0, 6, 0, 6));
        BorderPane.setMargin(outputArea, new Insets(4, 6, 6, 6));

        BorderPane toolRoom = new BorderPane();
        toolRoom.getStyleClass().add("tool-room");
        toolRoom.setCenter(root);
        toolRoom.setLeft(buildLeftStripWithIcon());
        toolRoom.setRight(buildToolStrip(46, 0, "tool-strip-right"));
        toolRoom.setBottom(buildToolStrip(0, 26, "tool-strip-bottom"));

        Scene scene = new Scene(toolRoom, 1650, 1050);
        DarculaFX.applyDarculaStyle(scene);
        scene.getStylesheets().addAll(
                getClass().getResource("/styles/tabs.css").toExternalForm(),
                getClass().getResource("/styles/project.css").toExternalForm(),
                getClass().getResource("/styles/editor.css").toExternalForm(),
                getClass().getResource("/styles/menu.css").toExternalForm(),
                getClass().getResource("/styles/layout.css").toExternalForm()
        );

        windowChrome.install(scene, topBar);
        topCtrl.installCollapseHooks(scene);

        fileController.fileNew();
        updateTitle();
        stage.setScene(scene);
        stage.show();
    }

    // === Tools ===

    private void setupTools() {
        toolRunner.register(new VerilatorLintTool());
        toolRunner.register(new IcarusSimulationTool());
        toolRunner.register(new GtkwaveTool());
    }

    // === Project ===

    private void wireProjectTree() {
        projectTree.setOnOpenFile(file -> {
            try {
                tabWorkspace.openFileInTab(file);
            } catch (IOException ex) {
                showError("Не удалось открыть файл: " + ex.getMessage());
            }
            updateTitle();
        });
    }

    // === Layout helpers ===

    private TextArea createOutputArea() {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setPromptText("Вывод Verilator / Icarus будет отображаться здесь...");
        area.getStyleClass().add("output-area");
        return area;
    }

    private HBox buildToolbar(OutputSink outputSink) {
        HBox toolbar = new HBox(8);
        for (Tool tool : toolRunner.getTools()) {
            Button btn = new Button(tool.getName());
            btn.getStyleClass().add("tool-button");
            btn.setOnAction(e -> fileController.runTool(tool, outputSink));
            toolbar.getChildren().add(btn);
        }
        return toolbar;
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
        if (minWidth > 0) { strip.setMinWidth(minWidth); strip.setPrefWidth(minWidth); }
        if (minHeight > 0) { strip.setMinHeight(minHeight); strip.setPrefHeight(minHeight); }
        return strip;
    }

    private Region buildLeftStripWithIcon() {
        VBox strip = new VBox(14);
        strip.getStyleClass().addAll("tool-strip", "tool-strip-left");
        strip.setMinWidth(46);
        strip.setPrefWidth(46);
        strip.setAlignment(Pos.TOP_CENTER);
        strip.setPadding(new Insets(12, 0, 0, 0));
        ImageView appIcon = new ImageView(new Image(
                getClass().getResourceAsStream("/images/app-icon-24.png")));
        appIcon.setFitWidth(24);
        appIcon.setFitHeight(24);
        strip.getChildren().add(appIcon);
        return strip;
    }

    // === Title / errors ===

    private void updateTitle() {
        TabInfo info = tabWorkspace.getActiveTabInfo();
        String name = info != null ? info.getTitle() : "Нет открытых файлов";
        stage.setTitle(name + " — " + APP_TITLE);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Ошибка");
        alert.showAndWait();
    }
}
