package parse2;

public class Symbol<T> {
    public final T val;
    public final int row;
    public final int col;

    public Symbol(T val, int row, int col) {
        this.val = val;
        this.row = row;
        this.col = col;
    }

    @Override
    public String toString() {
        return val.toString();
    }
}
