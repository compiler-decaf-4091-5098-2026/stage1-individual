package decaf;

import decaf.parser.DecafLexer;
import decaf.parser.DecafParser;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

public class DecafCompiler {
    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    // Returning a status makes the same entry point usable in automated tests.
    public static int run(String[] args, PrintStream out, PrintStream err) {
        CommandLine options;
        try {
            options = CommandLine.parse(args);
        } catch (IllegalArgumentException exception) {
            err.println(exception.getMessage());
            err.print(CommandLine.USAGE);
            return 2;
        }
        if (options.help()) {
            out.print(CommandLine.USAGE);
            return 0;
        }

        try {
            ErrorReporter errors = new ErrorReporter(options.input().toString(), err);
            DecafLexer lexer = new DecafLexer(
                    CharStreams.fromPath(options.input(), StandardCharsets.UTF_8));
            lexer.removeErrorListeners();
            lexer.addErrorListener(errors);

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            tokens.fill();
            if (errors.hasErrors()) {
                return 1;
            }

            String result;
            if (options.target().equals("scan")) {
                result = formatTokens(lexer, tokens);
            } else {
                DecafParser parser = new DecafParser(tokens);
                parser.removeErrorListeners();
                parser.addErrorListener(errors);
                var tree = parser.program();
                if (errors.hasErrors()) {
                    return 1;
                }
                result = options.debug() || options.output() != null
                        ? tree.toStringTree(parser) + System.lineSeparator() : "";
            }

            // No partial output is written when lexing or parsing fails.
            if (options.output() != null) {
                Files.writeString(options.output(), result, StandardCharsets.UTF_8);
            } else {
                out.print(result);
            }
            return 0;
        } catch (IOException exception) {
            err.println("I/O error: " + exception.getMessage());
            return 1;
        }
    }

    private static String formatTokens(DecafLexer lexer, CommonTokenStream tokens) {
        StringBuilder result = new StringBuilder();
        for (Token token : tokens.getTokens()) {
            if (token.getType() == Token.EOF) {
                continue;
            }
            result.append(token.getLine()).append(':')
                    .append(token.getCharPositionInLine() + 1).append(' ')
                    .append(lexer.getVocabulary().getSymbolicName(token.getType())).append(' ')
                    .append(token.getText()).append(System.lineSeparator());
        }
        return result.toString();
    }
}
