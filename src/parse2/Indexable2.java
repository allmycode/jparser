package parse2;

public interface Indexable2<T> {
    boolean hasMore();
    T next();
    void back();
}
