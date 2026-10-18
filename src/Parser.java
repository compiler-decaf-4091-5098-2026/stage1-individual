import java.util.List;

public final class Parser {
    private final List<Token> tokens;
    private int i = 0;

    public Parser(List<Token> tokens) { this.tokens = tokens; }

    private Token cur() { return tokens.get(i); }
    private boolean check(TokenType t) { return cur().type == t; }
    private boolean match(TokenType t) { if (check(t)) { i++; return true; } return false; }

    private Token expect(TokenType t, String what) {
        if (check(t)) { Token r = cur(); i++; return r; }
        throw new ParseException("ожидается " + what + ", получено '" + cur().text + "' (" + cur().type + ") в " + cur().line + ":" + cur().column);
    }

    public void parse() {
        while (!check(TokenType.EOF)) {
            declaration();
        }
        expect(TokenType.EOF, "конец файла");
    }

    private void declaration() {
        if (match(TokenType.EXTERN)) {
            type();
            expect(TokenType.IDENTIFIER, "имя функции");
            expect(TokenType.LPAREN, "(");
            params();
            expect(TokenType.RPAREN, ")");
            expect(TokenType.SEMI, ";");
            return;
        }
        if (check(TokenType.CLASS)) { classDecl(); return; }
        type();
        expect(TokenType.IDENTIFIER, "имя функции");
        expect(TokenType.LPAREN, "(");
        params();
        expect(TokenType.RPAREN, ")");
        block();
    }

    private void classDecl() {
        expect(TokenType.CLASS, "class");
        expect(TokenType.IDENTIFIER, "имя класса");
        expect(TokenType.LBRACE, "{");
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            if (check(TokenType.EXTERN)) { declaration(); continue; }
            type();
            expect(TokenType.IDENTIFIER, "имя поля");
            if (match(TokenType.LPAREN)) {
                params();
                expect(TokenType.RPAREN, ")");
                block();
            } else {
                expect(TokenType.SEMI, ";");
            }
        }
        expect(TokenType.RBRACE, "}");
        match(TokenType.SEMI);
    }

    private void type() {
        if (match(TokenType.INT) || match(TokenType.LONG) || match(TokenType.BOOLEAN) || match(TokenType.VOID)) return;
        throw new ParseException("ожидается тип, получено '" + cur().text + "' в " + cur().line + ":" + cur().column);
    }

    private void params() {
        if (check(TokenType.RPAREN)) return;
        param();
        while (match(TokenType.COMMA)) param();
    }

    private void param() {
        if (match(TokenType.VOID)) return;
        type();
        expect(TokenType.IDENTIFIER, "имя параметра");
        if (match(TokenType.LBRACKET)) expect(TokenType.RBRACKET, "]");
    }

    private void block() {
        expect(TokenType.LBRACE, "{");
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) statement();
        expect(TokenType.RBRACE, "}");
    }

    private void statement() {
        if (check(TokenType.LBRACE)) { block(); return; }
        if (match(TokenType.IF)) {
            expect(TokenType.LPAREN, "("); expr(); expect(TokenType.RPAREN, ")");
            statement();
            if (match(TokenType.ELSE)) statement();
            return;
        }
        if (match(TokenType.WHILE)) {
            expect(TokenType.LPAREN, "("); expr(); expect(TokenType.RPAREN, ")");
            statement();
            return;
        }
        if (match(TokenType.FOR)) {
            expect(TokenType.LPAREN, "(");
            if (!check(TokenType.SEMI)) { expr(); }
            expect(TokenType.SEMI, ";");
            if (!check(TokenType.SEMI)) { expr(); }
            expect(TokenType.SEMI, ";");
            if (!check(TokenType.RPAREN)) { expr(); }
            expect(TokenType.RPAREN, ")");
            statement();
            return;
        }
        if (match(TokenType.RETURN)) {
            if (!check(TokenType.SEMI)) expr();
            expect(TokenType.SEMI, ";");
            return;
        }
        if (match(TokenType.BREAK)) { expect(TokenType.SEMI, ";"); return; }
        if (match(TokenType.CONTINUE)) { expect(TokenType.SEMI, ";"); return; }

        if (check(TokenType.INT) || check(TokenType.LONG) || check(TokenType.BOOLEAN) || check(TokenType.VOID)) {
            type();
            expect(TokenType.IDENTIFIER, "имя переменной");
            if (match(TokenType.LBRACKET)) expect(TokenType.RBRACKET, "]");
            if (match(TokenType.ASSIGN)) expr();
            expect(TokenType.SEMI, ";");
            return;
        }
        expr();
        expect(TokenType.SEMI, ";");
    }

    private void expr() { logicalOr(); }

    private void logicalOr() {
        logicalAnd();
        while (match(TokenType.OR)) logicalAnd();
    }
    private void logicalAnd() {
        equality();
        while (match(TokenType.AND)) equality();
    }
    private void equality() {
        relational();
        while (check(TokenType.EQ) || check(TokenType.NE)) { i++; relational(); }
    }
    private void relational() {
        additive();
        while (check(TokenType.LT) || check(TokenType.GT) || check(TokenType.LE) || check(TokenType.GE)) { i++; additive(); }
    }
    private void additive() {
        multiplicative();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) { i++; multiplicative(); }
    }
    private void multiplicative() {
        unary();
        while (check(TokenType.STAR) || check(TokenType.SLASH) || check(TokenType.PERCENT)) { i++; unary(); }
    }
    private void unary() {
        if (check(TokenType.MINUS) || check(TokenType.NOT) || check(TokenType.INCREMENT) || check(TokenType.DECREMENT)) {
            i++; unary(); return;
        }
        postfix();
    }
    private void postfix() {
        primary();
        while (true) {
            if (match(TokenType.LPAREN)) {
                if (!check(TokenType.RPAREN)) { expr(); while (match(TokenType.COMMA)) expr(); }
                expect(TokenType.RPAREN, ")");
            } else if (match(TokenType.LBRACKET)) {
                expr();
                expect(TokenType.RBRACKET, "]");
            } else if (match(TokenType.DOT)) {
                expect(TokenType.IDENTIFIER, "имя поля");
            } else if (check(TokenType.INCREMENT) || check(TokenType.DECREMENT)) {
                i++;
            } else break;
        }
    }
    private void primary() {
        Token t = cur();
        switch (t.type) {
            case INTLITERAL: case LONGLITERAL:
            case CHARLITERAL: case STRINGLITERAL: case BOOLEANLITERAL:
            case IDENTIFIER:
                i++; return;
            case LPAREN:
                i++; expr(); expect(TokenType.RPAREN, ")"); return;
            default:
                throw new ParseException("неожиданный токен '" + t.text + "' (" + t.type + ") в " + t.line + ":" + t.column);
        }
    }
}