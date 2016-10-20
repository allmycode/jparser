package parse2;

import java.util.List;

public class ListIndexable<T> implements Indexable<T> {
    private final List<T> list;

    public ListIndexable(List<T> list) {
        this.list = list;
    }

    @Override
    public int length() {
        return list.size();
    }

    @Override
    public T get(int i) {
        return list.get(i);
    }
}
