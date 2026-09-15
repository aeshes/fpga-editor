package com.aoizora.editor.tools;

import java.io.File;

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
        String tbFile = dir + File.separator + "tb.sv";
        String designFile = currentFilePath;
        output.appendLine(">>> iverilog -g2012 -o " + dir + File.separator + "build" + File.separator + "out.vvp " + tbFile + " " + designFile);
        new Thread(() -> {
            try {
                String buildDir = dir + File.separator + "build";
                new File(buildDir).mkdirs();
                String outVvp = buildDir + File.separator + "out.vvp";
                Process p = new ProcessBuilder(
                        "iverilog", "-g2012", "-o", outVvp,
                        tbFile, designFile).start();
                int exit = p.waitFor();
                output.appendLine("Код возврата компиляции: " + exit);
                if (exit == 0) {
                    output.appendLine(">>> vvp " + outVvp);
                    ToolRunner runner = new ToolRunner();
                    runner.execute(output, "vvp", outVvp);
                }
            } catch (Exception ex) {
                output.appendLine("Ошибка выполнения Icarus: " + ex.getMessage());
            }
        }).start();
    }
}
