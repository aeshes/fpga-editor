package com.aoizora.editor.ui;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Читает SVG-иконки из ресурсов /images и извлекает из них данные path-элементов.
 * Сами SVGPath-узлы строит вызывающий код, чтобы раскрашивать их через CSS.
 */
public final class SvgIcons {

    private static final String RESOURCE_DIR = "/images/";

    private SvgIcons() {
    }

    /**
     * Возвращает содержимое атрибута d для всех &lt;path&gt; из SVG-файла в порядке следования.
     */
    public static List<String> pathData(String name) {
        try (InputStream in = SvgIcons.class.getResourceAsStream(RESOURCE_DIR + name + ".svg")) {
            if (in == null) {
                throw new RuntimeException("SVG icon not found: " + RESOURCE_DIR + name + ".svg");
            }
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new InputStreamReader(in, StandardCharsets.UTF_8)));

            NodeList paths = doc.getElementsByTagName("path");
            List<String> result = new ArrayList<>();
            for (int i = 0; i < paths.getLength(); i++) {
                String d = ((Element) paths.item(i)).getAttribute("d");
                if (!d.isEmpty()) {
                    result.add(d);
                }
            }
            if (result.isEmpty()) {
                throw new RuntimeException("No <path> found in " + RESOURCE_DIR + name + ".svg");
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load SVG icon: " + name, e);
        }
    }
}