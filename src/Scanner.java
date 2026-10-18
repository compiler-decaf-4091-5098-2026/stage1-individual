import java.util.*;

public class Scanner {
    private final String src;
    private final String filename;
    private int pos = 0;
    private int line = 1;
    private int col = 1;

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("if", TokenType.IF),
            Map.entry("bool", TokenType.BOOL),
            Map.entry("break", TokenType.BREAK),
            Map.entry("import", TokenType.IMPORT),
            Map.entry("continue", TokenType.CONTINUE),
            Map.entry("else", TokenType.ELSE),
            Map.entry("false", TokenType.FALSE),
            Map.entry("for", TokenType.FOR),
            Map.entry("while", TokenType.WHILE),
            Map.entry("int", TokenType.INT),
            Map.entry("long", TokenType.LONG),
            Map.entry("return", TokenType.RETURN),
            Map.entry("len", TokenType.LEN),
            Map.entry("true", TokenType.TRUE),
            Map.entry("void", TokenType.VOID)
    );

    public Scanner(String src, String filename) {
        this.src = src;
        this.filename = filename;
    }

    private char peek() {
        return pos < src.length() ? src.charAt(pos) : '\0';
    }

    private char peek(int k) {
        int p = pos + k;
        return p < src.length() ? src.charAt(p) : '\0';
    }

    private char advance() {
        char c = src.charAt(pos++);
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    private void error(int l, int c, String msg) {
        System.err.println(new CompilerError(filename, l, c, msg));
    }

    public List<Token> scanAll() {
        List<Token> out = new ArrayList<>();
        while (true) {
            skipWhitespaceAndComments();
            if (pos >= src.length()) {
                out.add(new Token(TokenType.EOF, "", line, col));
                return out;
            }

            int l = line;
            int c = col;
            char ch = peek();

            if (Character.isLetter(ch) || ch == '_') {
                out.add(scanIdentifierOrKeyword(l, c));
            } else if (Character.isDigit(ch)) {
                out.add(scanNumber(l, c));
            } else if (ch == '"') {
                out.add(scanString(l, c));
            } else if (ch == '\'') {
                out.add(scanChar(l, c));
            } else {
                Token t = scanPunctOrOperator(l, c);
                if (t != null) {
                    out.add(t);
                }
            }
        }
    }

    private void skipWhitespaceAndComments() {
        while (pos < src.length()) {
            char ch = peek();
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' || ch == '\f') {
                advance();
            } else if (ch == '/' && peek(1) == '/') {
                while (pos < src.length() && peek() != '\n') {
                    advance();
                }
            } else if (ch == '/' && peek(1) == '*') {
                int l = line;
                int c = col;
                advance();
                advance();
                boolean closed = false;
                while (pos < src.length()) {
                    if (peek() == '*' && peek(1) == '/') {
                        advance();
                        advance();
                        closed = true;
                        break;
                    }
                    advance();
                }
                if (!closed) {
                    error(l, c, "Незакрытый комментарий");
                }
            } else {
                break;
            }
        }
    }

    private Token scanIdentifierOrKeyword(int l, int c) {
        StringBuilder sb = new StringBuilder();
        while (pos < src.length()) {
            char ch = peek();
            if (Character.isLetterOrDigit(ch) || ch == '_') {
                sb.append(advance());
            } else {
                break;
            }
        }
        String text = sb.toString();
        TokenType kw = KEYWORDS.get(text);
        return new Token(kw != null ? kw : TokenType.IDENTIFIER, text, l, c);
    }

    private Token scanNumber(int l, int c) {
        StringBuilder sb = new StringBuilder();
        boolean isLong = false;

        if (peek() == '0' && (peek(1) == 'x' || peek(1) == 'X')) {
            sb.append(advance());
            sb.append(advance());
            while (pos < src.length() && isHexDigit(peek())) {
                sb.append(advance());
            }
        } else {
            while (pos < src.length() && Character.isDigit(peek())) {
                sb.append(advance());
            }
        }

        if (pos < src.length() && peek() == 'L') {
            isLong = true;
            sb.append(advance());
        }

        TokenType type = isLong ? TokenType.LONGLITERAL : TokenType.INTLITERAL;
        return new Token(type, sb.toString(), l, c);
    }

    private boolean isHexDigit(char ch) {
        return (ch >= '0' && ch <= '9')
                || (ch >= 'a' && ch <= 'f')
                || (ch >= 'A' && ch <= 'F');
    }

    private Token scanString(int l, int c) {
        StringBuilder sb = new StringBuilder();
        sb.append(advance());
        boolean closed = false;
        while (pos < src.length()) {
            char ch = peek();
            if (ch == '\n' || ch == '\r') {
                break;
            }
            if (ch == '\\') {
                sb.append(advance());
                if (pos < src.length()) {
                    sb.append(advance());
                }
            } else if (ch == '"') {
                sb.append(advance());
                closed = true;
                break;
            } else {
                sb.append(advance());
            }
        }
        if (!closed) {
            error(l, c, "Незакрытый строковый литерал");
            return new Token(TokenType.ERROR, sb.toString(), l, c);
        }
        return new Token(TokenType.STRINGLITERAL, sb.toString(), l, c);
    }

    private Token scanChar(int l, int c) {
        StringBuilder sb = new StringBuilder();
        sb.append(advance());
        boolean closed = false;
        while (pos < src.length()) {
            char ch = peek();
            if (ch == '\n' || ch == '\r') {
                break;
            }
            if (ch == '\\') {
                sb.append(advance());
                if (pos < src.length()) {
                    sb.append(advance());
                }
            } else if (ch == '\'') {
                sb.append(advance());
                closed = true;
                break;
            } else {
                sb.append(advance());
            }
        }
        if (!closed) {
            error(l, c, "Незакрытый символьный литерал");
            return new Token(TokenType.ERROR, sb.toString(), l, c);
        }
        return new Token(TokenType.CHARLITERAL, sb.toString(), l, c);
    }

    private Token scanPunctOrOperator(int l, int c) {
        char ch = advance();
        switch (ch) {
            case '(':
                return new Token(TokenType.LPAREN, "(", l, c);
            case ')':
                return new Token(TokenType.RPAREN, ")", l, c);
            case '{':
                return new Token(TokenType.LBRACE, "{", l, c);
            case '}':
                return new Token(TokenType.RBRACE, "}", l, c);
            case '[':
                return new Token(TokenType.LBRACKET, "[", l, c);
            case ']':
                return new Token(TokenType.RBRACKET, "]", l, c);
            case ',':
                return new Token(TokenType.COMMA, ",", l, c);
            case ';':
                return new Token(TokenType.SEMICOLON, ";", l, c);
            case '+':
                if (peek() == '+') {
                    advance();
                    return new Token(TokenType.INCREMENT, "++", l, c);
                }
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.PLUS_ASSIGN, "+=", l, c);
                }
                return new Token(TokenType.PLUS, "+", l, c);
            case '-':
                if (peek() == '-') {
                    advance();
                    return new Token(TokenType.DECREMENT, "--", l, c);
                }
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.MINUS_ASSIGN, "-=", l, c);
                }
                return new Token(TokenType.MINUS, "-", l, c);
            case '*':
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.STAR_ASSIGN, "*=", l, c);
                }
                return new Token(TokenType.STAR, "*", l, c);
            case '%':
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.PERCENT_ASSIGN, "%=", l, c);
                }
                return new Token(TokenType.PERCENT, "%", l, c);
            case '/':
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.SLASH_ASSIGN, "/=", l, c);
                }
                return new Token(TokenType.SLASH, "/", l, c);
            case '=':
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.EQ, "==", l, c);
                }
                return new Token(TokenType.ASSIGN, "=", l, c);
            case '!':
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.NE, "!=", l, c);
                }
                return new Token(TokenType.NOT, "!", l, c);
            case '<':
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.LE, "<=", l, c);
                }
                return new Token(TokenType.LT, "<", l, c);
            case '>':
                if (peek() == '=') {
                    advance();
                    return new Token(TokenType.GE, ">=", l, c);
                }
                return new Token(TokenType.GT, ">", l, c);
            case '&':
                if (peek() == '&') {
                    advance();
                    return new Token(TokenType.AND, "&&", l, c);
                }
                error(l, c, "Недопустимый символ '&'");
                return new Token(TokenType.ERROR, "&", l, c);
            case '|':
                if (peek() == '|') {
                    advance();
                    return new Token(TokenType.OR, "||", l, c);
                }
                error(l, c, "Недопустимый символ '|'");
                return new Token(TokenType.ERROR, "|", l, c);
            default:
                error(l, c, "Недопустимый символ '" + ch + "'");
                return new Token(TokenType.ERROR, String.valueOf(ch), l, c);
        }
    }
}