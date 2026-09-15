package com.aoizora.editor.project;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Читает файл проекта .proj (XML в духе Visual Studio C#).
 *
 * Сейчас извлекаются все элементы, у которых есть атрибут Include
 * (например, {@code <Compile Include="src/alu.sv" />}).
 * Схема будет расширена, когда формат проекта будет утверждён.
 */
public class ProjectParser {

    public Project parse(File projectFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(projectFile);
        document.getDocumentElement().normalize();

        List<String> files = new ArrayList<>();
        collectIncludes(document.getDocumentElement(), files);

        return new Project(projectFile, baseName(projectFile.getName()), files);
    }

    private void collectIncludes(Element element, List<String> files) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element child = (Element) node;
            if (child.hasAttribute("Include") && !child.hasAttribute("Exclude")) {
                String include = child.getAttribute("Include")
                        .replace('\\', '/')
                        .trim();
                if (!include.isEmpty() && !files.contains(include)) {
                    files.add(include);
                }
            }
            collectIncludes(child, files);
        }
    }

    private String baseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}