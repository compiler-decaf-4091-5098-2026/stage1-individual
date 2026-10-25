package decaf;

import java.util.Set;

public final class Lexer {
    private static final Set<String> KEYWORDS = Set.of(
        "if", "bool", "break", "import", "continue", "else", "for", "while",
        "int", "long", "return", "len", "void");
    private static final Set<String> DOUBLE_OPERATORS = Set.of(
        "+=", "-=", "*=", "/=", "%=", "++", "--", "<=", ">=", "==", "!=", "&&", "||");
    private final String source;
    private final Diagnostics diagnostics;
    private int offset;
    private int line = 1;
    private int column = 1;

    public Lexer(String source, Diagnostics diagnostics) {
        this.source = source;
        this.diagnostics = diagnostics;
    }

    public Token next() {
        while (offset < source.length()) {
            char ch = peek(0);
            if (" \t\r\n\f".indexOf(ch) >= 0) { advance(); continue; }
            int start = offset, row = line, col = column;
            if (ch == '/' && peek(1) == '/') {
                while (offset < source.length() && peek(0) != '\r' && peek(0) != '\n') advance();
                continue;
            }
            if (ch == '/' && peek(1) == '*') {
                advance(); advance();
                while (offset < source.length() && !(peek(0) == '*' && peek(1) == '/')) advance();
                if (offset == source.length()) diagnostics.error(row, col, "unterminated block comment");
                else { advance(); advance(); }
                continue;
            }
            if (alpha(ch)) {
                do { advance(); } while (alpha(peek(0)) || digit(peek(0)));
                String text = source.substring(start, offset);
                String kind = "IDENTIFIER";
                if (text.equals("true") || text.equals("false")) {
                    kind = "BOOLEANLITERAL";
                } else if (KEYWORDS.contains(text)) {
                    kind = text;
                }
                return new Token(kind, text, row, col);
            }
            if (digit(ch)) {
                if (ch == '0' && peek(1) == 'x') {
                    advance(); advance();
                    if (!hex(peek(0))) {
                        diagnostics.error(row, col, "hexadecimal literal requires a digit after 0x");
                        continue;
                    }
                    while (hex(peek(0))) advance();
                } else {
                    do { advance(); } while (digit(peek(0)));
                }
                String kind = "INTLITERAL";
                if (peek(0) == 'L') { advance(); kind = "LONGLITERAL"; }
                return token(kind, start, row, col);
            }
            if (ch == '\'' || ch == '"') {
                Token literal = quoted(ch, start, row, col);
                if (literal != null) return literal;
                continue;
            }
            if (offset + 1 < source.length()) {
                String pair = source.substring(offset, offset + 2);
                if (DOUBLE_OPERATORS.contains(pair)) {
                    advance(); advance();
                    return token(pair, start, row, col);
                }
            }
            if ("+-*/%=<>!(){}[],;".indexOf(ch) >= 0) {
                advance();
                return token(String.valueOf(ch), start, row, col);
            }
            diagnostics.error(row, col, String.format("invalid character U+%04X", (int) ch));
            advance();
        }
        return new Token("EOF", "", line, column);
    }

    private Token quoted(char quote, int start, int row, int col) {
        advance();
        int characters = 0;
        boolean valid = true;
        while (offset < source.length() && peek(0) != quote) {
            char ch = peek(0);
            if (ch == '\r' || ch == '\n') {
                diagnostics.error(row, col, "unterminated " + (quote == '"' ? "string" : "character") + " literal");
                return null;
            }
            int charLine = line, charColumn = column;
            if (ch == '\\') {
                advance();
                char escaped = peek(0);
                if (offset == source.length() || escaped == '\r' || escaped == '\n') {
                    diagnostics.error(row, col, "unterminated escape sequence in literal");
                    return null;
                }
                if ("\"'\\tnrf".indexOf(escaped) < 0) {
                    diagnostics.error(charLine, charColumn, "invalid escape sequence: \\" + escaped);
                    valid = false;
                }
                advance();
            } else {
                if (ch < 32 || ch > 126 || ch == '\'' || ch == '"') {
                    diagnostics.error(charLine, charColumn, "literal character must be printable ASCII; quotes must be escaped");
                    valid = false;
                }
                advance();
            }
            characters++;
        }
        if (offset == source.length()) {
            diagnostics.error(row, col, "unterminated " + (quote == '"' ? "string" : "character") + " literal");
            return null;
        }
        advance();
        if (quote == '\'' && characters != 1) {
            diagnostics.error(row, col, "character literal must contain exactly one character");
            valid = false;
        }
        return valid ? token(quote == '"' ? "STRINGLITERAL" : "CHARLITERAL", start, row, col) : null;
    }

    private Token token(String kind, int start, int row, int col) {
        return new Token(kind, source.substring(start, offset), row, col);
    }

    private char peek(int ahead) {
        return offset + ahead < source.length() ? source.charAt(offset + ahead) : '\0';
    }

    private void advance() {
        char ch = source.charAt(offset++);
        if (ch == '\r') {
            if (peek(0) == '\n') offset++;
            line++; column = 1;
        } else if (ch == '\n') {
            line++; column = 1;
        } else column++;
    }

    private static boolean alpha(char ch) {
        return ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch == '_';
    }
    private static boolean digit(char ch) { return ch >= '0' && ch <= '9'; }
    private static boolean hex(char ch) {
        return digit(ch) || ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F';
    }
}
