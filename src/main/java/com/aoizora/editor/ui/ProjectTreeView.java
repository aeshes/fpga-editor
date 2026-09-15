package com.aoizora.editor.ui;

import com.aoizora.editor.project.Project;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

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
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            TreeItem<ProjectNode> item = getSelectionModel().getSelectedItem();
            if (item == null || item.getValue() == null) {
                return;
            }
            if (!item.getValue().isFile()) {
                item.setExpanded(!item.isExpanded());
            } else if (event.getClickCount() == 2) {
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

        private static final String CHEVRON_PATH =
                "M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708"
                        + "l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708";

        private static final String FILE_ICON_BASE =
                "M5.5.5v2.5h1V.5zM9.5.5v2.5h1V.5zM5.5 13v2.5h1V13zM9.5 13v2.5h1V13z"
                        + "M.5 5.5h2.5v1H.5zM.5 9.5h2.5v1H.5zM13 5.5h2.5v1H13zM13 9.5h2.5v1H13z"
                        + "M4 2.5h8a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z";
        private static final String FILE_ICON_DIE = "M6 6h4v4H6z";
        private static final String FOLDER_ICON = "M1 3.5h4.2l1.3 1.4H15v7.6H1z";

        private final SVGPath chevron = new SVGPath();
        private final StackPane chevronBox = new StackPane(chevron);
        private final SVGPath rootChevron = new SVGPath();
        private final StackPane rootChevronBox = new StackPane(rootChevron);
        private final SVGPath fileIconBase = new SVGPath();
        private final SVGPath fileIconDie = new SVGPath();
        private final StackPane fileIcon = new StackPane(fileIconBase, fileIconDie);
        private final SVGPath folderIconGray = new SVGPath();
        private final StackPane folderGrayBox = new StackPane(folderIconGray);
        private final SVGPath folderIconBlue = new SVGPath();
        private final StackPane folderBlueBox = new StackPane(folderIconBlue);
        private final HBox rootGraphic = new HBox(rootChevronBox, folderGrayBox);
        private final HBox folderGraphic = new HBox(chevronBox, folderBlueBox);
        private ChangeListener<Boolean> expansionListener;

        {
            chevron.getStyleClass().addAll("tree-chevron");
            chevron.setContent(CHEVRON_PATH);
            chevron.setFill(null);
            chevronBox.getStyleClass().add("tree-chevron-box");
            rootChevron.getStyleClass().addAll("tree-chevron");
            rootChevron.setContent(CHEVRON_PATH);
            rootChevron.setFill(null);
            rootChevronBox.getStyleClass().add("tree-chevron-box");

            fileIcon.getStyleClass().add("file-icon");
            fileIconBase.getStyleClass().add("file-icon-base");
            fileIconBase.setContent(FILE_ICON_BASE);
            fileIconBase.setFill(null);
            fileIconDie.getStyleClass().add("file-icon-die");
            fileIconDie.setContent(FILE_ICON_DIE);
            fileIconDie.setFill(null);

            folderIconGray.getStyleClass().addAll("folder-icon", "folder-icon-gray");
            folderIconGray.setContent(FOLDER_ICON);
            folderIconGray.setFill(null);
            folderGrayBox.getStyleClass().add("file-icon");
            folderIconBlue.getStyleClass().addAll("folder-icon", "folder-icon-blue");
            folderIconBlue.setContent(FOLDER_ICON);
            folderIconBlue.setFill(null);
            folderBlueBox.getStyleClass().add("file-icon");

            rootGraphic.getStyleClass().add("tree-cell-graphic");
            folderGraphic.getStyleClass().add("tree-cell-graphic");
        }

        @Override
        protected void updateItem(ProjectNode item, boolean empty) {
            super.updateItem(item, empty);

            removeExpansionListener();
            getStyleClass().removeAll(
                    "project-tree-file", "project-tree-folder", "project-tree-root");

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            setText(item.getName());

            boolean isRoot = getTreeView().getRoot() != null
                    && getTreeView().getRoot().getValue() == item;

            if (!item.isFile()) {
                TreeItem<ProjectNode> treeItem = getTreeItem();
                if (isRoot) {
                    rootChevronBox.setRotate(treeItem.isExpanded() ? 90 : 0);
                } else {
                    chevronBox.setRotate(treeItem.isExpanded() ? 90 : 0);
                }
                expansionListener = (obs, was, isNow) -> {
                    if (isRoot) {
                        rootChevronBox.setRotate(isNow ? 90 : 0);
                    } else {
                        chevronBox.setRotate(isNow ? 90 : 0);
                    }
                };
                treeItem.expandedProperty().addListener(expansionListener);
                setGraphic(isRoot ? rootGraphic : folderGraphic);
                getStyleClass().add(isRoot
                        ? "project-tree-root" : "project-tree-folder");
            } else {
                setGraphic(fileIcon);
                getStyleClass().add("project-tree-file");
            }
        }

        private void removeExpansionListener() {
            if (expansionListener != null) {
                TreeItem<ProjectNode> treeItem = getTreeItem();
                if (treeItem != null) {
                    treeItem.expandedProperty().removeListener(expansionListener);
                }
                expansionListener = null;
            }
        }
    }
}