import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

// Главный класс программы, отвечающий за запуск компилятора.

public class Main {
    public static void main(String[] args) {
        String target = "parse";
        String fileName = null;
        String outputName = null;
        boolean debug = false;

        // Обработка параметров запуска.

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("-t") || arg.equals("--target")) {
                if (i + 1 >= args.length)
                    fail("Не указан этап после " + arg);
                target = args[++i];
            } else if (arg.equals("-o") || arg.equals("--output")) {
                if (i + 1 >= args.length)
                    fail("Не указано имя выходного файла");
                outputName = args[++i];
            } else if (arg.equals("-O") || arg.equals("--opt")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("-"))
                    i++;
            } else if (arg.equals("-d") || arg.equals("--debug")) {
                debug = true;
            } else if (!arg.startsWith("-") && fileName == null) {
                fileName = arg;
            } else {
                fail("Неизвестный или лишний аргумент: " + arg);
            }
        }

        if (fileName == null)
            fail("Не указан файл программы Decaf");
        if (!target.equals("scan") && !target.equals("parse"))
            fail("Неизвестный этап: " + target);


        // Чтение исходного файла.

        String source;
        try { source = Files.readString(Path.of(fileName)); }
        catch (IOException e) {
            fail("Не удалось прочитать файл: " + fileName);
            return;
        }


        // Лексический анализ.

        Lexer lexer = new Lexer(source, fileName);
        List<Token> tokens = lexer.scanTokens();
        List<CompilerError> errors = lexer.getErrors();

        if (debug)
            System.err.println("Этап: " + target);
        for (CompilerError error : errors)
            System.err.println(error);


        // Выбор этапа компиляции.

        if (target.equals("scan")) {
            StringBuilder result = new StringBuilder();

            for (Token token : tokens) {
                if (token.getType() != TokenType.EOF)
                    result.append(formatToken(token)).append('\n');
            }
            if (outputName == null) { System.out.print(result); }
            else {
                try { Files.writeString(Path.of(outputName), result.toString()); }
                catch (IOException e) { fail("Не удалось записать файл: " + outputName); }
            }
        } else if (errors.isEmpty()) {
            Parser parser = new Parser(tokens, fileName);
            boolean success = parser.parse();

            for (CompilerError error : parser.getErrors())
                System.err.println(error);
            if (!success)
                System.exit(1);
        }
        if (!errors.isEmpty())
            System.exit(1);
    }


    // Формирует строку для вывода распознанного токена.

    private static String formatToken(Token token) {
        String type = getDisplayType(token.getType());
        if (type == null)
            return token.getLine() + " " + token.getText();
        return token.getLine() + " " + type + " " + token.getText();
    }

    // Определяет название типа токена для вывода на экран.

    private static String getDisplayType(TokenType type) {
        switch (type) {
            case IDENTIFIER: return "IDENTIFIER";
            case INT_LITERAL: return "INTLITERAL";
            case LONG_LITERAL: return "LONGLITERAL";
            case BOOLEAN_LITERAL: return "BOOLEANLITERAL";
            case STRING_LITERAL: return "STRINGLITERAL";
            case CHAR_LITERAL: return "CHARLITERAL";
            default: return null;
        }
    }


    // Выводит сообщение об ошибке и завершает программу с кодом 1.

    private static void fail(String message) {
        System.err.println(message);
        System.exit(1);
    }
}