import java.util.*;

public final class Lexer {
    private final String src;
    private final String fileName;
    private final List<Token> tokens = new ArrayList<>();

    private int pos = 0;
    private int line = 1;
    private int col = 1;

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("boolean", TokenType.BOOLEAN),
            Map.entry("break",   TokenType.BREAK),
            Map.entry("class",   TokenType.CLASS),
            Map.entry("continue",TokenType.CONTINUE),
            Map.entry("else",    TokenType.ELSE),
            Map.entry("extern",  TokenType.EXTERN),
            Map.entry("false",   TokenType.FALSE),
            Map.entry("for",     TokenType.FOR),
            Map.entry("if",      TokenType.IF),
            Map.entry("int",     TokenType.INT),
            Map.entry("long",    TokenType.LONG),
            Map.entry("return",  TokenType.RETURN),
            Map.entry("true",    TokenType.TRUE),
            Map.entry("void",    TokenType.VOID),
            Map.entry("while",   TokenType.WHILE)
    );

    public Lexer(String src, String fileName) {
        this.src = src;
        this.fileName = fileName;
    }

    public List<Token> scan() {
        while (pos < src.length()) {
            char c = src.charAt(pos);

            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') { advance(); continue; }
            if (c == '/' && peek(1) == '/') { skipLineComment(); continue; }
            if (c == '/' && peek(1) == '*') { skipBlockComment(); continue; }
            if (c == '"') { readStringLiteral(); continue; }
            if (c == '\'') { readCharLiteral(); continue; }
            if (Character.isDigit(c)) { readNumber(); continue; }
            if (Character.isLetter(c) || c == '_') { readIdentifierOrKeyword(); continue; }

            int startLine = line, startCol = col;
            switch (c) {
                case '(': emit(TokenType.LPAREN, "("); break;
                case ')': emit(TokenType.RPAREN, ")"); break;
                case '{': emit(TokenType.LBRACE, "{"); break;
                case '}': emit(TokenType.RBRACE, "}"); break;
                case '[': emit(TokenType.LBRACKET, "["); break;
                case ']': emit(TokenType.RBRACKET, "]"); break;
                case ';': emit(TokenType.SEMI, ";"); break;
                case ',': emit(TokenType.COMMA, ","); break;
                case '.': emit(TokenType.DOT, "."); break;
                case '+':
                    if (peek(1) == '+') { advance(); advance(); push(TokenType.INCREMENT, "++", startLine, startCol); }
                    else if (peek(1) == '=') { advance(); advance(); push(TokenType.PLUS_ASSIGN, "+=", startLine, startCol); }
                    else emit(TokenType.PLUS, "+");
                    break;
                case '-':
                    if (peek(1) == '-') { advance(); advance(); push(TokenType.DECREMENT, "--", startLine, startCol); }
                    else if (peek(1) == '=') { advance(); advance(); push(TokenType.MINUS_ASSIGN, "-=", startLine, startCol); }
                    else emit(TokenType.MINUS, "-");
                    break;
                case '*': emit(TokenType.STAR, "*"); break;
                case '/': emit(TokenType.SLASH, "/"); break;
                case '%': emit(TokenType.PERCENT, "%"); break;
                case '=':
                    if (peek(1) == '=') { advance(); advance(); push(TokenType.EQ, "==", startLine, startCol); }
                    else emit(TokenType.ASSIGN, "=");
                    break;
                case '!':
                    if (peek(1) == '=') { advance(); advance(); push(TokenType.NE, "!=", startLine, startCol); }
                    else emit(TokenType.NOT, "!");
                    break;
                case '<':
                    if (peek(1) == '=') { advance(); advance(); push(TokenType.LE, "<=", startLine, startCol); }
                    else emit(TokenType.LT, "<");
                    break;
                case '>':
                    if (peek(1) == '=') { advance(); advance(); push(TokenType.GE, ">=", startLine, startCol); }
                    else emit(TokenType.GT, ">");
                    break;
                case '&':
                    if (peek(1) == '&') { advance(); advance(); push(TokenType.AND, "&&", startLine, startCol); }
                    else error("недопустимый символ '&'");
                    break;
                case '|':
                    if (peek(1) == '|') { advance(); advance(); push(TokenType.OR, "||", startLine, startCol); }
                    else error("недопустимый символ '|'");
                    break;
                default:
                    error("недопустимый символ '" + c + "'");
            }
        }
        tokens.add(new Token(TokenType.EOF, "", line, col));
        return tokens;
    }

    private char peek(int k) { int i = pos + k; return i < src.length() ? src.charAt(i) : '\0'; }

    private void advance() {
        if (pos >= src.length()) return;
        if (src.charAt(pos) == '\n') { line++; col = 1; } else { col++; }
        pos++;
    }

    private void push(TokenType t, String text, int l, int c) { tokens.add(new Token(t, text, l, c)); }

    private void emit(TokenType t, String text) {
        push(t, text, line, col);
        advance();
    }

    private void error(String msg) {
        System.err.println(fileName + ":" + line + ":" + col + ": " + msg);
        advance();
    }

    private void readIdentifierOrKeyword() {
        int startLine = line, startCol = col, start = pos;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_') advance();
            else break;
        }
        String word = src.substring(start, pos);
        TokenType kw = KEYWORDS.get(word);
        if (kw == TokenType.TRUE || kw == TokenType.FALSE) push(TokenType.BOOLEANLITERAL, word, startLine, startCol);
        else if (kw != null) push(kw, word, startLine, startCol);
        else push(TokenType.IDENTIFIER, word, startLine, startCol);
    }

    private void readNumber() {
        int startLine = line, startCol = col, start = pos;
        if (src.charAt(pos) == '0' && (peek(1) == 'x' || peek(1) == 'X')) {
            advance(); advance();
            while (pos < src.length() && isHex(src.charAt(pos))) advance();
        } else {
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) advance();
        }
        boolean isLong = false;
        if (pos < src.length() && (src.charAt(pos) == 'L' || src.charAt(pos) == 'l')) { isLong = true; advance(); }
        String text = src.substring(start, pos);
        push(isLong ? TokenType.LONGLITERAL : TokenType.INTLITERAL, text, startLine, startCol);
    }

    private boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private void readStringLiteral() {
        int startLine = line, startCol = col;
        advance();
        boolean closed = false;
        StringBuilder raw = new StringBuilder("\"");
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '\n') break;
            if (c == '\\') {
                raw.append(c); advance();
                if (pos < src.length()) { raw.append(src.charAt(pos)); advance(); }
                continue;
            }
            if (c == '"') { raw.append(c); advance(); closed = true; break; }
            raw.append(c); advance();
        }
        if (!closed) { error("незакрытый строковый литерал"); return; }
        push(TokenType.STRINGLITERAL, raw.toString(), startLine, startCol);
    }

    private void readCharLiteral() {
        int startLine = line, startCol = col;
        advance();
        StringBuilder raw = new StringBuilder("'");
        boolean closed = false;
        if (pos < src.length() && src.charAt(pos) == '\\') {
            raw.append('\\'); advance();
            if (pos < src.length()) { raw.append(src.charAt(pos)); advance(); }
        } else if (pos < src.length() && src.charAt(pos) != '\'') {
            raw.append(src.charAt(pos)); advance();
        }
        if (pos < src.length() && src.charAt(pos) == '\'') {
            raw.append('\''); advance(); closed = true;
        }
        if (!closed) { error("незакрытый символьный литерал"); return; }
        push(TokenType.CHARLITERAL, raw.toString(), startLine, startCol);
    }

    private void skipLineComment() {
        advance(); advance();
        while (pos < src.length() && src.charAt(pos) != '\n') advance();
    }

    private void skipBlockComment() {
        int startLine = line, startCol = col;
        advance(); advance();
        while (pos < src.length()) {
            if (src.charAt(pos) == '*' && peek(1) == '/') { advance(); advance(); return; }
            advance();
        }
        error("незакрытый блочный комментарий (открыт на " + startLine + ":" + startCol + ")");
    }
}