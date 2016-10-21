package parse2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static parse2.DQStringDFA.States.*;
import static parse2.States.INVALID;
import static parse2.SymbolClasses.BSL;
import static parse2.SymbolClasses.DQ;
import static parse2.SymbolClasses.SQ;

public class DQStringDFA extends TableDFA<Character> {

    enum States implements State {
        START,
        STRING_DQ,
        STRING_Q,
        ESCAPE_DQ,
        ESCAPE_Q,
        FINISH
    }

    enum Acc {
        STRING_VALUE_DQ(TokenType.String, STRING_DQ, ESCAPE_DQ),
        STRING_VALUE_Q(TokenType.String, STRING_Q, ESCAPE_Q),
        ;

        public final State[] states;
        public final TokenType tokenType;
        Acc(TokenType tokenType, State... states) {
            this.tokenType = tokenType;
            this.states = states;
        }
    }

    Map<State, Acc> createAccs() {
        Map<State, Acc> res = new HashMap<>();
        for (Acc a : Acc.values()) {
            for (State s : a.states) {
                res.put(s, a);
            }
        }
        return res;
    }
    Map<State, Acc> accs = createAccs();

    private StringBuilder sb = new StringBuilder();;
    List<Token> tokens;

    public DQStringDFA() {
        define(tr(START, INVALID, p(DQ, STRING_DQ), p(SQ, STRING_Q)),
                tr(STRING_DQ, STRING_DQ, p(DQ, FINISH), p(BSL, ESCAPE_DQ)),
                tr(ESCAPE_DQ, INVALID, p(DQ, STRING_DQ), p(BSL, STRING_DQ)),
                tr(STRING_Q, STRING_Q, p(SQ, FINISH), p(BSL, ESCAPE_Q)),
                tr(ESCAPE_Q, INVALID, p(SQ, STRING_Q), p(BSL, STRING_Q))
                );
    }

    public State onFinish;
    public DQStringDFA(State onFinish) {
        this();
        this.onFinish = onFinish;
    }

    @Override
    public State getStart() {
        return START;
    }

    @Override
    public boolean isFinal(State state) {
        return state == FINISH;
    }

    @Override
    public State handleSpecial(State from, SymbolClass symClass, Symbol<Character> symbol, Indexable2<Symbol<Character>> it) {
        if (from == ESCAPE_DQ) {
            if (symbol.val == 'n' || symbol.val == 't') {
                return STRING_DQ;
            }
        }
        if (from == ESCAPE_Q) {
            if (symbol.val == 'n' || symbol.val == 't') {
                return STRING_Q;
            }
        }
        return null;
    }

    @Override
    public String debugString() {
        return sb.toString();
    }

    @Override
    public void start(List<Token> tokens) {
        sb = new StringBuilder();
        this.tokens = tokens;
    }

    @Override
    public void process(State from, State to, SymbolClass symClass, Symbol<Character> symbol) {
        Acc fa = accs.get(from);
        Acc ta = accs.get(to);
        if (fa != ta) {
            if (fa != null) {
                if (sb.length() > 0) {
                    tokens.add(new Token(fa.tokenType, sb.toString()));
                }
            }
            sb.delete(0, sb.length());
        }
        if (to != ESCAPE_DQ && from != ESCAPE_DQ && to != ESCAPE_Q && from != ESCAPE_Q && fa != null) {
            sb.append(symbol.val);
        }
        if (from == ESCAPE_DQ || from == ESCAPE_Q) {
            if (symbol.val == 'n') {
                sb.append('\n');
            } else if (symbol.val == 't') {
                sb.append('\t');
            } else {
                sb.append(symbol.val);
            }
        }
    }

    @Override
    public State handleSpecial2(State from, SymbolClass symClass, Symbol<Character> symbol, Indexable2<Symbol<Character>> it) {
        return null;
    }

    public void dump() {
        System.out.println(tokens);
    }
}
