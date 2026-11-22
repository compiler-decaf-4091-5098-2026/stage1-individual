import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("bool", TokenType.KW_BOOL);
        KEYWORDS.put("break", TokenType.KW_BREAK);
        KEYWORDS.put("const", TokenType.KW_CONST);
        KEYWORDS.put("continue", TokenType.KW_CONTINUE);
        KEYWORDS.put("else", TokenType.KW_ELSE);
        KEYWORDS.put("for", TokenType.KW_FOR);
        KEYWORDS.put("if", TokenType.KW_IF);
        KEYWORDS.put("import", TokenType.KW_IMPORT);
        KEYWORDS.put("int", TokenType.KW_INT);
        KEYWORDS.put("len", TokenType.KW_LEN);
        KEYWORDS.put("long", TokenType.KW_LONG);
        KEYWORDS.put("return", TokenType.KW_RETURN);
        KEYWORDS.put("void", TokenType.KW_VOID);
        KEYWORDS.put("while", TokenType.KW_WHILE);
        KEYWORDS.put("true", TokenType.BOOLEANLITERAL);
        KEYWORDS.put("false", TokenType.BOOLEANLITERAL);
    }

    private final String filename;
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    private int pos = 0;
    private int line = 1;
    private int col = 1;

    public Scanner(String filename, String source) {
        this.filename = filename;
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            skipWhitespaceAndComments();
            if (isAtEnd()) break;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", line, col));
        return tokens;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    private void skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
            } else if (c == '/' && peekNext() == '/') {
                while (!isAtEnd() && peek() != '\n') {
                    advance();
                }
            } else if (c == '/' && peekNext() == '*') {
                int startLine = line;
                int startCol = col;
                advance(); // '/'
                advance(); // '*'
                boolean closed = false;
                while (!isAtEnd()) {
                    if (peek() == '*' && peekNext() == '/') {
                        advance();
                        advance();
                        closed = true;
                        break;
                    }
                    advance();
                }
                if (!closed) {
                    reportError(startLine, startCol, "Незакрытый многострочный комментарий");
                }
            } else {
                break;
            }
        }
    }

    private void scanToken() {
        int startPos = pos;
        int startLine = line;
        int startCol = col;
        char c = advance();

        switch (c) {
            case '(' -> addToken(TokenType.LPAREN, "(", startLine, startCol);
            case ')' -> addToken(TokenType.RPAREN, ")", startLine, startCol);
            case '{' -> addToken(TokenType.LBRACE, "{", startLine, startCol);
            case '}' -> addToken(TokenType.RBRACE, "}", startLine, startCol);
            case '[' -> addToken(TokenType.LBRACKET, "[", startLine, startCol);
            case ']' -> addToken(TokenType.RBRACKET, "]", startLine, startCol);
            case ';' -> addToken(TokenType.SEMICOLON, ";", startLine, startCol);
            case ',' -> addToken(TokenType.COMMA, ",", startLine, startCol);

            case '+' -> {
                if (match('+')) addToken(TokenType.INC, "++", startLine, startCol);
                else if (match('=')) addToken(TokenType.PLUS_ASSIGN, "+=", startLine, startCol);
                else addToken(TokenType.PLUS, "+", startLine, startCol);
            }
            case '-' -> {
                if (match('-')) addToken(TokenType.DEC, "--", startLine, startCol);
                else if (match('=')) addToken(TokenType.MINUS_ASSIGN, "-=", startLine, startCol);
                else addToken(TokenType.MINUS, "-", startLine, startCol);
            }
            case '*' -> {
                if (match('=')) addToken(TokenType.STAR_ASSIGN, "*=", startLine, startCol);
                else addToken(TokenType.STAR, "*", startLine, startCol);
            }
            case '/' -> {
                if (match('=')) addToken(TokenType.SLASH_ASSIGN, "/=", startLine, startCol);
                else addToken(TokenType.SLASH, "/", startLine, startCol);
            }
            case '%' -> {
                if (match('=')) addToken(TokenType.PERCENT_ASSIGN, "%=", startLine, startCol);
                else addToken(TokenType.PERCENT, "%", startLine, startCol);
            }
            case '=' -> {
                if (match('=')) addToken(TokenType.EQ, "==", startLine, startCol);
                else addToken(TokenType.ASSIGN, "=", startLine, startCol);
            }
            case '!' -> {
                if (match('=')) addToken(TokenType.NEQ, "!=", startLine, startCol);
                else addToken(TokenType.NOT, "!", startLine, startCol);
            }
            case '<' -> {
                if (match('=')) addToken(TokenType.LE, "<=", startLine, startCol);
                else addToken(TokenType.LT, "<", startLine, startCol);
            }
            case '>' -> {
                if (match('=')) addToken(TokenType.GE, ">=", startLine, startCol);
                else addToken(TokenType.GT, ">", startLine, startCol);
            }
            case '&' -> {
                if (match('&')) addToken(TokenType.AND, "&&", startLine, startCol);
                else reportError(startLine, startCol, "Недопустимый символ '&' (ожидалось '&&')");
            }
            case '|' -> {
                if (match('|')) addToken(TokenType.OR, "||", startLine, startCol);
                else reportError(startLine, startCol, "Недопустимый символ '|' (ожидалось '||')");
            }
            case '\'' -> scanCharLiteral(startPos, startLine, startCol);
            case '"' -> scanStringLiteral(startPos, startLine, startCol);
            default -> {
                if (isDigit(c)) {
                    scanNumber(c, startPos, startLine, startCol);
                } else if (isAlpha(c)) {
                    scanIdentifier(startPos, startLine, startCol);
                } else {
                    reportError(startLine, startCol, "Недопустимый символ '" + c + "'");
                }
            }
        }
    }

    private void scanNumber(char firstChar, int startPos, int startLine, int startCol) {
        if (firstChar == '0' && (peek() == 'x' || peek() == 'X')) {
            advance(); // 'x' или 'X'
            if (!isHexDigit(peek())) {
                reportError(startLine, startCol, "Некорректный шестнадцатеричный литерал");
                return;
            }
            while (isHexDigit(peek())) {
                advance();
            }
        } else {
            while (isDigit(peek())) {
                advance();
            }
        }

        TokenType type = TokenType.INTLITERAL;
        if (peek() == 'L') {
            advance();
            type = TokenType.LONGLITERAL;
        }
        addToken(type, source.substring(startPos, pos), startLine, startCol);
    }

    private void scanIdentifier(int startPos, int startLine, int startCol) {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        String text = source.substring(startPos, pos);
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);
        addToken(type, text, startLine, startCol);
    }

    private void scanCharLiteral(int startPos, int startLine, int startCol) {
        if (isAtEnd() || peek() == '\n') {
            reportError(startLine, startCol, "Незакрытый символьный литерал");
            return;
        }

        char c = advance();
        if (c == '\'') {
            reportError(startLine, startCol, "Пустой символьный литерал");
            return;
        }

        if (c == '\\') {
            if (isAtEnd() || !isValidEscape(peek())) {
                reportError(startLine, startCol, "Недопустимая escape-последовательность в символьном литерале");
                skipUntilCharEnd();
                return;
            }
            advance();
        } else if (!isValidCharContent(c)) {
            reportError(startLine, startCol, "Недопустимый символ в символьном литерале");
            skipUntilCharEnd();
            return;
        }

        if (isAtEnd() || peek() != '\'') {
            reportError(startLine, startCol, "Ожидалась закрывающая одинарная кавычка");
            skipUntilCharEnd();
            return;
        }

        advance(); // закрывающая '\''
        addToken(TokenType.CHARLITERAL, source.substring(startPos, pos), startLine, startCol);
    }

    private void skipUntilCharEnd() {
        while (!isAtEnd() && peek() != '\'' && peek() != '\n') {
            advance();
        }
        if (!isAtEnd() && peek() == '\'') {
            advance();
        }
    }

    private void scanStringLiteral(int startPos, int startLine, int startCol) {
        boolean hasError = false;
        while (!isAtEnd() && peek() != '"' && peek() != '\n') {
            char c = advance();
            if (c == '\\') {
                if (isAtEnd() || !isValidEscape(peek())) {
                    reportError(line, col, "Недопустимая escape-последовательность в строке");
                    hasError = true;
                    if (!isAtEnd() && peek() != '\n') advance();
                } else {
                    advance();
                }
            } else if (!isValidStringContent(c)) {
                reportError(line, col - 1, "Недопустимый символ в строковом литерале");
                hasError = true;
            }
        }

        if (isAtEnd() || peek() != '"') {
            reportError(startLine, startCol, "Незакрытый строковый литерал");
            return;
        }

        advance(); // закрывающая '"'
        if (!hasError) {
            addToken(TokenType.STRINGLITERAL, source.substring(startPos, pos), startLine, startCol);
        }
    }

    private boolean isValidEscape(char c) {
        return c == 'n' || c == 't' || c == 'r' || c == 'f' || c == '\\' || c == '\'' || c == '"';
    }

    private boolean isValidCharContent(char c) {
        return c >= 32 && c <= 126 && c != '\'' && c != '"' && c != '\\';
    }

    private boolean isValidStringContent(char c) {
        return c >= 32 && c <= 126 && c != '"' && c != '\\';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isAtEnd() {
        return pos >= source.length();
    }

    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(pos);
    }

    private char peekNext() {
        return (pos + 1 >= source.length()) ? '\0' : source.charAt(pos + 1);
    }

    private char advance() {
        char c = source.charAt(pos++);
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(pos) != expected) return false;
        advance();
        return true;
    }

    private void addToken(TokenType type, String lexeme, int startLine, int startCol) {
        tokens.add(new Token(type, lexeme, startLine, startCol));
    }

    private void reportError(int errLine, int errCol, String message) {
        errors.add(filename + " line " + errLine + ":" + errCol + ": " + message);
    }
}
