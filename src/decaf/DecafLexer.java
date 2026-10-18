package decaf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public final class DecafLexer {


    public enum Kind {
        IDENT, INT_LIT, LONG_LIT, CHAR_LIT, STRING_LIT, BOOL_LIT,
        KW_BOOL, KW_BREAK, KW_CONTINUE, KW_ELSE, KW_FOR, KW_IF, KW_IMPORT,
        KW_INT, KW_LEN, KW_LONG, KW_RETURN, KW_VOID, KW_WHILE,
        OP_PLUS, OP_MINUS, OP_MUL, OP_DIV, OP_MOD,
        OP_ASSIGN, OP_PLUS_ASSIGN, OP_MINUS_ASSIGN, OP_MUL_ASSIGN, OP_DIV_ASSIGN, OP_MOD_ASSIGN,
        OP_INC, OP_DEC,
        OP_LT, OP_GT, OP_LE, OP_GE, OP_EQ, OP_NE,
        OP_AND, OP_OR, OP_NOT,
        LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET,
        SEMI, COMMA, EOF
    }


    public static final class Token {
        public final Kind kind;
        public final String text;
        public final int row;
        public final int col;

        Token(Kind kind, String text, int row, int col) {
            this.kind = kind;
            this.text = text;
            this.row = row;
            this.col = col;
        }
    }

    private static final Map<String, Kind> KEYWORDS = Map.ofEntries(
            Map.entry("bool", Kind.KW_BOOL),
            Map.entry("break", Kind.KW_BREAK),
            Map.entry("continue", Kind.KW_CONTINUE),
            Map.entry("else", Kind.KW_ELSE),
            Map.entry("for", Kind.KW_FOR),
            Map.entry("if", Kind.KW_IF),
            Map.entry("import", Kind.KW_IMPORT),
            Map.entry("int", Kind.KW_INT),
            Map.entry("len", Kind.KW_LEN),
            Map.entry("long", Kind.KW_LONG),
            Map.entry("return", Kind.KW_RETURN),
            Map.entry("void", Kind.KW_VOID),
            Map.entry("while", Kind.KW_WHILE)
    );

    private final char[] src;
    private final String file;
    private int pos = 0;
    private int row = 1;
    private int col = 1;

    private final List<Token> result = new ArrayList<>();
    private final List<DecafError> problems = new ArrayList<>();

    public DecafLexer(String source, String file) {
        this.src = source.toCharArray();
        this.file = file;
    }

    public List<Token> scan() {
        while (pos < src.length) {
            int startRow = row;
            int startCol = col;
            char c = src[pos];

            if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\f') {
                bump();
                continue;
            }


            if (c == '/' && peek(1) == '/') {
                while (pos < src.length && src[pos] != '\n' && src[pos] != '\r') bump();
                continue;
            }
            if (c == '/' && peek(1) == '*') {
                bump();
                bump();
                boolean closed = false;
                while (pos < src.length) {
                    if (src[pos] == '*' && peek(1) == '/') {
                        bump();
                        bump();
                        closed = true;
                        break;
                    }
                    bump();
                }
                if (!closed) problems.add(new DecafError(file, startRow, startCol, "Незакрытый блочный комментарий"));
                continue;
            }


            if (Character.isLetter(c) || c == '_') {
                StringBuilder sb = new StringBuilder();
                while (pos < src.length && (Character.isLetterOrDigit(src[pos]) || src[pos] == '_')) {
                    sb.append(src[pos]);
                    bump();
                }
                String s = sb.toString();
                if (s.equals("true") || s.equals("false")) {
                    result.add(new Token(Kind.BOOL_LIT, s, startRow, startCol));
                } else {
                    Kind k = KEYWORDS.getOrDefault(s, Kind.IDENT);
                    result.add(new Token(k, s, startRow, startCol));
                }
                continue;
            }


            if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                if (c == '0' && (peek(1) == 'x' || peek(1) == 'X')) {
                    sb.append(src[pos]); bump();
                    sb.append(src[pos]); bump();
                    while (pos < src.length && isHex(src[pos])) { sb.append(src[pos]); bump(); }
                } else {
                    while (pos < src.length && Character.isDigit(src[pos])) { sb.append(src[pos]); bump(); }
                }
                String s = sb.toString();
                if (pos < src.length && src[pos] == 'L') {
                    s += "L";
                    bump();
                    result.add(new Token(Kind.LONG_LIT, s, startRow, startCol));
                } else {
                    result.add(new Token(Kind.INT_LIT, s, startRow, startCol));
                }
                continue;
            }

            // строковый литерал
            if (c == '"') {
                StringBuilder sb = new StringBuilder("\"");
                bump();
                boolean ok = true;
                while (pos < src.length && src[pos] != '"') {
                    if (src[pos] == '\n' || src[pos] == '\r') { ok = false; break; }
                    if (src[pos] == '\\') {
                        sb.append(src[pos]); bump();
                        if (pos >= src.length) { ok = false; break; }
                        if (!isEscape(src[pos])) {
                            problems.add(new DecafError(file, row, col, "Недопустимая escape-последовательность"));
                            ok = false;
                        }
                        sb.append(src[pos]); bump();
                    } else {
                        sb.append(src[pos]); bump();
                    }
                }
                if (ok && pos < src.length && src[pos] == '"') {
                    sb.append('"');
                    bump();
                    result.add(new Token(Kind.STRING_LIT, sb.toString(), startRow, startCol));
                } else {
                    problems.add(new DecafError(file, startRow, startCol, "Незакрытый строковый литерал"));
                }
                continue;
            }

            // символьный литерал
            if (c == '\'') {
                StringBuilder sb = new StringBuilder("'");
                bump();
                boolean ok = true;
                if (pos < src.length && src[pos] == '\\') {
                    sb.append(src[pos]); bump();
                    if (pos >= src.length || !isEscape(src[pos])) {
                        problems.add(new DecafError(file, row, col, "Недопустимая escape-последовательность"));
                        ok = false;
                    } else { sb.append(src[pos]); bump(); }
                } else if (pos < src.length && src[pos] != '\'') {
                    sb.append(src[pos]); bump();
                } else { ok = false; }

                if (ok && pos < src.length && src[pos] == '\'') {
                    sb.append('\'');
                    bump();
                    result.add(new Token(Kind.CHAR_LIT, sb.toString(), startRow, startCol));
                } else {
                    problems.add(new DecafError(file, startRow, startCol, "Некорректный символьный литерал"));
                }
                continue;
            }

            // двухсимвольные операторы
            String two = (pos + 1 < src.length) ? ("" + src[pos] + src[pos + 1]) : "";
            switch (two) {
                case "==": emit(Kind.OP_EQ, "==", startRow, startCol); continue;
                case "!=": emit(Kind.OP_NE, "!=", startRow, startCol); continue;
                case "<=": emit(Kind.OP_LE, "<=", startRow, startCol); continue;
                case ">=": emit(Kind.OP_GE, ">=", startRow, startCol); continue;
                case "&&": emit(Kind.OP_AND, "&&", startRow, startCol); continue;
                case "||": emit(Kind.OP_OR, "||", startRow, startCol); continue;
                case "++": emit(Kind.OP_INC, "++", startRow, startCol); continue;
                case "--": emit(Kind.OP_DEC, "--", startRow, startCol); continue;
                case "+=": emit(Kind.OP_PLUS_ASSIGN, "+=", startRow, startCol); continue;
                case "-=": emit(Kind.OP_MINUS_ASSIGN, "-=", startRow, startCol); continue;
                case "*=": emit(Kind.OP_MUL_ASSIGN, "*=", startRow, startCol); continue;
                case "/=": emit(Kind.OP_DIV_ASSIGN, "/=", startRow, startCol); continue;
                case "%=": emit(Kind.OP_MOD_ASSIGN, "%=", startRow, startCol); continue;
            }

            // односимвольные
            switch (c) {
                case '+': emit(Kind.OP_PLUS, "+", startRow, startCol); continue;
                case '-': emit(Kind.OP_MINUS, "-", startRow, startCol); continue;
                case '*': emit(Kind.OP_MUL, "*", startRow, startCol); continue;
                case '/': emit(Kind.OP_DIV, "/", startRow, startCol); continue;
                case '%': emit(Kind.OP_MOD, "%", startRow, startCol); continue;
                case '=': emit(Kind.OP_ASSIGN, "=", startRow, startCol); continue;
                case '<': emit(Kind.OP_LT, "<", startRow, startCol); continue;
                case '>': emit(Kind.OP_GT, ">", startRow, startCol); continue;
                case '!': emit(Kind.OP_NOT, "!", startRow, startCol); continue;
                case '(': emit(Kind.LPAREN, "(", startRow, startCol); continue;
                case ')': emit(Kind.RPAREN, ")", startRow, startCol); continue;
                case '{': emit(Kind.LBRACE, "{", startRow, startCol); continue;
                case '}': emit(Kind.RBRACE, "}", startRow, startCol); continue;
                case '[': emit(Kind.LBRACKET, "[", startRow, startCol); continue;
                case ']': emit(Kind.RBRACKET, "]", startRow, startCol); continue;
                case ';': emit(Kind.SEMI, ";", startRow, startCol); continue;
                case ',': emit(Kind.COMMA, ",", startRow, startCol); continue;
            }

            problems.add(new DecafError(file, startRow, startCol, "Недопустимый символ '" + c + "'"));
            bump();
        }

        result.add(new Token(Kind.EOF, "", row, col));
        return result;
    }

    public List<DecafError> problems() { return problems; }

    private void emit(Kind k, String text, int r, int c) {
        result.add(new Token(k, text, r, c));
        bump();
    }

    private void bump() {
        char c = src[pos++];
        if (c == '\n') { row++; col = 1; }
        else if (c == '\r') {
            if (pos < src.length && src[pos] == '\n') {
                pos++;
                row++;
                col = 1;
            } else { row++; col = 1; }
        } else col++;
    }

    private char peek(int ahead) {
        int i = pos + ahead;
        return i < src.length ? src[i] : '\0';
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean isEscape(char c) {
        return c == '"' || c == '\'' || c == '\\' || c == 't' || c == 'n' || c == 'r' || c == 'f';
    }
}