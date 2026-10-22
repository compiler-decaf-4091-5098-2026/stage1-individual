public class ErrorReporter {

    private String fileName; // имя файла для сообщений
    private int errorCount; // сколько ошибок уже выведено

    public ErrorReporter(String fileName) {
        this.fileName = fileName;
        this.errorCount = 0;
    }

    // вывод ошибки в stderr с именем файла строкой и столбцом
    public void error(int line, int column, String message) {
        System.out.flush();
        System.err.println(fileName + ":" + line + ":" + column + ": error: " + message);
        System.err.flush();
        errorCount = errorCount + 1;
    }

    // были ли ошибки
    public boolean hasErrors() {
        return errorCount > 0;
    }

    // количество ошибок
    public int getErrorCount() {
        return errorCount;
    }
}
