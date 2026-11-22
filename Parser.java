import java.util.ArrayList;
import java.util.List;

public class Parser {
    private static class ParseError extends RuntimeException {}

    private final String filename;
    private final List<Token> tokens;
    private final List<String> errors = new ArrayList<>();
    private int current = 0;

    public Parser(String filename, List<Token> tokens) {
        this.filename = filename;
        this.tokens = tokens;
    }

    public boolean parse() {
        parseProgram();
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    // program -> import_decl* field_decl* method_decl* EOF
    private void parseProgram() {
        while (check(TokenType.KW_IMPORT)) {
            try {
                parseImportDecl();
            } catch (ParseError e) {
                synchronizeTopLevel();
            }
        }

        while (!isAtEnd() && isFieldDeclStart()) {
            try {
                parseFieldDecl();
            } catch (ParseError e) {
                synchronizeTopLevel();
            }
        }

        while (!isAtEnd()) {
            try {
                parseMethodDecl();
            } catch (ParseError e) {
                synchronizeTopLevel();
            }
        }
    }

    // import_decl -> 'import' id ';'
    private void parseImportDecl() {
        consume(TokenType.KW_IMPORT, "Ожидалось 'import'");
        consume(TokenType.IDENTIFIER, "Ожидался идентификатор после 'import'");
        consume(TokenType.SEMICOLON, "Ожидалась ';' после объявления import");
    }

    // Отличаем field_decl от method_decl (если после type id идёт '(', это метод)
    private boolean isFieldDeclStart() {
        if (check(TokenType.KW_CONST)) return true;
        if (isTypeToken(peek().type)) {
            Token next = peekAt(1);
            Token afterNext = peekAt(2);
            if (next.type == TokenType.IDENTIFIER && afterNext.type == TokenType.LPAREN) {
                return false;
            }
            return true;
        }
        return false;
    }

    // field_decl -> 'const'? type field_item (',' field_item)* ';'
    private void parseFieldDecl() {
        match(TokenType.KW_CONST);
        parseType();
        parseFieldItem();
        while (match(TokenType.COMMA)) {
            parseFieldItem();
        }
        consume(TokenType.SEMICOLON, "Ожидалась ';' после объявления переменной");
    }

    // field_item -> id ('[' int_literal ']')? ('=' initializer)?
    private void parseFieldItem() {
        consume(TokenType.IDENTIFIER, "Ожидался идентификатор переменной");
        if (match(TokenType.LBRACKET)) {
            consume(TokenType.INTLITERAL, "Ожидался целочисленный литерал размера массива");
            consume(TokenType.RBRACKET, "Ожидалась ']'");
        }
        if (match(TokenType.ASSIGN)) {
            parseInitializer();
        }
    }

    // initializer -> literal | '{' literal (',' literal)* '}'
    private void parseInitializer() {
        if (match(TokenType.LBRACE)) {
            parseSignedLiteral();
            while (match(TokenType.COMMA)) {
                parseSignedLiteral();
            }
            consume(TokenType.RBRACE, "Ожидалась '}' в списке инициализации");
        } else {
            parseSignedLiteral();
        }
    }

    private void parseSignedLiteral() {
        if (match(TokenType.MINUS)) {
            if (!match(TokenType.INTLITERAL, TokenType.LONGLITERAL)) {
                throw error(peek(), "Ожидался числовой литерал после '-'");
            }
            return;
        }
        if (!match(TokenType.INTLITERAL, TokenType.LONGLITERAL,
                   TokenType.CHARLITERAL, TokenType.BOOLEANLITERAL)) {
            throw error(peek(), "Ожидался литерал");
        }
    }

    // method_decl -> (type | 'void') id '(' (type id (',' type id)*)? ')' block
    private void parseMethodDecl() {
        if (!match(TokenType.KW_VOID)) {
            parseType();
        }
        consume(TokenType.IDENTIFIER, "Ожидалось имя метода");
        consume(TokenType.LPAREN, "Ожидалась '(' после имени метода");
        if (!check(TokenType.RPAREN)) {
            parseType();
            consume(TokenType.IDENTIFIER, "Ожидалось имя параметра");
            while (match(TokenType.COMMA)) {
                parseType();
                consume(TokenType.IDENTIFIER, "Ожидалось имя параметра");
            }
        }
        consume(TokenType.RPAREN, "Ожидалась ')' после списка параметров");
        parseBlock();
    }

    private void parseType() {
        if (!match(TokenType.KW_INT, TokenType.KW_LONG, TokenType.KW_BOOL)) {
            throw error(peek(), "Ожидался тип (int, long или bool)");
        }
    }

    private boolean isTypeToken(TokenType type) {
        return type == TokenType.KW_INT || type == TokenType.KW_LONG || type == TokenType.KW_BOOL;
    }

    // block -> '{' field_decl* statement* '}'
    private void parseBlock() {
        consume(TokenType.LBRACE, "Ожидалась '{'");
        while (!check(TokenType.RBRACE) && !isAtEnd() && (check(TokenType.KW_CONST) || isTypeToken(peek().type))) {
            try {
                parseFieldDecl();
            } catch (ParseError e) {
                synchronizeStatement();
            }
        }
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            try {
                parseStatement();
            } catch (ParseError e) {
                synchronizeStatement();
            }
        }
        consume(TokenType.RBRACE, "Ожидалась '}'");
    }

