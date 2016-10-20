package parse;

public interface ISymTranslator<F, T> {
    public T translate(F from);
}
