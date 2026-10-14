import java.util.ArrayList;
import java.util.List;

// Выполняет синтаксический анализ Decaf методом рекурсивного спуска.

public class Parser {
    private final List<Token> tokens;
    private final String fileName;
    private final List<CompilerError> errors = new ArrayList<>();

    private int current = 0;

    public Parser(List<Token> tokens, String fileName) {
        this.tokens = tokens;
        this.fileName = fileName;
    }

    // Запускает синтаксический разбор и определяет его результат.

    public boolean parse() {
        try {
            program();
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    // Возвращает список обнаруженных синтаксических ошибок.

    public List<CompilerError> getErrors() { return errors; }


    // Разбирает структуру программы: импорты, переменные и методы.

    private void program() {
        while (match(TokenType.IMPORT))
            importDeclaration();
        while (isType(peek().getType()) && !isMethodDeclaration())
            fieldDeclaration();
        while (isType(peek().getType()) || check(TokenType.VOID))
            methodDeclaration();

        consume(TokenType.EOF, "Ожидался конец файла");
    }

    // Разбирает объявление импортируемого метода.

    private void importDeclaration() {
        consume(TokenType.IDENTIFIER, "Ожидалось имя импортируемого метода");
        consume(TokenType.SEMICOLON, "Ожидался символ ';'");
    }

    // Разбирает объявление переменных и массивов.

    private void fieldDeclaration() {
        parseType();
        field();
        while (match(TokenType.COMMA))
            field();
        consume(TokenType.SEMICOLON, "Ожидался символ ';'");
    }

    // Проверяет имя переменной и необязательный размер массива.

    private void field() {
        consume(TokenType.IDENTIFIER, "Ожидалось имя переменной");
        if (match(TokenType.LEFT_BRACKET)) {
            consume(TokenType.INT_LITERAL, "Ожидался размер массива");
            consume(TokenType.RIGHT_BRACKET, "Ожидался символ ']'");
        }
    }

    // Разбирает объявление метода: тип, имя, параметры и тело.

    private void methodDeclaration() {
        if (!match(TokenType.VOID))
            parseType();

        consume(TokenType.IDENTIFIER, "Ожидалось имя метода");
        consume(TokenType.LEFT_PAREN, "Ожидался символ '('");

        if (!check(TokenType.RIGHT_PAREN)) {
            parameter();
            while (match(TokenType.COMMA))
                parameter();
        }

        consume(TokenType.RIGHT_PAREN, "Ожидался символ ')'");
        block();
    }

    // Проверяет тип и имя параметра метода.

    private void parameter() {
        parseType();
        consume(TokenType.IDENTIFIER, "Ожидалось имя параметра");
    }

    // Разбирает блок с локальными переменными и операторами.

    private void block() {
        consume(TokenType.LEFT_BRACE, "Ожидался символ '{'");

        while (isType(peek().getType()))
            fieldDeclaration();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd())
            statement();

        consume(TokenType.RIGHT_BRACE, "Ожидался символ '}'");
    }

    // Определяет вид оператора и запускает соответствующий разбор.

    private void statement() {
        if (match(TokenType.IF)) {
            ifStatement();
            return;
        }
        if (match(TokenType.FOR)) {
            forStatement();
            return;
        }
        if (match(TokenType.WHILE)) {
            whileStatement();
            return;
        }
        if (match(TokenType.RETURN)) {
            returnStatement();
            return;
        }
        if (match(TokenType.BREAK)) {
            consume(TokenType.SEMICOLON, "Ожидался символ ';'");
            return;
        }
        if (match(TokenType.CONTINUE)) {
            consume(TokenType.SEMICOLON, "Ожидался символ ';'");
            return;
        }

        if (check(TokenType.IDENTIFIER)) {
            if (checkNext(TokenType.LEFT_PAREN)) {
                methodCall();
                consume(TokenType.SEMICOLON, "Ожидался символ ';'");
            } else {
                location();
                assignExpression();
                consume(TokenType.SEMICOLON, "Ожидался символ ';'");
            }
            return;
        }

        throw error(peek(), "Ожидался оператор");
    }

    // Разбирает условный оператор if и необязательную ветку else.

    private void ifStatement() {
        consume(TokenType.LEFT_PAREN, "Ожидался символ '('");
        expression();
        consume(TokenType.RIGHT_PAREN, "Ожидался символ ')'");
        block();
        if (match(TokenType.ELSE))
            block();
    }

    // Разбирает цикл for: начальное присваивание, условие, изменение и тело.

    private void forStatement() {
        consume(TokenType.LEFT_PAREN, "Ожидался символ '('");
        consume(TokenType.IDENTIFIER, "Ожидался идентификатор");
        consume(TokenType.ASSIGN, "Ожидался символ '='");
        expression();
        consume(TokenType.SEMICOLON, "Ожидался символ ';'");
        expression();
        consume(TokenType.SEMICOLON, "Ожидался символ ';'");
        location();
        assignExpression();
        consume(TokenType.RIGHT_PAREN, "Ожидался символ ')'");
        block();
    }

    // Разбирает цикл while с условием и телом.

    private void whileStatement() {
        consume(TokenType.LEFT_PAREN, "Ожидался символ '('");
        expression();
        consume(TokenType.RIGHT_PAREN, "Ожидался символ ')'");
        block();
    }

    // Разбирает оператор return с необязательным выражением.

    private void returnStatement() {
        if (!check(TokenType.SEMICOLON))
            expression();

        consume(TokenType.SEMICOLON, "Ожидался символ ';'");
    }

    // Проверяет операторы присваивания, увеличения и уменьшения.

    private void assignExpression() {
        if (match(TokenType.ASSIGN) || match(TokenType.PLUS_ASSIGN) || match(TokenType.MINUS_ASSIGN) || match(TokenType.MULTIPLY_ASSIGN) || match(TokenType.DIVIDE_ASSIGN) || match(TokenType.MODULO_ASSIGN)) {
            expression();
            return;
        }
        if (match(TokenType.INCREMENT) || match(TokenType.DECREMENT))
            return;

        throw error(peek(), "Ожидался оператор присваивания");
    }

    // Разбирает вызов метода и передаваемые аргументы.

    private void methodCall() {
        consume(TokenType.IDENTIFIER, "Ожидалось имя метода");
        consume(TokenType.LEFT_PAREN, "Ожидался символ '('");

        if (!check(TokenType.RIGHT_PAREN)) {
            argument();
            while (match(TokenType.COMMA))
                argument();
        }

        consume(TokenType.RIGHT_PAREN, "Ожидался символ ')'");
    }

    // Разбирает аргумент вызова метода.

    private void argument() {
        if (match(TokenType.STRING_LITERAL))
            return;
        expression();
    }

    // Разбирает обращение к переменной или элементу массива.

    private void location() {
        consume(TokenType.IDENTIFIER, "Ожидался идентификатор");
        if (match(TokenType.LEFT_BRACKET)) {
            expression();
            consume(TokenType.RIGHT_BRACKET, "Ожидался символ ']'");
        }
    }


    // Запускает разбор выражения с учётом приоритета операций.

    private void expression() {
        orExpression();
    }
    private void orExpression() {
        andExpression();
        while (match(TokenType.OR))
            andExpression();
    }
    private void andExpression() {
        equalityExpression();
        while (match(TokenType.AND))
            equalityExpression();
    }
    private void equalityExpression() {
        relationalExpression();
        while (match(TokenType.EQUAL) || match(TokenType.NOT_EQUAL))
            relationalExpression();
    }
    private void relationalExpression() {
        additiveExpression();
        while (match(TokenType.LESS) || match(TokenType.GREATER) || match(TokenType.LESS_EQUAL) || match(TokenType.GREATER_EQUAL))
            additiveExpression();
    }
    private void additiveExpression() {
        multiplicativeExpression();
        while (match(TokenType.PLUS) || match(TokenType.MINUS))
            multiplicativeExpression();
    }
    private void multiplicativeExpression() {
        unaryExpression();
        while (match(TokenType.MULTIPLY) || match(TokenType.DIVIDE) || match(TokenType.MODULO))
            unaryExpression();
    }

    // Разбирает унарные операции, приведение типов и функцию len.

    private void unaryExpression() {
        if (match(TokenType.MINUS) || match(TokenType.NOT)) {
            unaryExpression();
            return;
        }

        if ((check(TokenType.INT) || check(TokenType.LONG)) && checkNext(TokenType.LEFT_PAREN)) {
            advance();
            consume(TokenType.LEFT_PAREN, "Ожидался символ '('");
            expression();
            consume(TokenType.RIGHT_PAREN, "Ожидался символ ')'");
            return;
        }
        if (match(TokenType.LEN)) {
            consume(TokenType.LEFT_PAREN, "Ожидался символ '('");
            consume(TokenType.IDENTIFIER, "Ожидался идентификатор");
            consume(TokenType.RIGHT_PAREN, "Ожидался символ ')'");
            return;
        }

        primaryExpression();
    }

    // Разбирает литералы, переменные, вызовы методов и выражения в скобках.

    private void primaryExpression() {
        if (match(TokenType.INT_LITERAL) || match(TokenType.LONG_LITERAL) || match(TokenType.CHAR_LITERAL) || match(TokenType.BOOLEAN_LITERAL))
            return;
        if (match(TokenType.LEFT_PAREN)) {
            expression();
            consume(TokenType.RIGHT_PAREN, "Ожидался символ ')'");
            return;
        }
        if (check(TokenType.IDENTIFIER)) {
            if (checkNext(TokenType.LEFT_PAREN)) {
                methodCall();
            } else {
                location();
            }
            return;
        }

        throw error(peek(), "Ожидалось выражение");
    }


    // Проверяет допустимые типы данных: int, long и bool.

    private void parseType() {
        if (match(TokenType.INT) || match(TokenType.LONG) || match(TokenType.BOOL))
            return;
        throw error(peek(), "Ожидался тип данных");
    }

    private boolean isType(TokenType type) { return type == TokenType.INT || type == TokenType.LONG || type == TokenType.BOOL; }
    private boolean isMethodDeclaration() { return isType(peek().getType()) && checkAt(1, TokenType.IDENTIFIER) && checkAt(2, TokenType.LEFT_PAREN); }
    private boolean match(TokenType type) {
        if (!check(type))
            return false;
        advance();
        return true;
    }

    // Проверяет ожидаемый токен и сообщает об ошибке при несовпадении.

    private Token consume(TokenType type, String message) {
        if (check(type))
            return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) { return peek().getType() == type; }
    private boolean checkNext(TokenType type) { return checkAt(1, type); }
    private boolean checkAt(int offset, TokenType type) {
        int index = current + offset;
        if (index >= tokens.size())
            return false;
        return tokens.get(index).getType() == type;
    }

    private Token advance() {
        Token token = peek();
        if (!isAtEnd())
            current++;
        return token;
    }

    private Token peek() { return tokens.get(current); }
    private boolean isAtEnd() { return peek().getType() == TokenType.EOF; }


    // Сохраняет синтаксическую ошибку с указанием строки и столбца.

    private ParseException error(Token token, String message) {
        errors.add(new CompilerError(fileName, token.getLine(), token.getColumn(), message));
        return new ParseException();
    }

    private static class ParseException extends RuntimeException { }
}