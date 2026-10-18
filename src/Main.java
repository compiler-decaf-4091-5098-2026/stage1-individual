import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class Main {
    public static void main(String[] args) {
        String target = null;
        String output = null;
        boolean debug = false;
        List<String> files = new ArrayList<>();
        List<String> optimizations = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "-t": case "--target": target = args[++i]; break;
                case "-o": case "--output": output = args[++i]; break;
                case "-O": case "--opt": optimizations.add(args[++i]); break;
                case "-d": case "--debug": debug = true; break;
                default:
                    if (a.startsWith("-")) { System.err.println("неизвестный флаг: " + a); System.exit(2); }
                    files.add(a);
            }
        }

        if (files.size() != 1) { System.err.println("нужно указать ровно один файл"); System.exit(2); }
        if (target == null) target = "scan";

        String fileName = files.get(0);
        String src;
        try { src = Files.readString(Path.of(fileName), StandardCharsets.UTF_8); }
        catch (IOException e) { System.err.println("не удалось прочитать " + fileName + ": " + e.getMessage()); System.exit(1); return; }

        Lexer lexer = new Lexer(src, fileName);
        List<Token> tokens = lexer.scan();

        if ("scan".equals(target)) {
            StringBuilder out = new StringBuilder();
            for (Token t : tokens) {
                if (t.type == TokenType.EOF) continue;
                out.append(t.line).append(' ');
                if (needsTypeName(t.type)) out.append(t.type.name()).append(' ');
                out.append(t.text).append('\n');
            }
            writeOrPrint(out.toString(), output);
            return;
        }

        if ("parse".equals(target)) {
            Parser p = new Parser(tokens);
            try {
                p.parse();
            } catch (ParseException e) {
                System.err.println(fileName + ": " + e.getMessage());
                System.exit(1);
            }
            return;
        }

        System.err.println("неизвестная цель: " + target);
        System.exit(2);
    }

    private static boolean needsTypeName(TokenType t) {
        switch (t) {
            case CHARLITERAL: case INTLITERAL: case LONGLITERAL:
            case BOOLEANLITERAL: case STRINGLITERAL: case IDENTIFIER:
                return true;
            default: return false;
        }
    }

    private static void writeOrPrint(String s, String output) {
        if (output == null) { System.out.print(s); return; }
        try { Files.writeString(Path.of(output), s, StandardCharsets.UTF_8); }
        catch (IOException e) { System.err.println("не удалось записать " + output + ": " + e.getMessage()); System.exit(1); }
    }
}