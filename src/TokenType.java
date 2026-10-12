public enum TokenType {
    // Идентификаторы и литералы.
    IDENTIFIER,
    INT_LITERAL,
    LONG_LITERAL,
    CHAR_LITERAL,
    STRING_LITERAL,
    BOOLEAN_LITERAL,

    // Ключевые слова.

    BOOL,
    BREAK,
    CONTINUE,
    ELSE,
    FOR,
    IF,
    IMPORT,
    INT,
    LEN,
    LONG,
    RETURN,
    VOID,
    WHILE,

    // Арифметические операторы.

    PLUS,
    MINUS,
    MULTIPLY,
    DIVIDE,
    MODULO,

    // Операторы присваивания.

    ASSIGN,
    PLUS_ASSIGN,
    MINUS_ASSIGN,
    MULTIPLY_ASSIGN,
    DIVIDE_ASSIGN,
    MODULO_ASSIGN,

    // Инкремент и декремент.

    INCREMENT,
    DECREMENT,

    // Операторы сравнения.

    LESS,
    GREATER,
    LESS_EQUAL,
    GREATER_EQUAL,
    EQUAL,
    NOT_EQUAL,

    // Логические операторы.

    AND,
    OR,
    NOT,

    // Скобки и разделители.

    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,
    LEFT_BRACKET,
    RIGHT_BRACKET,
    SEMICOLON,
    COMMA,

    // Конец входного файла.

    EOF
}