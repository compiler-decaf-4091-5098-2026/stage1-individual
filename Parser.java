import java.util.ArrayList;

// рекурсивный нисходящий разбор грамматики decaf
public class Parser {

    private ArrayList<Token> tokens; // все токены программы вместе с eof
    private int index; // номер текущего токена
    private ErrorReporter reporter; // вывод ошибок

    public Parser(ArrayList<Token> tokens, ErrorReporter reporter) {
        this.tokens = tokens;
        this.reporter = reporter;
        this.index = 0;
    }

    // разбор всей программы
    public void parseProgram() {
        while (current().is("import")) {
            parseImportDecl();
        }
        boolean seenMethod = false; // был ли уже метод
        while (current().type != Token.EOF) {
            if (!isTypeKeyword(current()) && !current().is("void")) {
                syntaxError("expected field or method declaration");
            }
            boolean isMethod = false; // это метод или поле
            if (current().is("void")) {
                isMethod = true;
            } else if (lookAhead(2).is("(")) {
                isMethod = true;
            }
            if (isMethod) {
                parseMethodDecl();
                seenMethod = true;
            } else {
                if (seenMethod) {
                    syntaxError("field declaration after method declaration");
                }
                parseFieldDecl();
            }
        }
    }

    // объявление импорта
    private void parseImportDecl() {
        expect("import");
        expectIdentifier();
        expect(";");
    }

    // type id или id[число] через запятую до точки с запятой
    private void parseFieldDecl() {
        if (!isTypeKeyword(current())) {
            syntaxError("expected type int, long or bool");
        }
        advance();
        while (true) {
            expectIdentifier();
            if (current().is("[")) {
                advance();
                if (current().type != Token.INTLITERAL) {
                    syntaxError("expected integer array size");
                }
                advance();
                expect("]");
            }
            if (current().is(",")) {
                advance();
            } else {
                break;
            }
        }
        expect(";");
    }

    // заголовок метода с параметрами и тело
    private void parseMethodDecl() {
        if (current().is("void") || isTypeKeyword(current())) {
            advance();
        } else {
            syntaxError("expected method result type or void");
        }
        expectIdentifier();
        expect("(");
        if (!current().is(")")) {
            while (true) {
                if (!isTypeKeyword(current())) {
                    syntaxError("expected parameter type int, long or bool");
                }
                advance();
                expectIdentifier();
                if (current().is(",")) {
                    advance();
                } else {
                    break;
                }
            }
        }
        expect(")");
        parseBlock();
    }

    // блок сначала поля потом операторы
    private void parseBlock() {
        expect("{");
        while (isTypeKeyword(current())) {
            parseFieldDecl();
        }
        while (!current().is("}") && current().type != Token.EOF) {
            parseStatement();
        }
        expect("}");
    }

    // один оператор
    private void parseStatement() {
        Token t = current(); // текущий токен
        if (t.is("if")) {
            advance();
            expect("(");
            parseExpr();
            expect(")");
            parseBlock();
            if (current().is("else")) {
                advance();
                parseBlock();
            }
        } else if (t.is("for")) {
            advance();
            expect("(");
            expectIdentifier();
            expect("=");
            parseExpr();
            expect(";");
            parseExpr();
            expect(";");
            parseLocation();
            parseAssignExpr();
            expect(")");
            parseBlock();
        } else if (t.is("while")) {
            advance();
            expect("(");
            parseExpr();
            expect(")");
            parseBlock();
        } else if (t.is("return")) {
            advance();
            if (!current().is(";")) {
                parseExpr();
            }
            expect(";");
        } else if (t.is("break")) {
            advance();
            expect(";");
        } else if (t.is("continue")) {
            advance();
            expect(";");
        } else if (t.type == Token.IDENTIFIER) {
            if (lookAhead(1).is("(")) {
                parseMethodCall();
                expect(";");
            } else {
                parseLocation();
                parseAssignExpr();
                expect(";");
            }
        } else if (isTypeKeyword(t)) {
            syntaxError("variable declaration is allowed only at the beginning of a block");
        } else {
            syntaxError("expected statement");
        }
    }

    // id или id[выражение]
    private void parseLocation() {
        expectIdentifier();
        if (current().is("[")) {
            advance();
            parseExpr();
            expect("]");
        }
    }

