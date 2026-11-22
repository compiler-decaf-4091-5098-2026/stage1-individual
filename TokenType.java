public enum TokenType {
    // Типы лексем, которые выводятся в таблицу -t scan
    IDENTIFIER("IDENTIFIER"),
    INTLITERAL("INTLITERAL"),
    LONGLITERAL("LONGLITERAL"),
    CHARLITERAL("CHARLITERAL"),
    STRINGLITERAL("STRINGLITERAL"),
    BOOLEANLITERAL("BOOLEANLITERAL"),

    // Ключевые слова (выводятся без имени типа)
    KW_BOOL(null),
    KW_BREAK(null),
    KW_CONST(null),
    KW_CONTINUE(null),
    KW_ELSE(null),
    KW_FOR(null),
    KW_IF(null),
    KW_IMPORT(null),
    KW_INT(null),
    KW_LEN(null),
    KW_LONG(null),
    KW_RETURN(null),
    KW_VOID(null),
    KW_WHILE(null),

    // Операторы и разделители (выводятся без имени типа)
    LPAREN(null), RPAREN(null),
    LBRACE(null), RBRACE(null),
    LBRACKET(null), RBRACKET(null),
    SEMICOLON(null), COMMA(null),

    PLUS(null), MINUS(null), STAR(null), SLASH(null), PERCENT(null),
    INC(null), DEC(null),
    ASSIGN(null), PLUS_ASSIGN(null), MINUS_ASSIGN(null),
    STAR_ASSIGN(null), SLASH_ASSIGN(null), PERCENT_ASSIGN(null),

    EQ(null), NEQ(null), LT(null), LE(null), GT(null), GE(null),
    AND(null), OR(null), NOT(null),

    EOF(null);

    private final String scanLabel;

    TokenType(String scanLabel) {
        this.scanLabel = scanLabel;
    }

    public String getScanLabel() {
        return scanLabel;
    }
}
