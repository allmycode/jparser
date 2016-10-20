package parse2;

import java.util.List;
import java.util.function.Function;

public class DFAExecutor<T> {

    public void execute(Indexable2<Symbol<T>> it, Function<T, SymbolClass> translate, DFA dfa, List<Token> tokens, boolean debug, boolean stopOnFinal) {
        State state = dfa.getStart();
        dfa.start(tokens);

        while (it.hasMore()) {
            Symbol<T> sym = it.next();
            SymbolClass symClass = translate.apply(sym.val);

            State special = dfa.handleSpecial(state, symClass, sym, it);
            State nextState = special == null ? dfa.next(state, symClass, sym) : special;
            if (nextState == States.INVALID) {
                throw new ParseException("Illegal char '" + sym + "' (" + symClass + ") at [" + sym.row + ":" + sym.col + "]. State now: " + state + "."
                        + (debug ? dfa.debugString() : ""), sym.row, sym.col);
            }
            if (debug) {
                System.out.println(state + " -> " + nextState + " on '" + sym + "'" + ". " + dfa.debugString());
            }
            dfa.process(state, nextState, symClass, sym);
            state = nextState;
            State special2 = dfa.handleSpecial2(state, symClass, sym, it);
            if (special2 != null) {
                state = special2;
            }
            if (dfa.isFinal(state) && it.hasMore() && stopOnFinal) {
                break;
            }
        }
        dfa.process(state, null, null, null);
        if (!dfa.isFinal(state)) {
            throw new ParseException("Unexpected EOF", 0, 0);
        }
    }
}
