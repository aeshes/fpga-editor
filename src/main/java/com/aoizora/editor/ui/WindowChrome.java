package com.aoizora.editor.ui;

import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;

public class WindowChrome {

    private static final double RESIZE_THRESHOLD = 4;
    private static final double MIN_WIDTH = 640;
    private static final double MIN_HEIGHT = 420;

    private final Stage stage;
    private Button maxButton;
    private boolean maximized;
    private double savedX, savedY, savedW, savedH;

    public WindowChrome(Stage stage) {
        this.stage = stage;
    }

    public boolean isMaximized() {
        return maximized;
    }

    public HBox buildControls() {
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

    public void install(Scene scene, HBox topBar) {
        AtomicReference<EnumSet<Edge>> dir = new AtomicReference<>(EnumSet.noneOf(Edge.class));
        AtomicReference<EnumSet<Edge>> prev = new AtomicReference<>(EnumSet.noneOf(Edge.class));
        AtomicReference<double[]> start = new AtomicReference<>();
        AtomicReference<double[]> bounds = new AtomicReference<>();
        AtomicReference<double[]> dragOffset = new AtomicReference<>();

        scene.setOnMouseMoved(e -> {
            EnumSet<Edge> d = edgeAt(e.getSceneX(), e.getSceneY());
            if (!d.equals(prev.get())) {
                scene.setCursor(cursorFor(d));
                prev.set(d);
            }
            dir.set(d);
        });

        scene.setOnMousePressed(e -> {
            EnumSet<Edge> d = edgeAt(e.getSceneX(), e.getSceneY());
            if (!d.isEmpty()) {
                start.set(new double[]{e.getSceneX(), e.getSceneY()});
                bounds.set(new double[]{stage.getX(), stage.getY(),
                        stage.getWidth(), stage.getHeight()});
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
            EnumSet<Edge> d = dir.get();
            if (!d.isEmpty() && start.get() != null && bounds.get() != null) {
                double dx = e.getSceneX() - start.get()[0];
                double dy = e.getSceneY() - start.get()[1];
                double[] b = bounds.get();
                double x = b[0], y = b[1], w = b[2], h = b[3];
                if (d.contains(Edge.LEFT)) {
                    double nw = w - dx;
                    if (nw >= MIN_WIDTH) { x = b[0] + dx; w = nw; }
                } else if (d.contains(Edge.RIGHT)) {
                    double nw = w + dx;
                    if (nw >= MIN_WIDTH) w = nw;
                }
                if (d.contains(Edge.TOP)) {
                    double nh = h - dy;
                    if (nh >= MIN_HEIGHT) { y = b[1] + dy; h = nh; }
                } else if (d.contains(Edge.BOTTOM)) {
                    double nh = h + dy;
                    if (nh >= MIN_HEIGHT) h = nh;
                }
                stage.setX(x); stage.setY(y);
                stage.setWidth(w); stage.setHeight(h);
                return;
            }
            double[] grab = dragOffset.get();
            if (grab != null && !maximized) {
                stage.setX(e.getScreenX() - grab[0]);
                stage.setY(e.getScreenY() - grab[1]);
            }
        });

        scene.setOnMouseReleased(e -> {
            dir.set(EnumSet.noneOf(Edge.class));
            prev.set(EnumSet.noneOf(Edge.class));
            start.set(null);
            bounds.set(null);
            dragOffset.set(null);
            scene.setCursor(Cursor.DEFAULT);
        });

        topBar.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !isInsideControl(e.getTarget())) {
                toggleMaximize();
            }
        });
    }

    private void toggleMaximize() {
        if (maximized) {
            stage.setX(savedX); stage.setY(savedY);
            stage.setWidth(savedW); stage.setHeight(savedH);
            maximized = false;
        } else {
            savedX = stage.getX(); savedY = stage.getY();
            savedW = stage.getWidth(); savedH = stage.getHeight();
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            stage.setX(bounds.getMinX()); stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth()); stage.setHeight(bounds.getHeight());
            maximized = true;
        }
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

    // === Edge detection (EnumSet replaces buggy reference comparison) ===

    private enum Edge { LEFT, RIGHT, TOP, BOTTOM }

    private EnumSet<Edge> edgeAt(double x, double y) {
        if (maximized) return EnumSet.noneOf(Edge.class);
        EnumSet<Edge> edges = EnumSet.noneOf(Edge.class);
        if (x <= RESIZE_THRESHOLD) edges.add(Edge.LEFT);
        if (x >= stage.getWidth() - RESIZE_THRESHOLD) edges.add(Edge.RIGHT);
        if (y <= RESIZE_THRESHOLD) edges.add(Edge.TOP);
        if (y >= stage.getHeight() - RESIZE_THRESHOLD) edges.add(Edge.BOTTOM);
        return edges;
    }

    private Cursor cursorFor(EnumSet<Edge> edges) {
        boolean h = edges.contains(Edge.LEFT) || edges.contains(Edge.RIGHT);
        boolean v = edges.contains(Edge.TOP) || edges.contains(Edge.BOTTOM);
        if (h && v) {
            return (edges.contains(Edge.LEFT) ^ edges.contains(Edge.TOP))
                    ? Cursor.NE_RESIZE : Cursor.NW_RESIZE;
        }
        if (h) return Cursor.H_RESIZE;
        if (v) return Cursor.V_RESIZE;
        return Cursor.DEFAULT;
    }

    private boolean isInsideControl(Object target) {
        return walkParents(target, node -> node instanceof Control);
    }

    private boolean isInsideNode(Object target, Node container) {
        return walkParents(target, node -> node == container);
    }

    private boolean walkParents(Object target, java.util.function.Predicate<Node> test) {
        Node node = target instanceof Node n ? n : null;
        while (node != null) {
            if (test.test(node)) return true;
            node = node.getParent();
        }
        return false;
    }
}
