package decaf;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class ErrorReporter extends BaseErrorListener {

    private int count;


    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String message,
            RecognitionException exception
    ) {
        count++;

        System.err.printf(
                "%d:%d: %s%n",
                line,
                charPositionInLine + 1,
                message
        );
    }

    public boolean hasErrors() {
        return count > 0;
    }
}
