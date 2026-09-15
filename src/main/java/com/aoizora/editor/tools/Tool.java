package com.aoizora.editor.tools;

public interface Tool {
    String getName();
    void execute(OutputSink output, String currentFileName);
}
