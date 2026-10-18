import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DecafCompiler {
    public static void main(String[] args) throws Exception {
        String mode = "parse";
        String file = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-t") && i + 1 < args.length) {
                mode = args[++i];
            } else if (!args[i].startsWith("-")) {
                file = args[i];
            }
        }

        if (file == null) {
            System.err.println("usage: decaf [-t scan|parse] <file>");
            System.exit(2);
        }

        String src = Files.readString(Path.of(file));
        Scanner sc = new Scanner(src, file);
        List<Token> tokens = sc.scanAll();

        if ("scan".equals(mode)) {
            for (Token t : tokens) {
                if (t.type == TokenType.EOF) {
                    continue;
                }
                if (t.type == TokenType.ERROR) {
                    continue;
                }

                String typeStr = switch (t.type) {
                    case CHARLITERAL, INTLITERAL, LONGLITERAL, BOOLEANLITERAL,
                         STRINGLITERAL, IDENTIFIER -> t.type.name();
                    default -> null;
                };

                if (typeStr == null) {
                    System.out.printf("%d %s%n", t.line, t.text);
                } else {
                    System.out.printf("%d %s %s%n", t.line, typeStr, t.text);
                }
            }
            return;
        }

        if ("parse".equals(mode)) {
            Parser p = new Parser(tokens, file);
            if (!p.parse()) {
                System.exit(1);
            }
            return;
        }
    }
}