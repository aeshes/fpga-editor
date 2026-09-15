package com.aoizora.editor.ui;

import com.aoizora.editor.project.Project;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;

import java.io.File;
import java.util.Comparator;
import java.util.function.Consumer;

/**
 * Дерево файлов проекта (.proj).
 * Формат проекта задаётся {@link Project}; виджет лишь отображает его структуру.
 */
public class ProjectTreeView extends TreeView<ProjectTreeView.ProjectNode> {

    private Consumer<File> onOpenFile;

    public static class ProjectNode {

        private final String name;
        private final File file;

        public ProjectNode(String name, File file) {
            this.name = name;
            this.file = file;
        }

        public String getName() {
            return name;
        }

        public File getFile() {
            return file;
        }

        public boolean isFile() {
            return file != null;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public ProjectTreeView() {
        setShowRoot(true);
        getStyleClass().add("project-tree");
        setCellFactory(tree -> new ProjectCell());

        setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                openSelectedFile();
            }
        });
    }

    public void setOnOpenFile(Consumer<File> onOpenFile) {
        this.onOpenFile = onOpenFile;
    }

    public void loadProject(Project project) {
        TreeItem<ProjectNode> root = new TreeItem<>(
                new ProjectNode(project.getName(), null)
        );
        root.setExpanded(true);

        for (String relativePath : project.getFiles()) {
            addFile(root, project, relativePath);
        }
        sortRecursively(root);
        setRoot(root);
    }

    public void clearProject() {
        setRoot(null);
    }

    private void addFile(TreeItem<ProjectNode> root, Project project, String relativePath) {
        String[] parts = relativePath.split("/");
        TreeItem<ProjectNode> current = root;
        StringBuilder accumulated = new StringBuilder();

        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            if (accumulated.length() > 0) {
                accumulated.append('/');
            }
            accumulated.append(parts[i]);
            current = getOrCreateFolder(current, parts[i]);
        }

        String fileName = parts[parts.length - 1];
        if (fileName.isEmpty()) {
            return;
        }
        File resolved = project.resolve(relativePath);
        current.getChildren().add(
                new TreeItem<>(new ProjectNode(fileName, resolved))
        );
    }

    private TreeItem<ProjectNode> getOrCreateFolder(TreeItem<ProjectNode> parent, String folderName) {
        for (TreeItem<ProjectNode> child : parent.getChildren()) {
            ProjectNode node = child.getValue();
            if (node != null && !node.isFile() && node.getName().equals(folderName)) {
                return child;
            }
        }
        TreeItem<ProjectNode> folder = new TreeItem<>(new ProjectNode(folderName, null));
        folder.setExpanded(true);
        parent.getChildren().add(folder);
        return folder;
    }

    private void sortRecursively(TreeItem<ProjectNode> item) {
        item.getChildren().sort(Comparator
                .comparing((TreeItem<ProjectNode> child) -> child.getValue().isFile())
                .thenComparing(child -> child.getValue().getName(), String.CASE_INSENSITIVE_ORDER));
        for (TreeItem<ProjectNode> child : item.getChildren()) {
            sortRecursively(child);
        }
    }

    private void openSelectedFile() {
        TreeItem<ProjectNode> item = getSelectionModel().getSelectedItem();
        if (item == null || item.getValue() == null || !item.getValue().isFile()) {
            return;
        }
        if (onOpenFile != null) {
            onOpenFile.accept(item.getValue().getFile());
        }
    }

    private static class ProjectCell extends TreeCell<ProjectNode> {

        @Override
        protected void updateItem(ProjectNode item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().removeAll("project-tree-file", "project-tree-folder", "project-tree-root");
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(item.getName());
            setGraphic(null);
            if (item.isFile()) {
                getStyleClass().add("project-tree-file");
            } else if (getTreeView().getRoot() != null && getTreeView().getRoot().getValue() == item) {
                getStyleClass().add("project-tree-root");
            } else {
                getStyleClass().add("project-tree-folder");
            }
        }
    }
}