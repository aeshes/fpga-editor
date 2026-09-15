package com.aoizora.editor.ui;

import com.aoizora.editor.document.DocumentManager;
import com.aoizora.editor.project.Project;
import com.aoizora.editor.project.ProjectParser;
import com.aoizora.editor.tools.OutputSink;
import com.aoizora.editor.tools.Tool;
import com.aoizora.editor.tools.ToolRunner;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

public class FileController {

    private static final String DEFAULT_CONTENT =
            "module alu(input logic [7:0] a, b, output logic [7:0] y);\n" +
            "    assign y = a + b;\n" +
            "endmodule\n";

    private final Stage stage;
    private final TabWorkspace tabs;
    private final ProjectTreeView projectTree;
    private final ToolRunner toolRunner;
    private Project currentProject;

    public FileController(Stage stage, TabWorkspace tabs,
                          ProjectTreeView projectTree, ToolRunner toolRunner) {
        this.stage = stage;
        this.tabs = tabs;
        this.projectTree = projectTree;
        this.toolRunner = toolRunner;
    }

    public void fileNew() {
        tabs.openTabWithContent(DEFAULT_CONTENT, null);
    }

    public void fileOpen() {
        FileChooser chooser = fileChooser("Открыть файл",
                new FileChooser.ExtensionFilter("SystemVerilog", "*.sv", "*.svh", "*.v"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            try {
                tabs.openTabWithContent(Files.readString(file.toPath()), file);
            } catch (IOException ex) {
                showError("Не удалось открыть файл: " + ex.getMessage());
            }
        }
    }

    public void fileSave() {
        TabInfo info = tabs.getActiveTabInfo();
        if (info == null) return;
        tabs.syncTabFromEditor(info);
        DocumentManager dm = info.getDocumentManager();
        if (dm.hasFile()) {
            try {
                dm.save(dm.getCurrentFile());
            } catch (IOException ex) {
                showError("Не удалось сохранить файл: " + ex.getMessage());
            }
        } else {
            fileSaveAs(info);
        }
    }

    public void fileSaveAs() {
        fileSaveAs(tabs.getActiveTabInfo());
    }

    public void fileSaveAll() {
        for (TabInfo info : new ArrayList<>(tabs.getOpenTabs())) {
            tabs.syncTabFromEditor(info);
            DocumentManager dm = info.getDocumentManager();
            if (dm.hasFile()) {
                try {
                    dm.save(dm.getCurrentFile());
                } catch (IOException ex) {
                    showError("Не удалось сохранить файл: " + ex.getMessage());
                }
            } else {
                fileSaveAs(info);
            }
        }
    }

    private void fileSaveAs(TabInfo info) {
        if (info == null) return;
        FileChooser chooser = fileChooser("Сохранить файл",
                new FileChooser.ExtensionFilter("SystemVerilog", "*.sv"),
                new FileChooser.ExtensionFilter("SystemVerilog Header", "*.svh"),
                new FileChooser.ExtensionFilter("Verilog", "*.v"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*"));
        DocumentManager dm = info.getDocumentManager();
        if (dm.hasFile()) {
            chooser.setInitialDirectory(dm.getCurrentFile().getParentFile());
            chooser.setInitialFileName(dm.getCurrentFile().getName());
        }
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            tabs.syncEditorFromTab(info);
            try {
                dm.save(file);
                info.setTitle(file.getName());
                info.setPathTooltip(file.getAbsolutePath());
            } catch (IOException ex) {
                showError("Не удалось сохранить файл: " + ex.getMessage());
            }
        }
    }

    public void fileOpenProject() {
        FileChooser chooser = fileChooser("Открыть проект",
                new FileChooser.ExtensionFilter("Проект SystemVerilog (*.proj)", "*.proj"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        try {
            currentProject = new ProjectParser().parse(file);
            projectTree.loadProject(currentProject);
        } catch (Exception ex) {
            showError("Не удалось открыть проект: " + ex.getMessage());
        }
    }

    public void runTool(Tool tool, OutputSink outputSink) {
        TabInfo info = tabs.getActiveTabInfo();
        if (info == null || tool == null) return;
        tabs.syncTabFromEditor(info);
        tool.execute(outputSink, info.getDocumentManager().getAbsolutePath());
    }

    public Tool findTool(Class<? extends Tool> type) {
        for (Tool tool : toolRunner.getTools()) {
            if (type.isInstance(tool)) return tool;
        }
        return null;
    }

    private FileChooser fileChooser(String title, FileChooser.ExtensionFilter... filters) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().addAll(filters);
        return chooser;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText("Ошибка");
        alert.showAndWait();
    }
}
