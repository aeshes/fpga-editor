package com.aoizora.editor.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class IcarusSimulationTool implements Tool {

    @Override
    public String getName() {
        return "Run Simulation";
    }

    @Override
    public void execute(OutputSink output, String currentFilePath) {
        output.clear();
        if (currentFilePath == null) {
            output.appendLine("Ошибка: сначала откройте или сохраните файл");
            return;
        }

        String dir = new File(currentFilePath).getParent();
        Set<File> sources = new LinkedHashSet<>();

        File tb = new File(dir, "tb.sv");
        if (tb.isFile()) {
            sources.add(tb);
        }

        File current = new File(currentFilePath);
        if (current.equals(tb)) {
            File[] others = new File(dir).listFiles((d, name) ->
                    name.endsWith(".sv") || name.endsWith(".svh") || name.endsWith(".v"));
            if (others != null) {
                for (File f : others) sources.add(f);
            }
        } else {
            sources.add(current);
        }

        File buildDir = new File(dir, "build");
        buildDir.mkdirs();
        String outVvp = new File(buildDir, "out.vvp").getPath();

        List<String> command = new ArrayList<>();
        command.add("iverilog");
        command.add("-g2012");
        command.add("-o");
        command.add(outVvp);
        for (File f : sources) command.add(f.getPath());
        output.appendLine(">>> " + String.join(" ", command));

        new Thread(() -> {
            try {
                int exit = runCaptured(output, command.toArray(new String[0]));
                output.appendLine("Код возврата компиляции: " + exit);
                if (exit == 0) {
                    output.appendLine(">>> vvp " + outVvp);
                    runCaptured(output, "vvp", outVvp);
                }
            } catch (Exception ex) {
                output.appendLine("Ошибка выполнения Icarus: " + ex.getMessage());
            }
        }).start();
    }

    private int runCaptured(OutputSink output, String... command)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        Process p = pb.start();
        Thread out = streamReader(p.getInputStream(), line -> output.appendLine(line));
        Thread err = streamReader(p.getErrorStream(), line -> output.appendLine(line));
        out.start();
        err.start();
        int exit = p.waitFor();
        out.join();
        err.join();
        return exit;
    }

    private Thread streamReader(InputStream in, java.util.function.Consumer<String> consumer) {
        return new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
                r.lines().forEach(consumer);
            } catch (IOException ignored) {}
        });
    }
}