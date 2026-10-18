package decaf;

import java.util.ArrayList;
import java.util.List;

public final class DecafParser {
    private final List<DecafLexer.Token> tokens;
    private final String file;
    private final List<DecafError> problems = new ArrayList<>();
    private int idx = 0;

    public DecafParser(List<DecafLexer.Token> tokens, String file) {
        this.tokens = tokens;
        this.file = file;
    }

    public boolean run() {
        try {
            program();
            return true;
        } catch (Stop e) {
            return false;
        }
    }

    public List<DecafError> problems() { return problems; }

    private void program() {
        while (is(DecafLexer.Kind.KW_IMPORT)) importDecl();
        while (isType(cur().kind) && !looksLikeMethod()) fieldDecl();
        while (isType(cur().kind) || is(DecafLexer.Kind.KW_VOID)) methodDecl();
        expect(DecafLexer.Kind.EOF, "Ожидался конец файла");
    }

    private void importDecl() {
        expect(DecafLexer.Kind.KW_IMPORT, "Ожидался import");
        expect(DecafLexer.Kind.IDENT, "Ожидалось имя импортируемого метода");
        expect(DecafLexer.Kind.SEMI, "Ожидался символ ';'");
    }

    private void fieldDecl() {
        type();
        name();
        while (is(DecafLexer.Kind.COMMA)) { skip(); name(); }
        expect(DecafLexer.Kind.SEMI, "Ожидался символ ';'");
    }

    private void methodDecl() {
        if (!is(DecafLexer.Kind.KW_VOID)) type(); else skip();
        name();
        expect(DecafLexer.Kind.LPAREN, "Ожидался символ '('");
        if (!is(DecafLexer.Kind.RPAREN)) {
            param();
            while (is(DecafLexer.Kind.COMMA)) { skip(); param(); }
        }
        expect(DecafLexer.Kind.RPAREN, "Ожидался символ ')'");
        block();
    }

    private void param() {
        type();
        name();
    }

    private void block() {
        expect(DecafLexer.Kind.LBRACE, "Ожидался символ '{'");
        while (isType(cur().kind)) fieldDecl();
        while (!is(DecafLexer.Kind.RBRACE) && !is(DecafLexer.Kind.EOF)) stmt();
        expect(DecafLexer.Kind.RBRACE, "Ожидался символ '}'");
    }

    private void stmt() {
        if (is(DecafLexer.Kind.KW_IF)) { skip(); ifStmt(); return; }
        if (is(DecafLexer.Kind.KW_FOR)) { skip(); forStmt(); return; }
        if (is(DecafLexer.Kind.KW_WHILE)) { skip(); whileStmt(); return; }
        if (is(DecafLexer.Kind.KW_RETURN)) { skip(); retStmt(); return; }

        if (is(DecafLexer.Kind.IDENT)) {
            if (peekKind(1) == DecafLexer.Kind.LPAREN) {
                call();
                expect(DecafLexer.Kind.SEMI, "Ожидался символ ';'");
            } else {
                loc();
                assignOp();
                expect(DecafLexer.Kind.SEMI, "Ожидался символ ';'");
            }
            return;
        }
        throw fail(cur(), "Ожидался оператор");
    }

    private void ifStmt() {
        expect(DecafLexer.Kind.LPAREN, "Ожидался символ '('");
        expr();
        expect(DecafLexer.Kind.RPAREN, "Ожидался символ ')'");
        block();
        if (is(DecafLexer.Kind.KW_ELSE)) { skip(); block(); }
    }

    private void forStmt() {
        expect(DecafLexer.Kind.LPAREN, "Ожидался символ '('");
        name();
        expect(DecafLexer.Kind.OP_ASSIGN, "Ожидался символ '='");
        expr();
        expect(DecafLexer.Kind.SEMI, "Ожидался символ ';'");
        expr();
        expect(DecafLexer.Kind.SEMI, "Ожидался символ ';'");
        loc();
        expect(DecafLexer.Kind.OP_ASSIGN, "Ожидался символ '='");
        expr();
        expect(DecafLexer.Kind.RPAREN, "Ожидался символ ')'");
        block();
    }

    private void whileStmt() {
        expect(DecafLexer.Kind.LPAREN, "Ожидался символ '('");
        expr();
        expect(DecafLexer.Kind.RPAREN, "Ожидался символ ')'");
        block();
    }

    private void retStmt() {
        if (!is(DecafLexer.Kind.SEMI)) expr();
        expect(DecafLexer.Kind.SEMI, "Ожидался символ ';'");
    }

    private void assignOp() {
        expect(DecafLexer.Kind.OP_ASSIGN, "Ожидался оператор присваивания");
        expr();
    }

    private void call() {
        expect(DecafLexer.Kind.IDENT, "Ожидалось имя метода");
        expect(DecafLexer.Kind.LPAREN, "Ожидался символ '('");
        if (!is(DecafLexer.Kind.RPAREN)) {
            arg();
            while (is(DecafLexer.Kind.COMMA)) { skip(); arg(); }
        }
        expect(DecafLexer.Kind.RPAREN, "Ожидался символ ')'");
    }

