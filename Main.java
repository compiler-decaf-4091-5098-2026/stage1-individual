import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {

    // точка входа разбор аргументов и запуск нужного этапа
    public static void main(String[] args) {
        String target = "parse"; // этап компиляции по умолчанию
        String outputName = null; // имя файла вывода
        String fileName = null; // имя входного файла
        boolean debug = false; // режим отладки

        int i = 0; // номер текущего аргумента
        while (i < args.length) {
            String arg = args[i];
            if (arg.equals("-t") || arg.equals("--target")) {
                if (i + 1 >= args.length) {
                    System.err.println("missing target after " + arg);
                    System.exit(2);
                }
                target = args[i + 1];
                i = i + 2;
            } else if (arg.equals("-o") || arg.equals("--output")) {
                if (i + 1 >= args.length) {
                    System.err.println("missing file name after " + arg);
                    System.exit(2);
                }
                outputName = args[i + 1];
                i = i + 2;
            } else if (arg.equals("-O") || arg.equals("--opt")) {
                if (i + 2 < args.length && !args[i + 1].startsWith("-")) {
                    i = i + 2;
                } else {
                    i = i + 1;
                }
            } else if (arg.equals("-d") || arg.equals("--debug")) {
                debug = true;
                i = i + 1;
            } else if (arg.startsWith("-")) {
                System.err.println("unknown option: " + arg);
                System.exit(2);
            } else {
                if (fileName != null) {
                    System.err.println("exactly one input file is required");
                    System.exit(2);
                }
                fileName = arg;
                i = i + 1;
            }
        }

        if (fileName == null) {
            System.err.println("usage: run.sh [-t scan|parse] [-o file] [-d] file.decaf");
            System.exit(2);
        }
        if (!target.equals("scan") && !target.equals("parse")) {
            System.err.println("target '" + target + "' is not implemented, stage 1 supports scan and parse");
            System.exit(2);
        }

        String source = ""; // текст программы
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(fileName));
            source = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("cannot read file " + fileName);
            System.exit(1);
        }

        ErrorReporter reporter = new ErrorReporter(fileName);
        Lexer lexer = new Lexer(source, reporter);

        if (target.equals("scan")) {
            runScan(lexer, outputName, debug);
        } else {
            runParse(lexer, reporter, debug);
        }

        if (reporter.hasErrors()) {
            System.exit(1);
        }
        System.exit(0);
    }

    // режим scan вывод таблицы токенов
    private static void runScan(Lexer lexer, String outputName, boolean debug) {
        PrintStream out = System.out; // куда выводить токены
        if (outputName != null) {
            try {
                out = new PrintStream(outputName, "UTF-8");
            } catch (IOException e) {
                System.err.println("cannot open output file " + outputName);
                System.exit(1);
            }
        }
        int count = 0; // сколько токенов выведено
        while (true) {
            Token t = lexer.nextToken();
            if (t.type == Token.EOF) {
                break;
            }
            out.print(t.toScanLine() + "\n");
            count = count + 1;
        }
        out.flush();
        if (outputName != null) {
            out.close();
        }
        if (debug) {
            System.err.println("[debug] tokens: " + count);
        }
    }

    // режим parse при успехе ничего не выводим
    private static void runParse(Lexer lexer, ErrorReporter reporter, boolean debug) {
        ArrayList<Token> tokens = new ArrayList<Token>(); // все токены программы
        while (true) {
            Token t = lexer.nextToken();
            tokens.add(t);
            if (t.type == Token.EOF) {
                break;
            }
        }
        if (debug) {
            System.err.println("[debug] tokens: " + (tokens.size() - 1));
        }
        if (reporter.hasErrors()) {
            return;
        }
        Parser parser = new Parser(tokens, reporter);
        try {
            parser.parseProgram();
        } catch (ParseException e) {
            // ошибка уже выведена в parser
            return;
        }
    }
}
