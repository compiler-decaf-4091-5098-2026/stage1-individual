package decaf;

import java.io.PrintStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class ErrorReporter extends BaseErrorListener {
    private final String filename;
    private final PrintStream err;
    private int count;

    public ErrorReporter(String filename, PrintStream err) {
        this.filename = filename;
        this.err = err;
    }

    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String message,
            RecognitionException exception) {
        count++;
        err.printf("%s:%d:%d: %s%n", filename, line, charPositionInLine + 1, message);
    }

    public boolean hasErrors() {
        return count > 0;
    }
}
