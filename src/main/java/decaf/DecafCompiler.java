package decaf;

import decaf.parser.DecafLexer;
import decaf.parser.DecafParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DecafCompiler {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: DecafCompiler <file>");
            System.exit(2);
        }

        try {
            String source = Files.readString(Path.of(args[0]));

            ErrorReporter errors = new ErrorReporter();

            DecafLexer lexer = new DecafLexer(CharStreams.fromString(source));
            lexer.removeErrorListeners();
            lexer.addErrorListener(errors);

            CommonTokenStream tokens = new CommonTokenStream(lexer);

            DecafParser parser = new DecafParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(errors);

            var tree = parser.program();

            if (errors.hasErrors()) {
                System.exit(1);
            }

            System.out.println(tree.toStringTree(parser));
        } catch (IOException ex) {
            System.err.println("Cannot read file: " + ex.getMessage());
            System.exit(1);
        }
    }
}
