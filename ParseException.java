public class ParseException extends RuntimeException {

    // исключение для остановки разбора после синтаксической ошибки
    public ParseException(String message) {
        super(message);
    }
}
