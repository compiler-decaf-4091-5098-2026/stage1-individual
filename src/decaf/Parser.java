package decaf;

import java.util.List;
import java.util.Set;

public final class Parser {
    private static final Set<String> TYPES = Set.of("int", "long", "bool");
    private static final Set<String> ASSIGN = Set.of("=", "+=", "-=", "*=", "/=", "%=");
    private static final List<Set<String>> BINARY = List.of(
        Set.of("||"), Set.of("&&"), Set.of("==", "!="), Set.of("<", "<=", ">=", ">"),
        Set.of("+", "-"), Set.of("*", "/", "%"));
    private final List<Token> tokens;
    private final Diagnostics diagnostics;
    private int position;

    public Parser(List<Token> tokens, Diagnostics diagnostics) {
        this.tokens = tokens;
        this.diagnostics = diagnostics;
    }

    public boolean parse() {
        try {
            program();
            return true;
        } catch (ParseFailure failure) {
            return false;
        }
    }

    private void program() {
        while (take("import")) { expect("IDENTIFIER"); expect(";"); }
        boolean methodsStarted = false;
        while (!at("EOF")) {
            boolean isVoid = take("void");
            if (!isVoid) type();
            expect("IDENTIFIER");
            if (take("(")) {
                methodsStarted = true;
                if (!at(")")) {
                    do { type(); expect("IDENTIFIER"); } while (take(","));
                }
                expect(")"); block();
            } else {
                if (isVoid) fail("a void declaration must be a method");
                if (methodsStarted) fail("field declarations must precede method declarations");
                fieldTail();
            }
        }
        expect("EOF");
    }

    private void type() {
        if (!TYPES.contains(current().kind())) fail("expected type int, long or bool");
        position++;
    }

    private void fieldTail() {
        arraySuffix();
        while (take(",")) { expect("IDENTIFIER"); arraySuffix(); }
        expect(";");
    }

    private void arraySuffix() {
        if (take("[")) { expect("INTLITERAL"); expect("]"); }
    }

    private void block() {
        expect("{");
        while (TYPES.contains(current().kind())) {
            type(); expect("IDENTIFIER"); fieldTail();
        }
        while (!at("}") && !at("EOF")) statement();
        expect("}");
    }

    private void statement() {
        if (at("IDENTIFIER")) {
            expect("IDENTIFIER");
            if (at("(")) callTail();
            else { locationTail(); assignment(); }
            expect(";");
        } else if (take("if")) {
            condition(); block();
            if (take("else")) block();
        } else if (take("while")) {
            condition(); block();
        } else if (take("for")) {
            expect("("); expect("IDENTIFIER"); expect("="); expression(); expect(";");
            expression(); expect(";"); expect("IDENTIFIER"); locationTail(); assignment();
            expect(")"); block();
        } else if (take("return")) {
            if (!at(";")) expression();
            expect(";");
        } else if (take("break") || take("continue")) {
            expect(";");
        } else fail("expected assignment, method call, if, for, while, return, break or continue");
    }

    private void condition() { expect("("); expression(); expect(")"); }

    private void assignment() {
        if (take("++") || take("--")) return;
        if (!ASSIGN.contains(current().kind())) fail("expected assignment operator or increment");
        position++;
        expression();
    }

    private void locationTail() {
        if (take("[")) { expression(); expect("]"); }
    }

    private void callTail() {
        expect("(");
        if (!at(")")) {
            do {
                if (!take("STRINGLITERAL")) expression();
            } while (take(","));
        }
        expect(")");
    }

    private void expression() { binary(0); }

    private void binary(int level) {
        if (level == BINARY.size()) { unary(); return; }
        binary(level + 1);
        while (BINARY.get(level).contains(current().kind())) {
            position++;
            binary(level + 1);
        }
    }

    private void unary() {
        if (take("-") || take("!")) { unary(); return; }
        if (take("int") || take("long")) { condition(); return; }
        if (take("len")) { expect("("); expect("IDENTIFIER"); expect(")"); return; }
        if (take("(")) { expression(); expect(")"); return; }
        if (take("INTLITERAL") || take("LONGLITERAL") || take("CHARLITERAL") || take("BOOLEANLITERAL")) return;
        if (take("IDENTIFIER")) {
            if (at("(")) callTail();
            else locationTail();
            return;
        }
        fail("expected expression");
    }

    private Token current() { return tokens.get(position); }
    private boolean at(String kind) { return current().kind().equals(kind); }
    private boolean take(String kind) {
        if (!at(kind)) return false;
        position++;
        return true;
    }
    private void expect(String kind) {
        if (!take(kind)) fail("expected " + kind);
    }
    private void fail(String message) {
        Token token = current();
        diagnostics.error(token.line(), token.column(), message + "; found "
            + (token.kind().equals("EOF") ? "end of file" : "'" + token.text() + "'"));
        throw new ParseFailure();
    }
    private static final class ParseFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
