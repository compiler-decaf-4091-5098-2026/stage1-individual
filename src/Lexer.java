import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String source;
    private final String fileName;

    private final List<Token> tokens = new ArrayList<>();
    private final List<CompilerError> errors = new ArrayList<>();
    private final Map<String, TokenType> keywords = new HashMap<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 1;
    private int startLine = 1;
    private int startColumn = 1;

    public Lexer(String source, String fileName) {
        this.source = source;
        this.fileName = fileName;

        keywords.put("bool", TokenType.BOOL);
        keywords.put("break", TokenType.BREAK);
        keywords.put("continue", TokenType.CONTINUE);
        keywords.put("else", TokenType.ELSE);
        keywords.put("for", TokenType.FOR);
        keywords.put("if", TokenType.IF);
        keywords.put("import", TokenType.IMPORT);
        keywords.put("int", TokenType.INT);
        keywords.put("len", TokenType.LEN);
        keywords.put("long", TokenType.LONG);
        keywords.put("return", TokenType.RETURN);
        keywords.put("void", TokenType.VOID);
        keywords.put("while", TokenType.WHILE);
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            startLine = line;
            startColumn = column;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    public List<CompilerError> getErrors() {
        return errors;
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            case '(':
                addToken(TokenType.LEFT_PAREN);
                break;
            case ')':
                addToken(TokenType.RIGHT_PAREN);
                break;
            case '{':
                addToken(TokenType.LEFT_BRACE);
                break;
            case '}':
                addToken(TokenType.RIGHT_BRACE);
                break;
            case '[':
                addToken(TokenType.LEFT_BRACKET);
                break;
            case ']':
                addToken(TokenType.RIGHT_BRACKET);
                break;
            case ';':
                addToken(TokenType.SEMICOLON);
                break;
            case ',':
                addToken(TokenType.COMMA);
                break;

            case '+':
                if (match('+')) { addToken(TokenType.INCREMENT); }
                else if (match('=')) { addToken(TokenType.PLUS_ASSIGN); }
                else { addToken(TokenType.PLUS); }
                break;
            case '-':
                if (match('-')) { addToken(TokenType.DECREMENT); }
                else if (match('=')) { addToken(TokenType.MINUS_ASSIGN); }
                else { addToken(TokenType.MINUS); }
                break;
            case '*':
                if (match('=')) { addToken(TokenType.MULTIPLY_ASSIGN); }
                else { addToken(TokenType.MULTIPLY); }
                break;
            case '/':
                if (match('/')) { skipLineComment(); }
                else if (match('*')) { skipBlockComment(); }
                else if (match('=')) { addToken(TokenType.DIVIDE_ASSIGN); }
                else { addToken(TokenType.DIVIDE); }
                break;
            case '%':
                if (match('=')) { addToken(TokenType.MODULO_ASSIGN); }
                else { addToken(TokenType.MODULO); }
                break;
            case '=':
                if (match('=')) { addToken(TokenType.EQUAL); }
                else { addToken(TokenType.ASSIGN); }
                break;
            case '!':
                if (match('=')) { addToken(TokenType.NOT_EQUAL); }
                else { addToken(TokenType.NOT); }
                break;
            case '<':
                if (match('=')) { addToken(TokenType.LESS_EQUAL); }
                else { addToken(TokenType.LESS); }
                break;
            case '>':
                if (match('=')) { addToken(TokenType.GREATER_EQUAL); }
                else { addToken(TokenType.GREATER); }
                break;
            case '&':
                if (match('&')) { addToken(TokenType.AND); }
                else { addError(startLine, startColumn, "Недопустимый символ '&'"); }
                break;
            case '|':
                if (match('|')) { addToken(TokenType.OR); }
                else { addError(startLine, startColumn, "Недопустимый символ '|'"); }
                break;
            case '"':
                scanStringLiteral();
                break;
            case '\'':
                scanCharLiteral();
                break;

            case ' ':
            case '\t':
            case '\n':
            case '\r':
            case '\f':
                break;

            default:
                if (isDigit(c)) { scanNumber(); }
                else if (isAlpha(c)) { scanIdentifier(); }
                else { addError(startLine, startColumn, "Недопустимый символ '" + c + "'"); }
                break;
        }
    }

    private void scanIdentifier() {
        while (isAlphaNumeric(peek()))
            advance();

        String text = source.substring(start, current);
        if (text.equals("true") || text.equals("false")) {
            addToken(TokenType.BOOLEAN_LITERAL);
            return;
        }

        TokenType type = keywords.get(text);
        if (type == null)
            type = TokenType.IDENTIFIER;
        addToken(type);
    }

    private void scanNumber() {
        if (source.charAt(start) == '0' && peek() == 'x') {
            advance();
            if (!isHexDigit(peek())) {
                addError(startLine, startColumn, "Некорректный шестнадцатеричный литерал");
                return;
            }
            while (isHexDigit(peek()))
                advance();
        } else {
            while (isDigit(peek()))
                advance();
        }

        if (peek() == 'L') {
            advance();
            addToken(TokenType.LONG_LITERAL);
        } else
            addToken(TokenType.INT_LITERAL);
    }

    private void scanStringLiteral() {
        boolean valid = true;

        while (!isAtEnd() && peek() != '"') {
            if (peek() == '\n' || peek() == '\r') {
                addError(startLine, startColumn, "Незакрытый строковый литерал");
                return;
            }
            if (peek() == '\\') {
                int errorLine = line;
                int errorColumn = column;
                advance();
                if (isAtEnd() || peek() == '\n' || peek() == '\r') {
                    addError(startLine, startColumn, "Незакрытый строковый литерал");
                    return;
                }
                if (!isValidEscape(peek())) {
                    addError(errorLine, errorColumn, "Недопустимая escape-последовательность");
                    valid = false;
                }
                advance();
            } else {
                if (!isValidLiteralChar(peek())) {
                    addError(line, column, "Недопустимый символ в строковом литерале");
                    valid = false;
                }
                advance();
            }
        }

        if (isAtEnd()) {
            addError(startLine, startColumn, "Незакрытый строковый литерал");
            return;
        }
        advance();
        if (valid)
            addToken(TokenType.STRING_LITERAL);
    }

    private void scanCharLiteral() {
        boolean valid = true;

        if (isAtEnd() || peek() == '\n' || peek() == '\r') {
            addError(startLine, startColumn, "Незакрытый символьный литерал");
            return;
        }
        if (peek() == '\'') {
            advance();
            addError(startLine, startColumn, "Пустой символьный литерал");
            return;
        }

        if (peek() == '\\') {
            int errorLine = line;
            int errorColumn = column;
            advance();
            if (isAtEnd() || peek() == '\n' || peek() == '\r') {
                addError(startLine, startColumn, "Незакрытый символьный литерал");
                return;
            }
            if (!isValidEscape(peek())) {
                addError(errorLine, errorColumn, "Недопустимая escape-последовательность");
                valid = false;
            }
            advance();
        } else {
            if (!isValidLiteralChar(peek())) {
                addError(line, column, "Недопустимый символ в символьном литерале");
                valid = false;
            }
            advance();
        }

        if (isAtEnd() || peek() == '\n' || peek() == '\r') {
            addError(startLine, startColumn, "Незакрытый символьный литерал");
            return;
        }
        if (peek() != '\'') {
            while (!isAtEnd() && peek() != '\'' && peek() != '\n' && peek() != '\r')
                advance();

            if (peek() == '\'') {
                advance();
                addError(startLine, startColumn, "Символьный литерал должен содержать один символ");
            } else
                addError(startLine, startColumn, "Незакрытый символьный литерал");

            return;
        }

        advance();
        if (valid)
            addToken(TokenType.CHAR_LITERAL);
    }

    private void skipLineComment() {
        while (!isAtEnd() && peek() != '\n' && peek() != '\r')
            advance();
    }

    private void skipBlockComment() {
        int commentLine = startLine;
        int commentColumn = startColumn;

        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                advance();
                advance();
                return;
            }
            advance();
        }

        addError(commentLine, commentColumn, "Незакрытый многострочный комментарий");
    }

    private boolean isValidEscape(char c) { return c == '"' || c == '\'' || c == '\\' || c == 't' || c == 'n' || c == 'r' || c == 'f'; }
    private boolean isValidLiteralChar(char c) { return c >= 32 && c <= 126 && c != '"' && c != '\'' && c != '\\'; }
    private boolean isAlpha(char c) { return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_'; }
    private boolean isAlphaNumeric(char c) { return isAlpha(c) || isDigit(c); }
    private boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private boolean isHexDigit(char c) { return isDigit(c) || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F'; }

    private char advance() {
        char c = source.charAt(current);
        current++;

        if (c == '\n') {
            line++;
            column = 1;
        } else if (c == '\r') {
            if (current >= source.length() || source.charAt(current) != '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        } else
            column++;

        return c;
    }

    private boolean match(char expected) {
        if (isAtEnd())
            return false;
        if (source.charAt(current) != expected)
            return false;

        advance();
        return true;
    }

    private char peek() {
        if (isAtEnd())
            return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length())
            return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void addToken(TokenType type) {
        String text = source.substring(start, current);
        Token token = new Token(type, text, startLine, startColumn);
        tokens.add(token);
    }

    private void addError(int errorLine, int errorColumn, String message) {
        CompilerError error = new CompilerError(fileName, errorLine, errorColumn, message);
        errors.add(error);
    }
}