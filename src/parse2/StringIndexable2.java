package parse2;

public class StringIndexable2 implements Indexable2<Symbol<Character>> {
    private final String str;

    int i = 0;
    int row = 0;
    int col = 0;
    int prevcol = 0;
    public StringIndexable2(String str) {
        this.str = str;
    }

    @Override
    public boolean hasMore() {
        return i < str.length();
    }

    @Override
    public Symbol<Character> next() {
        char c = str.charAt(i++);
        Symbol s = new Symbol<>(c, row, col);
        col++;
        if (c == '\n') {
            row++;
            prevcol = col;
            col = 0;
        }
        return s;
    }

    @Override
    public void back() {
        char c = str.charAt(--i);
        if (c == '\n') {
            row--;
            col = prevcol;
        } else {
            col--;
        }
    }
}