    private void parseStatement() {
        if (match(TokenType.KW_IF)) {
            consume(TokenType.LPAREN, "Ожидалась '(' после 'if'");
            parseExpr();
            consume(TokenType.RPAREN, "Ожидалась ')' после условия 'if'");
            parseBlock();
            if (match(TokenType.KW_ELSE)) {
                parseBlock();
            }
        } else if (match(TokenType.KW_FOR)) {
            consume(TokenType.LPAREN, "Ожидалась '(' после 'for'");
            consume(TokenType.IDENTIFIER, "Ожидался идентификатор в инициализации 'for'");
            consume(TokenType.ASSIGN, "Ожидался '=' в инициализации 'for'");
            parseExpr();
            consume(TokenType.SEMICOLON, "Ожидалась ';' после инициализации 'for'");
            parseExpr();
            consume(TokenType.SEMICOLON, "Ожидалась ';' после условия 'for'");
            parseLocation();
            parseAssignExpr();
            consume(TokenType.RPAREN, "Ожидалась ')' после заголовка 'for'");
            parseBlock();
        } else if (match(TokenType.KW_WHILE)) {
            consume(TokenType.LPAREN, "Ожидалась '(' после 'while'");
            parseExpr();
            consume(TokenType.RPAREN, "Ожидалась ')' после условия 'while'");
            parseBlock();
        } else if (match(TokenType.KW_RETURN)) {
            if (!check(TokenType.SEMICOLON)) {
                parseExpr();
            }
            consume(TokenType.SEMICOLON, "Ожидалась ';' после 'return'");
        } else if (match(TokenType.KW_BREAK)) {
            consume(TokenType.SEMICOLON, "Ожидалась ';' после 'break'");
        } else if (match(TokenType.KW_CONTINUE)) {
            consume(TokenType.SEMICOLON, "Ожидалась ';' после 'continue'");
        } else if (check(TokenType.IDENTIFIER)) {
            if (peekAt(1).type == TokenType.LPAREN) {
                parseMethodCall();
                consume(TokenType.SEMICOLON, "Ожидалась ';' после вызова метода");
            } else {
                parseLocation();
                parseAssignExpr();
                consume(TokenType.SEMICOLON, "Ожидалась ';' после оператора присваивания");
            }
        } else {
            throw error(peek(), "Ожидался оператор");
        }
    }

    private void parseLocation() {
        consume(TokenType.IDENTIFIER, "Ожидался идентификатор");
        if (match(TokenType.LBRACKET)) {
            parseExpr();
            consume(TokenType.RBRACKET, "Ожидалась ']'");
        }
    }

    private void parseAssignExpr() {
        if (match(TokenType.INC, TokenType.DEC)) {
            return;
        }
        if (match(TokenType.ASSIGN, TokenType.PLUS_ASSIGN, TokenType.MINUS_ASSIGN,
                  TokenType.STAR_ASSIGN, TokenType.SLASH_ASSIGN, TokenType.PERCENT_ASSIGN)) {
            parseExpr();
            return;
        }
        throw error(peek(), "Ожидался оператор присваивания, '++' или '--'");
    }