    private void arg() {
        if (is(DecafLexer.Kind.STRING_LIT)) { skip(); return; }
        expr();
    }

    private void loc() {
        expect(DecafLexer.Kind.IDENT, "Ожидался идентификатор");
        if (is(DecafLexer.Kind.LBRACKET)) {
            skip();
            expr();
            expect(DecafLexer.Kind.RBRACKET, "Ожидался символ ']'");
        }
    }

    private void expr()      { orExpr(); }
    private void orExpr()    { andExpr();  while (is(DecafLexer.Kind.OP_OR))  { skip(); andExpr(); } }
    private void andExpr()   { eqExpr();   while (is(DecafLexer.Kind.OP_AND)) { skip(); eqExpr(); } }
    private void eqExpr()    { relExpr();  while (is(DecafLexer.Kind.OP_EQ) || is(DecafLexer.Kind.OP_NE)) { skip(); relExpr(); } }
    private void relExpr()   { addExpr();  while (is(DecafLexer.Kind.OP_LT) || is(DecafLexer.Kind.OP_GT) || is(DecafLexer.Kind.OP_LE) || is(DecafLexer.Kind.OP_GE)) { skip(); addExpr(); } }
    private void addExpr()   { mulExpr();  while (is(DecafLexer.Kind.OP_PLUS) || is(DecafLexer.Kind.OP_MINUS)) { skip(); mulExpr(); } }
    private void mulExpr()   { unExpr();   while (is(DecafLexer.Kind.OP_MUL) || is(DecafLexer.Kind.OP_DIV) || is(DecafLexer.Kind.OP_MOD)) { skip(); unExpr(); } }

    private void unExpr() {
        if (is(DecafLexer.Kind.OP_MINUS) || is(DecafLexer.Kind.OP_NOT)) { skip(); unExpr(); return; }
        if ((is(DecafLexer.Kind.KW_INT) || is(DecafLexer.Kind.KW_LONG)) && peekKind(1) == DecafLexer.Kind.LPAREN) {
            skip();
            expect(DecafLexer.Kind.LPAREN, "Ожидался символ '('");
            expr();
            expect(DecafLexer.Kind.RPAREN, "Ожидался символ ')'");
            return;
        }
        if (is(DecafLexer.Kind.KW_LEN)) {
            skip();
            expect(DecafLexer.Kind.LPAREN, "Ожидался символ '('");
            expect(DecafLexer.Kind.IDENT, "Ожидался идентификатор");
            expect(DecafLexer.Kind.RPAREN, "Ожидался символ ')'");
            return;
        }
        primary();
    }

    private void primary() {
        if (is(DecafLexer.Kind.INT_LIT) || is(DecafLexer.Kind.LONG_LIT) || is(DecafLexer.Kind.CHAR_LIT) || is(DecafLexer.Kind.BOOL_LIT)) { skip(); return; }
        if (is(DecafLexer.Kind.LPAREN)) {
            skip();
            expr();
            expect(DecafLexer.Kind.RPAREN, "Ожидался символ ')'");
            return;
        }
        if (is(DecafLexer.Kind.IDENT)) {
            if (peekKind(1) == DecafLexer.Kind.LPAREN) call();
            else loc();
            return;
        }
        throw fail(cur(), "Ожидалось выражение");
    }

    private void type() {
        if (is(DecafLexer.Kind.KW_INT) || is(DecafLexer.Kind.KW_LONG) || is(DecafLexer.Kind.KW_BOOL)) { skip(); return; }
        throw fail(cur(), "Ожидался тип данных");
    }

    private void name() { expect(DecafLexer.Kind.IDENT, "Ожидалось имя"); }

    private boolean isType(DecafLexer.Kind k) {
        return k == DecafLexer.Kind.KW_INT || k == DecafLexer.Kind.KW_LONG || k == DecafLexer.Kind.KW_BOOL;
    }

    private boolean looksLikeMethod() {
        return isType(cur().kind)
                && peekKind(1) == DecafLexer.Kind.IDENT
                && peekKind(2) == DecafLexer.Kind.LPAREN;
    }

    private DecafLexer.Token cur() { return tokens.get(idx); }

    private DecafLexer.Kind peekKind(int ahead) {
        int i = idx + ahead;
        return i < tokens.size() ? tokens.get(i).kind : DecafLexer.Kind.EOF;
    }

    private boolean is(DecafLexer.Kind k) { return cur().kind == k; }

    private void skip() { if (!is(DecafLexer.Kind.EOF)) idx++; }

    private void expect(DecafLexer.Kind k, String msg) {
        if (is(k)) { skip(); return; }
        throw fail(cur(), msg);
    }

    private Stop fail(DecafLexer.Token t, String msg) {
        problems.add(new DecafError(file, t.row, t.col, msg));
        return new Stop();
    }

    private static final class Stop extends RuntimeException { }
}