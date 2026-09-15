package com.aoizora.editor.document;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class DocumentManager {

    private static final String UNTITLED = "Безымянный";

    private File currentFile;
    private String content = "";

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(File file) {
        this.currentFile = file;
    }

    public String getTitle() {
        String name = currentFile != null ? currentFile.getName() : UNTITLED;
        return name;
    }

    public void newDocument() {
        content = "";
        currentFile = null;
    }

    public void load(File file) throws IOException {
        content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        currentFile = file;
    }

    public void save(File file) throws IOException {
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        currentFile = file;
    }

    public boolean hasFile() {
        return currentFile != null;
    }

    public String getAbsolutePath() {
        return currentFile != null ? currentFile.getAbsolutePath() : null;
    }

    public String getParentDirectory() {
        return currentFile != null ? currentFile.getParent() : System.getProperty("user.dir");
    }
}