    private void parseMethodCall() {
        consume(TokenType.IDENTIFIER, "Ожидалось имя метода");
        consume(TokenType.LPAREN, "Ожидалась '('");
        if (!check(TokenType.RPAREN)) {
            parseCallArg();
            while (match(TokenType.COMMA)) {
                parseCallArg();
            }
        }
        consume(TokenType.RPAREN, "Ожидалась ')'");
    }

    private void parseCallArg() {
        if (match(TokenType.STRINGLITERAL)) {
            return;
        }
        parseExpr();
    }

    // Выражения по приоритетам
    private void parseExpr() {
        parseOr();
    }

    private void parseOr() {
        parseAnd();
        while (match(TokenType.OR)) {
            parseAnd();
        }
    }

    private void parseAnd() {
        parseEquality();
        while (match(TokenType.AND)) {
            parseEquality();
        }
    }

    private void parseEquality() {
        parseRelational();
        while (match(TokenType.EQ, TokenType.NEQ)) {
            parseRelational();
        }
    }

    private void parseRelational() {
        parseAdditive();
        while (match(TokenType.LT, TokenType.LE, TokenType.GT, TokenType.GE)) {
            parseAdditive();
        }
    }

    private void parseAdditive() {
        parseMultiplicative();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            parseMultiplicative();
        }
    }

    private void parseMultiplicative() {
        parseUnary();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            parseUnary();
        }
    }

    private void parseUnary() {
        if (match(TokenType.MINUS, TokenType.NOT)) {
            parseUnary();
            return;
        }
        parsePrimary();
    }

    private void parsePrimary() {
        if (match(TokenType.KW_INT, TokenType.KW_LONG)) {
            consume(TokenType.LPAREN, "Ожидалась '(' при приведении типа");
            parseExpr();
            consume(TokenType.RPAREN, "Ожидалась ')' при приведении типа");
            return;
        }
        if (match(TokenType.KW_LEN)) {
            consume(TokenType.LPAREN, "Ожидалась '(' после 'len'");
            consume(TokenType.IDENTIFIER, "Ожидался идентификатор внутри 'len(...)'");
            consume(TokenType.RPAREN, "Ожидалась ')' после аргумента 'len'");
            return;
        }
        if (match(TokenType.LPAREN)) {
            parseExpr();
            consume(TokenType.RPAREN, "Ожидалась ')'");
            return;
        }
        if (match(TokenType.INTLITERAL, TokenType.LONGLITERAL,
                  TokenType.CHARLITERAL, TokenType.BOOLEANLITERAL)) {
            return;
        }
        if (check(TokenType.IDENTIFIER)) {
            if (peekAt(1).type == TokenType.LPAREN) {
                parseMethodCall();
            } else {
                parseLocation();
            }
            return;
        }
        throw error(peek(), "Ожидалось выражение");
    }

    // Вспомогательные методы и восстановление после ошибок
    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        return !isAtEnd() && peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return tokens.get(current - 1);
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token peekAt(int offset) {
        int idx = current + offset;
        if (idx >= tokens.size()) return tokens.get(tokens.size() - 1);
        return tokens.get(idx);
    }

    private ParseError error(Token token, String message) {
        String where = token.type == TokenType.EOF ? "в конце файла" : "у '" + token.lexeme + "'";
        errors.add(filename + " line " + token.line + ":" + token.column + ": " + message + " (" + where + ")");
        return new ParseError();
    }

    private void synchronizeTopLevel() {
        advance();
        while (!isAtEnd()) {
            TokenType t = peek().type;
            if (t == TokenType.KW_IMPORT || t == TokenType.KW_CONST ||
                t == TokenType.KW_VOID || isTypeToken(t)) {
                return;
            }
            advance();
        }
    }

    private void synchronizeStatement() {
        advance();
        while (!isAtEnd()) {
            if (tokens.get(current - 1).type == TokenType.SEMICOLON) return;
            TokenType t = peek().type;
            if (t == TokenType.RBRACE || t == TokenType.KW_IF || t == TokenType.KW_FOR ||
                t == TokenType.KW_WHILE || t == TokenType.KW_RETURN ||
                t == TokenType.KW_BREAK || t == TokenType.KW_CONTINUE) {
                return;
            }
            advance();
        }
    }
}
