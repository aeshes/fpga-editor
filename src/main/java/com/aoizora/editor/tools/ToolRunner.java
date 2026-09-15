package com.aoizora.editor.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ToolRunner {

    private final List<Tool> tools = new ArrayList<>();

    public void register(Tool tool) {
        tools.add(tool);
    }

    public List<Tool> getTools() {
        return Collections.unmodifiableList(tools);
    }

    void execute(OutputSink output, String... command) {
        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(false);
                Process process = pb.start();

                Thread outThread = streamReader(process.getInputStream(), line -> output.appendLine("[OUT] " + line));
                Thread errThread = streamReader(process.getErrorStream(), line -> output.appendLine("[ERR] " + line));

                outThread.start();
                errThread.start();

                int exit = process.waitFor();
                outThread.join();
                errThread.join();

                output.appendLine("--- Процесс завершён, код возврата: " + exit + " ---");
            } catch (Exception ex) {
                output.appendLine("Ошибка выполнения: " + ex.getMessage());
            }
        }).start();
    }

    private Thread streamReader(InputStream in, Consumer<String> consumer) {
        return new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
                r.lines().forEach(consumer);
            } catch (IOException ignored) {}
        });
    }
}
