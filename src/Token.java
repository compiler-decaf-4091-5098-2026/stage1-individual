public class Token {
    public final TokenType type;
    public final String text;
    public final int line;
    public final int col;

    public Token(TokenType type, String text, int line, int col) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.col = col;
    }
}