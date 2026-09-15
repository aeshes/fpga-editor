package com.aoizora.editor.project;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Модель проекта SystemVerilog (.proj).
 * Формат файла проекта намеренно отделён от отображения:
 * сейчас это XML в духе проектов Visual Studio C#,
 * конкретная схема будет уточнена позже.
 */
public class Project {

    private final File projectFile;
    private final String name;
    private final List<String> files;

    public Project(File projectFile, String name, List<String> files) {
        this.projectFile = projectFile;
        this.name = name;
        this.files = List.copyOf(files);
    }

    public File getProjectFile() {
        return projectFile;
    }

    public String getName() {
        return name;
    }

    /**
     * Относительные пути к файлам проекта (разделитель — '/').
     */
    public List<String> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public File getBaseDirectory() {
        File parent = projectFile.getParentFile();
        return parent != null ? parent : new File(".");
    }

    /**
     * Преобразует относительный путь из проекта в абсолютный файл.
     */
    public File resolve(String relativePath) {
        return new File(getBaseDirectory(), relativePath);
    }
}