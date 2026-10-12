public class Token {
    private final TokenType type;
    private final String text;
    private final int line;
    private final int column;

    public Token(TokenType type, String text, int line, int column) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.column = column;
    }

    public TokenType getType() { return type; }
    public String getText() { return text; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
}