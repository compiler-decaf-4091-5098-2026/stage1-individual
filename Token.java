public class Token {

    public static final String IDENTIFIER = "IDENTIFIER";
    public static final String INTLITERAL = "INTLITERAL";
    public static final String LONGLITERAL = "LONGLITERAL";
    public static final String CHARLITERAL = "CHARLITERAL";
    public static final String STRINGLITERAL = "STRINGLITERAL";
    public static final String BOOLEANLITERAL = "BOOLEANLITERAL";
    public static final String KEYWORD = "KEYWORD";
    public static final String SYMBOL = "SYMBOL";
    public static final String EOF = "EOF";

    public String type; // тип токена
    public String text; // текст токена как в исходнике
    public int line; // номер строки с единицы
    public int column; // номер столбца с единицы

    public Token(String type, String text, int line, int column) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.column = column;
    }

    // проверка что токен ключевое слово или символ с таким текстом
    public boolean is(String value) {
        if (type.equals(KEYWORD) || type.equals(SYMBOL)) {
            return text.equals(value);
        }
        return false;
    }

    // строка для вывода в режиме scan
    public String toScanLine() {
        if (type.equals(KEYWORD) || type.equals(SYMBOL)) {
            return line + " " + text;
        }
        return line + " " + type + " " + text;
    }
}
