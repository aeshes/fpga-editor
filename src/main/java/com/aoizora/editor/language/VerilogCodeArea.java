package com.aoizora.editor.language;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.reactfx.Subscription;

/**
 * Обёртка над {@link CodeArea} (RichTextFX): редактор без WebView.
 * <p>
 * Предоставляет виртуализированную прокрутку, номера строк,
 * автоподсветку Verilog и вставку табуляции пробелами.
 */
public final class VerilogCodeArea {

    private final CodeArea area;
    private final VirtualizedScrollPane<CodeArea> scrollPane;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "verilog-highlighter");
        t.setDaemon(true);
        return t;
    });

    @SuppressWarnings("unused")
    private final Subscription highlightSubscription;

    public VerilogCodeArea() {
        this.area = new CodeArea();
        this.area.setParagraphGraphicFactory(LineNumberFactory.get(area));
        this.area.getStyleClass().add("verilog-code-area");
        installTabIndentation();
        this.highlightSubscription = installHighlighting();
        this.scrollPane = new VirtualizedScrollPane<>(area);
    }

    /**
     * Узел для встраивания в вкладку (скролл-панель с редактором).
     */
    public Node getNode() {
        return scrollPane;
    }

    public String getText() {
        return area.getText();
    }

    public void replaceText(String text) {
        area.replaceText(text);
        area.moveTo(0);
        requestHighlight();
    }

    public void setEditable(boolean editable) {
        area.setEditable(editable);
    }

    public void requestFocus() {
        area.requestFocus();
    }

    private void installTabIndentation() {
        area.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                event.consume();
                area.replaceSelection("    ");
            }
        });
    }

    private Subscription installHighlighting() {
        return area.multiPlainChanges()
                .successionEnds(Duration.ofMillis(300))
                .retainLatestUntilLater(executor)
                .supplyTask(() -> {
                    String text = area.getText();
                    Task<StyleSpans<Collection<String>>> task = new Task<>() {
                        @Override
                        protected StyleSpans<Collection<String>> call() {
                            return VerilogHighlighter.computeHighlighting(text);
                        }
                    };
                    executor.execute(task);
                    return task;
                })
                .awaitLatest(area.multiPlainChanges())
                .filterMap(t -> {
                    if (t.isSuccess()) {
                        return Optional.of(t.get());
                    }
                    t.getFailure().printStackTrace();
                    return Optional.empty();
                })
                .subscribe(spans -> area.setStyleSpans(0, spans));
    }

    private void requestHighlight() {
        String text = area.getText();
        executor.execute(() -> {
            StyleSpans<Collection<String>> spans = VerilogHighlighter.computeHighlighting(text);
            Platform.runLater(() -> area.setStyleSpans(0, spans));
        });
    }
}