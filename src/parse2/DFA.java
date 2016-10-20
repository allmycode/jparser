package parse2;

import java.util.List;

public interface DFA<T> {
    State getStart();

    boolean isFinal(State state);

    State next(State from, SymbolClass symClass, Symbol sym);

    State handleSpecial(State from, SymbolClass symClass, Symbol<T> symbol, Indexable2<Symbol<T>> it);

    String debugString();

    void start(List<Token> tokens);

    void process(State from, State to, SymbolClass symClass, Symbol<T> symbol);

    State handleSpecial2(State from, SymbolClass symClass, Symbol<T> symbol, Indexable2<Symbol<T>> it);
}
