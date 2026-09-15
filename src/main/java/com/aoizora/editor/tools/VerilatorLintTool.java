package com.aoizora.editor.tools;

public class VerilatorLintTool implements Tool {

    @Override
    public String getName() {
        return "Verilator Lint";
    }

    @Override
    public void execute(OutputSink output, String currentFilePath) {
        output.clear();
        if (currentFilePath == null) {
            output.appendLine("Ошибка: сначала откройте или сохраните файл");
            return;
        }
        output.appendLine(">>> verilator --lint-only -Wall " + currentFilePath);
        ToolRunner runner = new ToolRunner();
        runner.execute(output, "verilator", "--lint-only", "-Wall", currentFilePath);
    }
}
