import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String target = null;
        String inputFile = null;

        for (int i = 0; i < args.length; i++) {
            if ("-t".equals(args[i]) && i + 1 < args.length) {
                target = args[++i];
            } else if (!args[i].startsWith("-")) {
                inputFile = args[i];
            }
        }

        if (target == null || inputFile == null) {
            System.err.println("Использование: java Main -t <scan|parse> <файл.dcf>");
            System.exit(1);
        }

        String source;
        try {
            source = Files.readString(Path.of(inputFile));
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла " + inputFile + ": " + e.getMessage());
            System.exit(1);
            return;
        }

        Scanner scanner = new Scanner(inputFile, source);
        List<Token> tokens = scanner.scanTokens();

        if ("scan".equals(target)) {
            for (String err : scanner.getErrors()) {
                System.err.println(err);
            }
            for (Token token : tokens) {
                if (token.type != TokenType.EOF) {
                    System.out.println(token.formatForScan());
                }
            }
            System.exit(scanner.hasErrors() ? 1 : 0);
        } else if ("parse".equals(target)) {
            if (scanner.hasErrors()) {
                for (String err : scanner.getErrors()) {
                    System.err.println(err);
                }
                System.exit(1);
            }

            Parser parser = new Parser(inputFile, tokens);
            boolean ok = parser.parse();
            if (!ok) {
                for (String err : parser.getErrors()) {
                    System.err.println(err);
                }
                System.exit(1);
            }
            System.exit(0);
        } else {
            System.err.println("Неизвестная цель: " + target + " (ожидалось scan или parse)");
            System.exit(1);
        }
    }
}
