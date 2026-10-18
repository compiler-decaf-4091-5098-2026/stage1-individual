import java.util.List;

public class Parser {
    private final List<Token> toks;
    private int i = 0;
    private final String file;
    private boolean hadError = false;

    public Parser(List<Token> toks, String file) {
        this.toks = toks;
        this.file = file;
    }

    private Token cur() {
        return toks.get(i);
    }

    private boolean check(TokenType t) {
        return cur().type == t;
    }

    private Token match(TokenType t) {
        return check(t) ? toks.get(i++) : null;
    }

    private void expect(TokenType t, String what) {
        if (!check(t)) {
            error("ожидалось " + what + ", получено '" + cur().text + "'");
        } else {
            i++;
        }
    }

    private void error(String msg) {
        hadError = true;
        System.err.println(new CompilerError(file, cur().line, cur().col, msg));
        while (!check(TokenType.SEMICOLON) && !check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            i++;
        }
        if (check(TokenType.SEMICOLON) || check(TokenType.RBRACE)) {
            i++;
        }
    }

    public boolean parse() {
        program();
        if (!check(TokenType.EOF)) {
            error("лишние токены в конце файла");
        }
        return !hadError;
    }

    private void program() {
        while (!check(TokenType.EOF)) {
            if (check(TokenType.IMPORT)) {
                importDecl();
            } else if (isTypeStart()) {
                if (isMethodDecl()) {
                    methodDecl();
                } else {
                    fieldDecl();
                }
            } else {
                error("ожидалось объявление import, поля или метода");
            }
        }
    }

    private void importDecl() {
        expect(TokenType.IMPORT, "'import'");
        expect(TokenType.IDENTIFIER, "идентификатор");
        expect(TokenType.SEMICOLON, "';'");
    }

    private boolean isTypeStart() {
        return check(TokenType.INT) || check(TokenType.LONG) || check(TokenType.BOOL) || check(TokenType.VOID);
    }

    private boolean isMethodDecl() {
        int temp = i;
        if (check(TokenType.VOID)) {
            i++;
            boolean res = check(TokenType.IDENTIFIER) && i + 1 < toks.size() && toks.get(i+1).type == TokenType.LPAREN;
            i = temp;
            return res;
        }
        if (isTypeStart()) {
            i++;
            boolean res = check(TokenType.IDENTIFIER) && i + 1 < toks.size() && toks.get(i+1).type == TokenType.LPAREN;
            i = temp;
            return res;
        }
        return false;
    }

    private void fieldDecl() {
        type();
        expect(TokenType.IDENTIFIER, "идентификатор");
        if (check(TokenType.LBRACKET)) {
            expect(TokenType.LBRACKET, "'['");
            expect(TokenType.RBRACKET, "']'");
        }
        while (match(TokenType.COMMA) != null) {
            expect(TokenType.IDENTIFIER, "идентификатор");
            if (check(TokenType.LBRACKET)) {
                expect(TokenType.LBRACKET, "'['");
                expect(TokenType.RBRACKET, "']'");
            }
        }
        expect(TokenType.SEMICOLON, "';'");
    }

    private void methodDecl() {
        if (!check(TokenType.VOID)) {
            type();
        } else {
            match(TokenType.VOID);
        }

        expect(TokenType.IDENTIFIER, "идентификатор метода");
        expect(TokenType.LPAREN, "'('");
        if (!check(TokenType.RPAREN)) {
            param();
            while (match(TokenType.COMMA) != null) {
                param();
            }
        }
        expect(TokenType.RPAREN, "')'");
        block();
    }

    private void type() {
        if (match(TokenType.INT) != null) return;
        if (match(TokenType.LONG) != null) return;
        if (match(TokenType.BOOL) != null) return;
        error("ожидался тип (int/long/bool)");
    }

    private void param() {
        type();
        expect(TokenType.IDENTIFIER, "идентификатор параметра");
        if (check(TokenType.LBRACKET)) {
            expect(TokenType.LBRACKET, "'['");
            expect(TokenType.RBRACKET, "']'");
        }
    }

