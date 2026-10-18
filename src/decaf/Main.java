package decaf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Main {

    public static void main(String[] args) {
        String stage = "parse";
        String input = null;
        String output = null;
        boolean debug = false;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("-t") || a.equals("--target")) {
                if (++i >= args.length) die("После " + a + " нужен этап");
                stage = args[i];
            } else if (a.equals("-o") || a.equals("--output")) {
                if (++i >= args.length) die("После " + a + " нужно имя файла");
                output = args[i];
            } else if (a.equals("-O") || a.equals("--opt")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) i++;
            } else if (a.equals("-d") || a.equals("--debug")) {
                debug = true;
            } else if (!a.startsWith("-") && input == null) {
                input = a;
            } else {
                die("Лишний аргумент: " + a);
            }
        }

        if (input == null) die("Не указан файл программы");
        if (!stage.equals("scan") && !stage.equals("parse")) die("Неизвестный этап: " + stage);

        String source;
        try {
            source = Files.readString(Path.of(input));
        } catch (IOException e) {
            die("Не удалось прочитать " + input);
            return;
        }

        DecafLexer lexer = new DecafLexer(source, input);
        List<DecafLexer.Token> tokens = lexer.scan();
        List<DecafError> lexErrors = lexer.problems();

        if (debug) System.err.println("Этап: " + stage);
        for (DecafError e : lexErrors) System.err.println(e);

        if (stage.equals("scan")) {
            StringBuilder sb = new StringBuilder();
            for (DecafLexer.Token t : tokens) {
                if (t.kind == DecafLexer.Kind.EOF) continue;
                sb.append(t.row).append(' ');
                String named = displayName(t.kind);
                if (named != null) sb.append(named).append(' ');
                sb.append(t.text).append('\n');
            }
            try {
                if (output == null) System.out.print(sb);
                else Files.writeString(Path.of(output), sb.toString());
            } catch (IOException e) {
                die("Не удалось записать " + output);
            }
        } else if (lexErrors.isEmpty()) {
            DecafParser parser = new DecafParser(tokens, input);
            boolean ok = parser.run();
            for (DecafError e : parser.problems()) System.err.println(e);
            if (!ok) System.exit(1);
        }

        if (!lexErrors.isEmpty()) System.exit(1);
    }

    private static String displayName(DecafLexer.Kind k) {
        switch (k) {
            case IDENT: return "IDENTIFIER";
            case INT_LIT: return "INTLITERAL";
            case LONG_LIT: return "LONGLITERAL";
            case CHAR_LIT: return "CHARLITERAL";
            case STRING_LIT: return "STRINGLITERAL";
            case BOOL_LIT: return "BOOLEANLITERAL";
            default: return null;
        }
    }

    private static void die(String msg) {
        System.err.println(msg);
        System.exit(1);
    }
}