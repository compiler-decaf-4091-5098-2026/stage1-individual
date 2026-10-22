import java.util.ArrayList;

public class Lexer {

    private String text; // исходный текст программы
    private int pos; // текущая позиция в тексте
    private int line; // текущая строка с единицы
    private int column; // текущий столбец с единицы
    private ErrorReporter reporter; // вывод ошибок

    public Lexer(String text, ErrorReporter reporter) {
        this.text = text;
        this.reporter = reporter;
        this.pos = 0;
        this.line = 1;
        this.column = 1;
    }

    // возвращает следующий корректный токен или токен eof в конце текста
    public Token nextToken() {
        while (true) {
            skipWhitespaceAndComments();
            if (pos >= text.length()) {
                return new Token(Token.EOF, "", line, column);
            }
            char c = text.charAt(pos);
            int startLine = line; // строка начала токена
            int startColumn = column; // столбец начала токена
            Token result = null; // найденный токен
            if (isLetter(c)) {
                result = readWord(startLine, startColumn);
            } else if (isDigit(c)) {
                result = readNumber(startLine, startColumn);
            } else if (c == '"') {
                result = readString(startLine, startColumn);
            } else if (c == '\'') {
                result = readChar(startLine, startColumn);
            } else {
                result = readSymbol(startLine, startColumn);
            }
            if (result != null) {
                return result;
            }
        }
    }

    // буква или подчеркивание
    private boolean isLetter(char c) {
        if (c >= 'a' && c <= 'z') {
            return true;
        }
        if (c >= 'A' && c <= 'Z') {
            return true;
        }
        if (c == '_') {
            return true;
        }
        return false;
    }

    // десятичная цифра
    private boolean isDigit(char c) {
        if (c >= '0' && c <= '9') {
            return true;
        }
        return false;
    }

    // шестнадцатеричная цифра
    private boolean isHexDigit(char c) {
        if (isDigit(c)) {
            return true;
        }
        if (c >= 'a' && c <= 'f') {
            return true;
        }
        if (c >= 'A' && c <= 'F') {
            return true;
        }
        return false;
    }

    // символ внутри кавычек без экранирования
    private boolean isPlainCharInLiteral(char c) {
        if (c < 32 || c > 126) {
            return false;
        }
        if (c == '"' || c == '\'' || c == '\\') {
            return false;
        }
        return true;
    }

    // допустимая буква после обратной косой черты
    private boolean isEscapeLetter(char c) {
        if (c == '"' || c == '\'' || c == '\\') {
            return true;
        }
        if (c == 't' || c == 'n' || c == 'r' || c == 'f') {
            return true;
        }
        return false;
    }

    // символ на смещении от текущей позиции или нулевой символ если текст кончился
    private char peek(int offset) {
        int index = pos + offset;
        if (index >= text.length()) {
            return '\0';
        }
        return text.charAt(index);
    }

    // сдвиг на один символ с учетом строки и столбца
    private void advance() {
        char c = text.charAt(pos);
        pos = pos + 1;
        if (c == '\n') {
            line = line + 1;
            column = 1;
        } else {
            column = column + 1;
        }
    }

