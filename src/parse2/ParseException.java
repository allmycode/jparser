package parse2;

public class ParseException extends RuntimeException {
    public final int row;
    public final int col;

    public ParseException(String message, int row, int col) {
        super(message);
        this.row = row;
        this.col = col;
    }

    public ParseException(Throwable cause, String message, int row, int col) {
        super(message, cause);
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }
}
