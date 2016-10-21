package modeparser;

import java.util.HashMap;
import java.util.Map;

import parse2.L2SymbolClasses;
import parse2.ParseException;
import parse2.SymbolClass;
import parse2.SymbolClasses;

public class TableDFAHelper {
    public static class StateTransitions {
        public final State from;
        public final Map<SymbolClass, State> transitions;

        public StateTransitions(State from, Map<SymbolClass, State> transitions) {
            this.from = from;
            this.transitions = transitions;
        }
    }

    public static class Transition {
        final SymbolClass sym;
        final State to;

        public Transition(SymbolClass sym, State to) {
            this.sym = sym;
            this.to = to;
        }
    }

    public static Map<State, Map<SymbolClass, State>> define(StateTransitions... sts) {
        Map<State, Map<SymbolClass, State>> res = new HashMap<>();
        for (StateTransitions o : sts) {
            res.put(o.from, o.transitions);
        }
        return res;
    }

    public static Transition $(SymbolClass sym, State to) {
        return new Transition(sym, to);
    }

    public static Transition $(State to) {
        return new Transition(null, to);
    }

    public static StateTransitions tr(State from, Transition... ots) {
        Map<SymbolClass, State> res = new HashMap<>();
        for (Transition o : ots) {
            if (o.sym == null) {
                for (SymbolClass sym : SymbolClasses.values()) {
                    if (res.get(sym) == null) {
                        res.put(sym, o.to);
                    }
                }
                continue;
            }
            if (o.sym instanceof L2SymbolClasses) {
                for (SymbolClass realSym : ((L2SymbolClasses) o.sym).symClasses) {
                    res.put(realSym, o.to);
                }
            } else {
                res.put(o.sym, o.to);
            }
        }
        return new StateTransitions(from, res);
    }

    public static State getNext(Map<State, Map<SymbolClass, State>> transitions, State from, SymbolClass by, int row, int col) {
        Map<SymbolClass, State> stateTransitions = transitions.get(from);
        if (stateTransitions == null) {
            throw new ParseException("No transitions from " + from, row, col);
        }
        return stateTransitions.get(by);
    }
}