    // знак присваивания и выражение или ++ или --
    private void parseAssignExpr() {
        Token t = current(); // текущий токен
        if (t.is("=") || t.is("+=") || t.is("-=") || t.is("*=") || t.is("/=") || t.is("%=")) {
            advance();
            parseExpr();
        } else if (t.is("++") || t.is("--")) {
            advance();
        } else {
            syntaxError("expected assignment operator, ++ or --");
        }
    }

    // вызов метода аргументом может быть выражение или строка
    private void parseMethodCall() {
        expectIdentifier();
        expect("(");
        if (!current().is(")")) {
            while (true) {
                if (current().type == Token.STRINGLITERAL) {
                    advance();
                } else {
                    parseExpr();
                }
                if (current().is(",")) {
                    advance();
                } else {
                    break;
                }
            }
        }
        expect(")");
    }

    // выражение самого низкого приоритета ||
    private void parseExpr() {
        parseAnd();
        while (current().is("||")) {
            advance();
            parseAnd();
        }
    }

    // &&
    private void parseAnd() {
        parseEquality();
        while (current().is("&&")) {
            advance();
            parseEquality();
        }
    }

    // == и !=
    private void parseEquality() {
        parseRelational();
        while (current().is("==") || current().is("!=")) {
            advance();
            parseRelational();
        }
    }

    // < <= >= >
    private void parseRelational() {
        parseAdditive();
        while (current().is("<") || current().is("<=") || current().is(">=") || current().is(">")) {
            advance();
            parseAdditive();
        }
    }

    // + и -
    private void parseAdditive() {
        parseMultiplicative();
        while (current().is("+") || current().is("-")) {
            advance();
            parseMultiplicative();
        }
    }

    // * / %
    private void parseMultiplicative() {
        parseUnary();
        while (current().is("*") || current().is("/") || current().is("%")) {
            advance();
            parseUnary();
        }
    }

    // унарный минус и отрицание
    private void parseUnary() {
        if (current().is("-") || current().is("!")) {
            advance();
            parseUnary();
        } else {
            parsePrimary();
        }
    }

    // самые простые выражения литералы вызовы приведения скобки
    private void parsePrimary() {
        Token t = current(); // текущий токен
        if (t.type == Token.INTLITERAL || t.type == Token.LONGLITERAL
                || t.type == Token.CHARLITERAL || t.type == Token.BOOLEANLITERAL) {
            advance();
        } else if (t.is("int") || t.is("long")) {
            advance();
            expect("(");
            parseExpr();
            expect(")");
        } else if (t.is("len")) {
            advance();
            expect("(");
            expectIdentifier();
            expect(")");
        } else if (t.is("(")) {
            advance();
            parseExpr();
            expect(")");
        } else if (t.type == Token.IDENTIFIER) {
            if (lookAhead(1).is("(")) {
                parseMethodCall();
            } else {
                parseLocation();
            }
        } else if (t.type == Token.STRINGLITERAL) {
            syntaxError("string literal is allowed only as a method call argument");
        } else {
            syntaxError("expected expression");
        }
    }

    // текущий токен
    private Token current() {
        return tokens.get(index);
    }

    // токен впереди текущего если токены кончились то eof
    private Token lookAhead(int offset) {
        int i = index + offset; // номер нужного токена
        if (i >= tokens.size()) {
            return tokens.get(tokens.size() - 1);
        }
        return tokens.get(i);
    }

    // переход к следующему токену eof не пропускается
    private void advance() {
        if (index < tokens.size() - 1) {
            index = index + 1;
        }
    }

    // токен является типом int long или bool
    private boolean isTypeKeyword(Token t) {
        return t.is("int") || t.is("long") || t.is("bool");
    }

    // ожидается ключевое слово или символ
    private void expect(String value) {
        if (current().is(value)) {
            advance();
        } else {
            syntaxError("expected '" + value + "'");
        }
    }

    // ожидается идентификатор
    private void expectIdentifier() {
        if (current().type == Token.IDENTIFIER) {
            advance();
        } else {
            syntaxError("expected identifier");
        }
    }

    // сообщение о синтаксической ошибке и остановка разбора
    private void syntaxError(String message) {
        Token t = current(); // токен на котором ошибка
        String found; // описание найденного токена
        if (t.type == Token.EOF) {
            found = "end of file";
        } else {
            found = "'" + t.text + "'";
        }
        reporter.error(t.line, t.column, "syntax error: " + message + ", found " + found);
        throw new ParseException(message);
    }
}
