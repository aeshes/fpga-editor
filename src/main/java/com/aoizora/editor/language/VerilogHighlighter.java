package com.aoizora.editor.language;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

/**
 * Подсветка синтаксиса Verilog/SystemVerilog для RichTextFX.
 * <p>
 * Порт Monarch-токенизатора на Java-регэкспы с именованными группами.
 * Каждый токен сопоставляется CSS-классу в {@code styles/editor.css};
 * палитра близка к тёмной теме Vivado (Xilinx).
 */
public final class VerilogHighlighter {

    private static final List<String> KEYWORDS = List.of(
            "always", "always_comb", "always_ff", "always_latch", "and",
            "assert", "assign", "assume", "automatic",
            "begin", "bind", "break",
            "case", "casex", "casez", "checker", "class",
            "clocking", "config", "const", "constraint", "continue",
            "cover", "covergroup", "coverpoint", "cross",
            "deassign", "default", "defparam", "design", "disable",
            "dist", "do",
            "edge", "else", "end", "endchecker", "endclass",
            "endclocking", "endconfig", "endfunction", "endgenerate",
            "endgroup", "endinterface", "endmodule", "endpackage",
            "endprimitive", "endprogram", "endproperty", "endspecify",
            "endsequence", "endtable", "endtask", "enum", "event",
            "expect", "export", "extends", "extern",
            "final", "first_match", "for", "force", "foreach",
            "forever", "fork", "forkjoin", "function",
            "generate", "genvar", "global",
            "highz0", "highz1",
            "if", "iff", "ifnone", "implements", "implies",
            "import", "incdir", "include", "initial", "inout",
            "input", "inside", "interconnect", "interface", "intersect",
            "join", "join_any", "join_none",
            "large", "let", "liblist", "library", "local",
            "localparam",
            "macromodule", "matches", "medium", "modport", "module",
            "nand", "negedge", "nettype", "new", "nexttime",
            "nmos", "nor", "noshowcancelled", "not", "notif0",
            "notif1", "null",
            "or", "output",
            "package", "packed", "parameter", "pmos", "posedge",
            "primitive", "priority", "program", "property", "protected",
            "pull0", "pull1", "pulldown", "pullup",
            "pulsestyle_ondetect", "pulsestyle_onevent", "pure",
            "rand", "randc", "randcase", "randsequence", "rcmos",
            "realtime", "ref", "release", "repeat", "restrict",
            "return", "rnmos", "rpmos", "rtran", "rtranif0",
            "rtranif1",
            "scalared", "sequence", "showcancelled", "signed",
            "small", "solve", "specify", "specparam", "static",
            "strong", "strong0", "strong1", "super",
            "sync_accept_on", "sync_reject_on",
            "table", "tagged", "task", "this", "throughout",
            "timeprecision", "timeunit", "tran", "tranif0", "tranif1",
            "type", "typedef", "union", "unique", "unique0",
            "unsigned", "until", "until_with", "untyped", "use",
            "var", "vectored", "virtual", "void",
            "wait", "wait_order", "wand", "weak", "weak0",
            "weak1", "while", "wildcard", "within", "wor",
            "xnor", "xor");

    private static final List<String> TYPES = List.of(
            "bit", "byte", "chandle", "enum", "event",
            "integer", "int", "logic", "longint", "reg",
            "real", "realtime", "shortint", "shortreal", "string",
            "struct", "time", "tri", "tri0", "tri1",
            "triand", "trior", "trireg", "union", "uwire",
            "wand", "wire", "wor", "supply0", "supply1",
            "genvar", "var", "packed");

    private static final String KEYWORD_PATTERN =
            "\\b(?:" + String.join("|", KEYWORDS) + ")\\b";

    private static final String TYPE_PATTERN =
            "\\b(?:" + String.join("|", TYPES) + ")\\b";

    private static final String COMMENT_PATTERN = "//[^\\n]*|/\\*(?:.|\\R)*?\\*/";
    private static final String DIRECTIVE_PATTERN = "`[a-zA-Z_][a-zA-Z0-9_]*";
    private static final String SYSTEMTASK_PATTERN = "\\$[a-zA-Z_][a-zA-Z0-9_]*";
    private static final String STRING_PATTERN = "\"(?:[^\"\\\\]|\\\\.)*\"";
    private static final String NUMBER_PATTERN =
            "\\d+'[sS]?[bBoOdDhH][0-9a-fA-FxXzZ_?]*"
            + "|'[sS]?[bBoOdDhH][0-9a-fA-FxXzZ_?]*"
            + "|\\d+\\.\\d+(?:[eE][+-]?\\d+)?\\b"
            + "|\\d+(?:[eE][+-]?\\d+)?\\b";
    private static final String DELIMITER_PATTERN = "[;:,.()\\[\\]{}]";
    private static final String OPERATOR_PATTERN = "[=+\\-*/%!<>~&|^?:]+";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<COMMENT>" + COMMENT_PATTERN + ")"
            + "|(?<DIRECTIVE>" + DIRECTIVE_PATTERN + ")"
            + "|(?<SYSTEMTASK>" + SYSTEMTASK_PATTERN + ")"
            + "|(?<STRING>" + STRING_PATTERN + ")"
            + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
            + "|(?<KEYWORD>" + KEYWORD_PATTERN + ")"
            + "|(?<TYPE>" + TYPE_PATTERN + ")"
            + "|(?<DELIMITER>" + DELIMITER_PATTERN + ")"
            + "|(?<OPERATOR>" + OPERATOR_PATTERN + ")");

    private static final Map<String, String> GROUP_TO_CLASS = Map.of(
            "COMMENT", "comment",
            "DIRECTIVE", "directive",
            "SYSTEMTASK", "systemtask",
            "STRING", "string",
            "NUMBER", "number",
            "KEYWORD", "keyword",
            "TYPE", "type",
            "DELIMITER", "delimiter",
            "OPERATOR", "operator");

    private VerilogHighlighter() {
    }

    /**
     * Вычисляет стилизованные фрагменты для всего текста.
     *
     * @param text исходный текст файла
     * @return набор стилей, где каждый фрагмент помечен CSS-классом токена
     */
    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int lastEnd = 0;

        while (matcher.find()) {
            builder.add(Collections.emptyList(), matcher.start() - lastEnd);

            String styleClass = null;
            for (Map.Entry<String, String> entry : GROUP_TO_CLASS.entrySet()) {
                if (matcher.group(entry.getKey()) != null) {
                    styleClass = entry.getValue();
                    break;
                }
            }

            if (styleClass != null) {
                builder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            }
            lastEnd = matcher.end();
        }

        builder.add(Collections.emptyList(), text.length() - lastEnd);
        return builder.create();
    }
}