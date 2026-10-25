package decaf;

import java.io.PrintWriter;

public final class Diagnostics {
    private final String filename;
    private final PrintWriter output;
    private int count;

    public Diagnostics(String filename, PrintWriter output) {
        this.filename = filename;
        this.output = output;
    }

    public void error(int line, int column, String message) {
        count++;
        output.printf("%s:%d:%d: error: %s%n", filename, line, column, message);
        output.flush();
    }

    public int count() { return count; }
}
