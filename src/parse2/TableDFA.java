package parse2;

import java.util.HashMap;
import java.util.Map;

import static parse2.States.INVALID;

public abstract class TableDFA<T> implements DFA<T> {
    Map<State, StateTransitions> transitions;

    static class StateTransitions {
        public final State from;
        public final State def;
        public final Map<SymbolClass, State> transitions;

        public StateTransitions(State from, State def, Map<SymbolClass, State> transitions) {
            this.from = from;
            this.def = def;
            this.transitions = transitions;
        }
    }

    public static StateTransitions tr(State from, State def, Transition... ots) {
        Map<SymbolClass, State> res = new HashMap<>();
        for (Transition o : ots) {
            if (o.sym instanceof L2SymbolClasses) {
                for (SymbolClass realSym : ((L2SymbolClasses) o.sym).symClasses) {
                    res.put(realSym, o.to);
                }
            } else {
                res.put(o.sym, o.to);
            }
        }
        return new StateTransitions(from, def, res);
    }

    public static StateTransitions tr(State from, Transition... ots) {
        Map<SymbolClass, State> res = new HashMap<>();
        for (Transition o : ots) {
            if (o.sym instanceof L2SymbolClasses) {
                for (SymbolClass realSym : ((L2SymbolClasses) o.sym).symClasses) {
                    res.put(realSym, o.to);
                }
            } else {
                res.put(o.sym, o.to);
            }
        }
        return new StateTransitions(from, INVALID, res);
    }

    public void define(StateTransitions... sts) {
        Map<State, StateTransitions> res = new HashMap<>();
        for (StateTransitions o : sts) {
            res.put(o.from, o);
        }
        transitions = res;
    }

    static class Transition {
        final SymbolClass sym;
        final State to;

        public Transition(SymbolClass sym, State to) {
            this.sym = sym;
            this.to = to;
        }
    }

    public static Transition p(SymbolClass sym, State to) {
        return new Transition(sym, to);
    }

    @Override
    public State next(State from, SymbolClass symClass, Symbol sym) {
        StateTransitions stateTransitions = transitions.get(from);
        if (stateTransitions == null) {
            throw new ParseException("Transition from " + from + " by " + symClass + " is not defined.", sym.row, sym.col);
        }
        State to = stateTransitions.transitions.get(symClass);
        if (to == null) {
            to = stateTransitions.def;
        }
        return to;
    }
}
