package com.aoizora.editor.language;

import eu.mihosoft.monacofx.LanguageSupport;
import eu.mihosoft.monacofx.MonarchSyntaxHighlighter;

/**
 * Подсветка синтаксиса Verilog/SystemVerilog для Monaco Editor.
 * <p>
 * Monaco 0.20.0, встроенный в MonacoFX, не содержит Verilog,
 * поэтому язык регистрируется через Monarch-токенизатор.
 * Палитра токенов близка к тёмной теме Vivado (Xilinx).
 *
 * @see <a href="https://microsoft.github.io/monaco-editor/monarch.html">Monarch</a>
 */
public class VerilogLanguage implements LanguageSupport {

    @Override
    public String getName() {
        return "verilog";
    }

    @Override
    public MonarchSyntaxHighlighter getMonarchSyntaxHighlighter() {
        return () -> MONARCH_RULES;
    }

    private static final String MONARCH_RULES = """
            keywords: [
              'always', 'always_comb', 'always_ff', 'always_latch', 'and',
              'assert', 'assign', 'assume', 'automatic',
              'begin', 'bind', 'break',
              'case', 'casex', 'casez', 'checker', 'class',
              'clocking', 'config', 'const', 'constraint', 'continue',
              'cover', 'covergroup', 'coverpoint', 'cross',
              'deassign', 'default', 'defparam', 'design', 'disable',
              'dist', 'do',
              'edge', 'else', 'end', 'endchecker', 'endclass',
              'endclocking', 'endconfig', 'endfunction', 'endgenerate',
              'endgroup', 'endinterface', 'endmodule', 'endpackage',
              'endprimitive', 'endprogram', 'endproperty', 'endspecify',
              'endsequence', 'endtable', 'endtask', 'enum', 'event',
              'expect', 'export', 'extends', 'extern',
              'final', 'first_match', 'for', 'force', 'foreach',
              'forever', 'fork', 'forkjoin', 'function',
              'generate', 'genvar', 'global',
              'highz0', 'highz1',
              'if', 'iff', 'ifnone', 'implements', 'implies',
              'import', 'incdir', 'include', 'initial', 'inout',
              'input', 'inside', 'interconnect', 'interface', 'intersect',
              'join', 'join_any', 'join_none',
              'large', 'let', 'liblist', 'library', 'local',
              'localparam',
              'macromodule', 'matches', 'medium', 'modport', 'module',
              'nand', 'negedge', 'nettype', 'new', 'nexttime',
              'nmos', 'nor', 'noshowcancelled', 'not', 'notif0',
              'notif1', 'null',
              'or', 'output',
              'package', 'packed', 'parameter', 'pmos', 'posedge',
              'primitive', 'priority', 'program', 'property', 'protected',
              'pull0', 'pull1', 'pulldown', 'pullup',
              'pulsestyle_ondetect', 'pulsestyle_onevent', 'pure',
              'rand', 'randc', 'randcase', 'randsequence', 'rcmos',
              'realtime', 'ref', 'release', 'repeat', 'restrict',
              'return', 'rnmos', 'rpmos', 'rtran', 'rtranif0',
              'rtranif1',
              'scalared', 'sequence', 'showcancelled', 'signed',
              'small', 'solve', 'specify', 'specparam', 'static',
              'strong', 'strong0', 'strong1', 'super',
              'sync_accept_on', 'sync_reject_on',
              'table', 'tagged', 'task', 'this', 'throughout',
              'timeprecision', 'timeunit', 'tran', 'tranif0', 'tranif1',
              'type', 'typedef', 'union', 'unique', 'unique0',
              'unsigned', 'until', 'until_with', 'untyped', 'use',
              'var', 'vectored', 'virtual', 'void',
              'wait', 'wait_order', 'wand', 'weak', 'weak0',
              'weak1', 'while', 'wildcard', 'within', 'wor',
              'xnor', 'xor'
            ],
            types: [
              'bit', 'byte', 'chandle', 'enum', 'event',
              'integer', 'int', 'logic', 'longint', 'reg',
              'real', 'realtime', 'shortint', 'shortreal', 'string',
              'struct', 'time', 'tri', 'tri0', 'tri1',
              'triand', 'trior', 'trireg', 'union', 'uwire',
              'wand', 'wire', 'wor', 'supply0', 'supply1',
              'genvar', 'var', 'packed'
            ],
            tokenizer: {
              root: [
                [/[ \\t\\r\\n]+/, 'white'],
                [/\\/\\/.*$/, 'comment'],
                [/\\/\\*/, 'comment', '@comment'],
                [/`[a-zA-Z_][a-zA-Z0-9_]*/, 'keyword.other.directive'],
                [/\\$[a-zA-Z_][a-zA-Z0-9_]*/, 'keyword.other.systemtask'],
                [/"/, 'string', '@string'],
                [/\\d+'([sS])?[bBoOdDhH][0-9a-fA-FxXzZ_?]*/, 'number'],
                [/'([sS])?[bBoOdDhH][0-9a-fA-FxXzZ_?]*/, 'number'],
                [/\\d+\\.\\d+([eE][+-]?\\d+)?\\b/, 'number'],
                [/\\d+([eE][+-]?\\d+)?\\b/, 'number'],
                [/[a-zA-Z_][a-zA-Z0-9_$]*/, {
                  cases: {
                    '@keywords': 'keyword',
                    '@types': 'type',
                    '@default': 'identifier'
                  }
                }],
                [/[;:,.()\\[\\]{}]/, 'delimiter'],
                [/[=+\\-*/%!<>~&|^?:]+/, 'keyword.other.operator']
              ],
              comment: [
                [/\\*\\//, 'comment', '@pop'],
                [/[^*]+|\\*(?!\\/)/, 'comment']
              ],
              string: [
                [/[^"]+/, 'string'],
                [/"/, 'string', '@pop']
              ]
            }
            """;
}