package com.aoizora.editor.tools;

import java.io.File;

public class GtkwaveTool implements Tool {

    @Override
    public String getName() {
        return "Open Waveform";
    }

    @Override
    public void execute(OutputSink output, String currentFilePath) {
        String vcdFile;
        if (currentFilePath != null) {
            String dir = new File(currentFilePath).getParent();
            vcdFile = dir + File.separator + "dump.vcd";
        } else {
            vcdFile = "dump.vcd";
        }
        output.appendLine(">>> gtkwave " + vcdFile);
        ToolRunner runner = new ToolRunner();
        runner.execute(output, "gtkwave", vcdFile);
    }
}
