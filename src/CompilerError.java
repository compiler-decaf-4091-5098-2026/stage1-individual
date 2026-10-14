// Хранит информацию об ошибке: файл, строку, столбец и сообщение.

public class CompilerError {
    private final String fileName;
    private final int line;
    private final int column;
    private final String message;

    public CompilerError(String fileName, int line, int column, String message) {
        this.fileName = fileName;
        this.line = line;
        this.column = column;
        this.message = message;
    }

    public String getFileName() { return fileName; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return fileName + ":" + line + ":" + column + ": " + message;
    }
}