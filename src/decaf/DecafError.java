package decaf;

public final class DecafError {
    private final String file;
    private final int row;
    private final int col;
    private final String text;

    public DecafError(String file, int row, int col, String text) {
        this.file = file;
        this.row = row;
        this.col = col;
        this.text = text;
    }

    @Override
    public String toString() {
        return file + ":" + row + ":" + col + ": " + text;
    }
}