package decaf;

public record Token(String kind, String text, int line, int column) {
    public String listing() {
        if (kind.equals("IDENTIFIER") || kind.endsWith("LITERAL")) {
            return line + " " + kind + " " + text;
        }
        return line + " " + text;
    }
}