    // пропуск пробелов и комментариев
    private void skipWhitespaceAndComments() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
                advance();
            } else if (c == '/' && peek(1) == '/') {
                while (pos < text.length() && text.charAt(pos) != '\n') {
                    advance();
                }
            } else if (c == '/' && peek(1) == '*') {
                int startLine = line; // строка начала комментария
                int startColumn = column; // столбец начала комментария
                advance();
                advance();
                boolean closed = false; // нашли ли конец комментария
                while (pos < text.length()) {
                    if (text.charAt(pos) == '*' && peek(1) == '/') {
                        advance();
                        advance();
                        closed = true;
                        break;
                    }
                    advance();
                }
                if (!closed) {
                    reporter.error(startLine, startColumn, "unterminated block comment");
                }
            } else {
                break;
            }
        }
    }

    // идентификатор ключевое слово или булев литерал
    private Token readWord(int startLine, int startColumn) {
        int start = pos; // начало слова
        while (pos < text.length() && (isLetter(text.charAt(pos)) || isDigit(text.charAt(pos)))) {
            advance();
        }
        String word = text.substring(start, pos);
        if (word.equals("true") || word.equals("false")) {
            return new Token(Token.BOOLEANLITERAL, word, startLine, startColumn);
        }
        if (isKeyword(word)) {
            return new Token(Token.KEYWORD, word, startLine, startColumn);
        }
        return new Token(Token.IDENTIFIER, word, startLine, startColumn);
    }

    // проверка на ключевое слово
    private boolean isKeyword(String word) {
        String[] keywords = {"if", "bool", "break", "import", "continue", "else", "for", "while",
                "int", "long", "return", "len", "void"}; // список ключевых слов без true и false
        for (int i = 0; i < keywords.length; i++) {
            if (keywords[i].equals(word)) {
                return true;
            }
        }
        return false;
    }

    // целый или длинный литерал в десятичной или шестнадцатеричной записи
    private Token readNumber(int startLine, int startColumn) {
        int start = pos; // начало числа
        if (text.charAt(pos) == '0' && peek(1) == 'x') {
            advance();
            advance();
            int digitsStart = pos; // начало шестнадцатеричных цифр
            while (pos < text.length() && isHexDigit(text.charAt(pos))) {
                advance();
            }
            if (pos == digitsStart) {
                reporter.error(startLine, startColumn, "no hexadecimal digits after 0x");
                return null;
            }
        } else {
            while (pos < text.length() && isDigit(text.charAt(pos))) {
                advance();
            }
        }
        if (pos < text.length() && text.charAt(pos) == 'L') {
            advance();
            return new Token(Token.LONGLITERAL, text.substring(start, pos), startLine, startColumn);
        }
        return new Token(Token.INTLITERAL, text.substring(start, pos), startLine, startColumn);
    }

    // строковый литерал в двойных кавычках
    private Token readString(int startLine, int startColumn) {
        int start = pos; // начало литерала вместе с кавычкой
        ArrayList<Integer> errLines = new ArrayList<Integer>(); // строки отложенных ошибок
        ArrayList<Integer> errColumns = new ArrayList<Integer>(); // столбцы отложенных ошибок
        ArrayList<String> errMessages = new ArrayList<String>(); // тексты отложенных ошибок
        advance();
        while (true) {
            if (pos >= text.length() || text.charAt(pos) == '\n') {
                reporter.error(startLine, startColumn, "unterminated string literal, missing closing quote");
                return null;
            }
            char c = text.charAt(pos);
            if (c == '"') {
                advance();
                break;
            }
            if (c == '\\') {
                int errLine = line; // строка обратной косой черты
                int errColumn = column; // столбец обратной косой черты
                advance();
                if (pos >= text.length() || text.charAt(pos) == '\n') {
                    errLines.add(errLine);
                    errColumns.add(errColumn);
                    errMessages.add("backslash at end of line");
                } else if (isEscapeLetter(text.charAt(pos))) {
                    advance();
                } else {
                    errLines.add(errLine);
                    errColumns.add(errColumn);
                    errMessages.add("invalid escape sequence \\" + text.charAt(pos));
                    advance();
                }
            } else if (isPlainCharInLiteral(c)) {
                advance();
            } else {
                errLines.add(line);
                errColumns.add(column);
                errMessages.add("invalid character in string literal: " + describeChar(c));
                advance();
            }
        }
        if (errMessages.size() > 0) {
            for (int i = 0; i < errMessages.size(); i++) {
                reporter.error(errLines.get(i), errColumns.get(i), errMessages.get(i));
            }
            return null;
        }
        return new Token(Token.STRINGLITERAL, text.substring(start, pos), startLine, startColumn);
    }

    // символьный литерал в одинарных кавычках
    private Token readChar(int startLine, int startColumn) {
        int start = pos; // начало литерала вместе с кавычкой
        advance();
        if (pos >= text.length() || text.charAt(pos) == '\n') {
            reporter.error(startLine, startColumn, "unterminated character literal");
            return null;
        }
        char c = text.charAt(pos);
        if (c == '\'') {
            advance();
            reporter.error(startLine, startColumn, "empty character literal");
            return null;
        }
        boolean bad = false; // был ли недопустимый символ
        if (c == '\\') {
            int errLine = line; // строка обратной косой черты
            int errColumn = column; // столбец обратной косой черты
            advance();
            if (pos >= text.length() || text.charAt(pos) == '\n') {
                reporter.error(errLine, errColumn, "backslash at end of line");
                return null;
            }
            if (isEscapeLetter(text.charAt(pos))) {
                advance();
            } else {
                reporter.error(errLine, errColumn, "invalid escape sequence \\" + text.charAt(pos));
                bad = true;
                advance();
            }
        } else if (isPlainCharInLiteral(c)) {
            advance();
        } else {
            reporter.error(line, column, "invalid character in character literal: " + describeChar(c));
            bad = true;
            advance();
        }
        if (pos < text.length() && text.charAt(pos) == '\'') {
            advance();
        } else {
            reporter.error(startLine, startColumn, "character literal must contain exactly one character and a closing quote");
            while (pos < text.length() && text.charAt(pos) != '\n' && text.charAt(pos) != '\'') {
                advance();
            }
            if (pos < text.length() && text.charAt(pos) == '\'') {
                advance();
            }
            return null;
        }
        if (bad) {
            return null;
        }
        return new Token(Token.CHARLITERAL, text.substring(start, pos), startLine, startColumn);
    }

    // знаки операций и разделители
    private Token readSymbol(int startLine, int startColumn) {
        char c = text.charAt(pos);
        char next = peek(1);
        String two = "" + c + next; // пара символов для двухсимвольных операций
        String[] twoSymbols = {"+=", "-=", "*=", "/=", "%=", "++", "--", "<=", ">=", "==", "!=", "&&", "||"}; // двухсимвольные
        for (int i = 0; i < twoSymbols.length; i++) {
            if (twoSymbols[i].equals(two)) {
                advance();
                advance();
                return new Token(Token.SYMBOL, two, startLine, startColumn);
            }
        }
        String oneSymbols = "(){}[];,=+-*/%<>!"; // односимвольные
        if (oneSymbols.indexOf(c) >= 0) {
            advance();
            return new Token(Token.SYMBOL, "" + c, startLine, startColumn);
        }
        if (c == '&' || c == '|') {
            reporter.error(startLine, startColumn, "invalid character " + describeChar(c) + ", did you mean " + c + c + "?");
        } else {
            reporter.error(startLine, startColumn, "invalid character " + describeChar(c));
        }
        advance();
        return null;
    }

    // описание символа для сообщения об ошибке
    private String describeChar(char c) {
        if (c >= 32 && c <= 126) {
            return "'" + c + "'";
        }
        return "with code " + (int) c;
    }
}