    private void block() {
        expect(TokenType.LBRACE, "'{'");
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            if (isTypeStart()) {
                fieldDecl();
            } else {
                statement();
            }
        }
        expect(TokenType.RBRACE, "'}'");
    }

    private void statement() {
        if (check(TokenType.LBRACE)) {
            block();
            return;
        }
        if (match(TokenType.IF) != null) {
            expect(TokenType.LPAREN, "'('");
            expr();
            expect(TokenType.RPAREN, "')'");
            block();
            if (match(TokenType.ELSE) != null) {
                block();
            }
            return;
        }
        if (match(TokenType.WHILE) != null) {
            expect(TokenType.LPAREN, "'('");
            expr();
            expect(TokenType.RPAREN, "')'");
            block();
            return;
        }
        if (match(TokenType.FOR) != null) {
            expect(TokenType.LPAREN, "'('");
            expect(TokenType.IDENTIFIER, "идентификатор");
            expect(TokenType.ASSIGN, "'='");
            expr();
            expect(TokenType.SEMICOLON, "';'");
            expr();
            expect(TokenType.SEMICOLON, "';'");
            expect(TokenType.IDENTIFIER, "идентификатор");
            if (match(TokenType.ASSIGN) == null && match(TokenType.PLUS_ASSIGN) == null && match(TokenType.MINUS_ASSIGN) == null) {
                if (match(TokenType.INCREMENT) == null && match(TokenType.DECREMENT) == null) {
                    error("ожидалось присваивание в for");
                }
            } else {
                expr();
            }
            expect(TokenType.RPAREN, "')'");
            block();
            return;
        }
        if (match(TokenType.RETURN) != null) {
            if (!check(TokenType.SEMICOLON)) {
                expr();
            }
            expect(TokenType.SEMICOLON, "';'");
            return;
        }
        if (match(TokenType.BREAK) != null) {
            expect(TokenType.SEMICOLON, "';'");
            return;
        }
        if (match(TokenType.CONTINUE) != null) {
            expect(TokenType.SEMICOLON, "';'");
            return;
        }

        if (check(TokenType.IDENTIFIER)) {
            i++;
            if (check(TokenType.LBRACKET)) {
                expect(TokenType.LBRACKET, "'['");
                expr();
                expect(TokenType.RBRACKET, "']'");
            }
            if (match(TokenType.ASSIGN) != null || match(TokenType.PLUS_ASSIGN) != null || match(TokenType.MINUS_ASSIGN) != null
                    || match(TokenType.STAR_ASSIGN) != null || match(TokenType.SLASH_ASSIGN) != null || match(TokenType.PERCENT_ASSIGN) != null) {
                expr();
                expect(TokenType.SEMICOLON, "';'");
                return;
            }
            if (match(TokenType.INCREMENT) != null || match(TokenType.DECREMENT) != null) {
                expect(TokenType.SEMICOLON, "';'");
                return;
            }
            if (check(TokenType.LPAREN)) {
                expect(TokenType.LPAREN, "'('");
                if (!check(TokenType.RPAREN)) {
                    expr();
                    while (match(TokenType.COMMA) != null) {
                        expr();
                    }
                }
                expect(TokenType.RPAREN, "')'");
                expect(TokenType.SEMICOLON, "';'");
                return;
            }
            error("ожидалось '=', '(' или ';'");
            return;
        }
        error("ожидался оператор");
    }

    private void expr() {
        lor();
    }

    private void lor() {
        land();
        while (match(TokenType.OR) != null) {
            land();
        }
    }

    private void land() {
        equality();
        while (match(TokenType.AND) != null) {
            equality();
        }
    }

    private void equality() {
        relational();
        while (check(TokenType.EQ) || check(TokenType.NE)) {
            i++;
            relational();
        }
    }

    private void relational() {
        additive();
        while (check(TokenType.LT) || check(TokenType.GT) || check(TokenType.LE) || check(TokenType.GE)) {
            i++;
            additive();
        }
    }

    private void additive() {
        multiplicative();
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            i++;
            multiplicative();
        }
    }

    private void multiplicative() {
        unary();
        while (check(TokenType.STAR) || check(TokenType.SLASH) || check(TokenType.PERCENT)) {
            i++;
            unary();
        }
    }

    private void unary() {
        if (match(TokenType.MINUS) != null) { unary(); return; }
        if (match(TokenType.NOT) != null)   { unary(); return; }
        if (check(TokenType.INT) && i + 1 < toks.size() && toks.get(i+1).type == TokenType.LPAREN) {
            i += 2;
            expr();
            expect(TokenType.RPAREN, "')'");
            return;
        }
        if (check(TokenType.LONG) && i + 1 < toks.size() && toks.get(i+1).type == TokenType.LPAREN) {
            i += 2;
            expr();
            expect(TokenType.RPAREN, "')'");
            return;
        }
        if (match(TokenType.LEN) != null) {
            expect(TokenType.LPAREN, "'('");
            expect(TokenType.IDENTIFIER, "идентификатор");
            expect(TokenType.RPAREN, "')'");
            return;
        }
        primary();
    }

    private void primary() {
        if (check(TokenType.INTLITERAL) || check(TokenType.LONGLITERAL) || check(TokenType.CHARLITERAL) || check(TokenType.BOOLEANLITERAL) || check(TokenType.STRINGLITERAL)) {
            i++;
            return;
        }
        if (match(TokenType.LPAREN) != null) {
            expr();
            expect(TokenType.RPAREN, "')'");
            return;
        }
        if (check(TokenType.IDENTIFIER)) {
            i++;
            if (check(TokenType.LBRACKET)) {
                expect(TokenType.LBRACKET, "'['");
                expr();
                expect(TokenType.RBRACKET, "']'");
            }
            if (check(TokenType.LPAREN)) {
                expect(TokenType.LPAREN, "'('");
                if (!check(TokenType.RPAREN)) {
                    expr();
                    while (match(TokenType.COMMA) != null) {
                        expr();
                    }
                }
                expect(TokenType.RPAREN, "')'");
            }
            return;
        }
        error("ожидалось выражение");
    }
}