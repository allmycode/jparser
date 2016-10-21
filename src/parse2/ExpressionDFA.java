package parse2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static parse2.ExpressionDFA.States.*;
import static parse2.L2SymbolClasses.ALNUM;
import static parse2.L2SymbolClasses.BLANK;
import static parse2.L2SymbolClasses.OP;
import static parse2.L2SymbolClasses.OP_NODOT;
import static parse2.SymbolClasses.ALPHA;
import static parse2.SymbolClasses.DOT;
import static parse2.SymbolClasses.DQ;
import static parse2.SymbolClasses.NUM;
import static parse2.SymbolClasses.SQ;
import static parse2.SymbolClasses.UNDERSCORE;

public class ExpressionDFA extends TableDFA<Character> {
    enum States implements State {
        BASE,
        NUM1,
        NUM_DOT,
        NUM2,
        IDENTIFIER,
        OPST,
        STR,
        STR_
    }

    enum Acc {
        ACC_IDENTIFIER(TokenType.Identifier, IDENTIFIER),
        ACC_NUMBER(TokenType.Number, NUM1, NUM_DOT ,NUM2),
        ACC_OPERATOR(TokenType.Operator, OPST),

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

    public ExpressionDFA() {
        define(
                tr(BASE, p(NUM, NUM1), p(ALPHA, IDENTIFIER), p(UNDERSCORE, IDENTIFIER), p(OP, OPST), p(SQ, STR), p(DQ, STR), p(BLANK, BASE)),
                tr(NUM1, p(NUM, NUM1), p(DOT, NUM_DOT), p(BLANK, BASE), p(OP_NODOT, OPST)),
                tr(NUM_DOT, p(NUM, NUM2)),
                tr(NUM2, p(NUM, NUM2), p(BLANK, BASE), p(OP_NODOT, OPST)),
                tr(IDENTIFIER, p(ALNUM, IDENTIFIER), p(UNDERSCORE, IDENTIFIER), p(BLANK, BASE), p(OP, OPST)),
                tr(OPST, p(BLANK, BASE), p(ALNUM, IDENTIFIER), p(UNDERSCORE, IDENTIFIER), p(NUM, NUM1), p(SQ, STR), p(DQ, STR)),
                tr(STR_, p(OP, OPST), p(BLANK, BASE))
        );
    }

    @Override
    public State getStart() {
        return BASE;
    }

    @Override
    public boolean isFinal(State state) {
        return true;
    }

    @Override
    public State handleSpecial(State from, SymbolClass symClass, Symbol<Character> symbol, Indexable2<Symbol<Character>> it) {
        return null;
    }

    @Override
    public String debugString() {
        return sb.toString();
    }

    @Override
    public void start(List<Token> tokens) {
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
        if (symbol != null) {
            sb.append(symbol.val);
        }
    }

    DQStringDFA stringDFA = new DQStringDFA(STR_);
    @Override
    public State handleSpecial2(State from, SymbolClass symClass, Symbol<Character> symbol, Indexable2<Symbol<Character>> it) {
        if (from == STR) {
            DFAExecutor ex = new DFAExecutor();
            it.back();
            List<Token> tokens1 = new ArrayList<>();
            ex.execute(it, SymbolClasses.translate, stringDFA, tokens1, true, true);
            tokens.add(new Token(TokenType.String, tokens1.size() > 0 ? tokens1.get(0).text : ""));
            return stringDFA.onFinish;
        }
        return null;
    }
    public void execute(Indexable2<Symbol<Character>> it, Function<Character, SymbolClass> translate, DFA dfa, List<Token> tokens, boolean debug, boolean stopOnFinal, Set<SymbolClass> breakOn) {
        State state = dfa.getStart();
        dfa.start(tokens);

        while (it.hasMore()) {
            Symbol<Character> sym = it.next();
            SymbolClass symClass = translate.apply(sym.val);
            if (breakOn.contains(symClass)) {
                dfa.process(state, null, null, null);
                return;
            }

            State special = dfa.handleSpecial(state, symClass, sym, it);
            State nextState = special == null ? dfa.next(state, symClass, sym) : special;
            if (nextState == parse2.States.INVALID) {
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
