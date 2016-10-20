package parse2;

public class StringIndexable implements Indexable<Symbol<Character>> {
    private final String str;

    int row = 0;
    int col = 0;
    public StringIndexable(String str) {
        this.str = str;
    }

    @Override
    public int length() {
        return str.length();
    }

    @Override
    public Symbol<Character> get(int i) {
        char c = str.charAt(i);
        Symbol s = new Symbol<>(c, row, col);
        col++;
        if (c == '\n') {
            row++;
            col = 0;
        }
        return s;
    }
}
