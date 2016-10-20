package parse;

import java.util.HashMap;
import java.util.Map;

public class DFA {
    public final IState start;
    public final Map<IState, StateTransitions> transitions;

    public DFA(IState start, Map<IState, StateTransitions> transitions) {
        this.start = start;
        this.transitions = transitions;
    }

    static class StateTransitions {
        public final IState from;
        public final IState def;
        public final Map<ISwitch, IState> transitions;

        public StateTransitions(IState from, IState def, Map<ISwitch, IState> transitions) {
            this.from = from;
            this.def = def;
            this.transitions = transitions;
        }
    }

    public static DFA define(IState state, StateTransitions... sts) {
        Map<IState, StateTransitions> res = new HashMap<>();
        for (StateTransitions o : sts) {
            res.put(o.from, o);
        }
        return new DFA(state, res);
    }

    static class Transition {
        final ISwitch sym;
        final IState to;

        public Transition(ISwitch sym, IState to) {
            this.sym = sym;
            this.to = to;
        }
    }

    public static Transition p(ISwitch sym, IState to) {
        return new Transition(sym, to);
    }

    public static StateTransitions tr(IState from, IState def, Transition... ots) {
        Map<ISwitch, IState> res = new HashMap<>();
        for (Transition o : ots) {
            res.put(o.sym, o.to);
        }
        return new StateTransitions(from, def, res);
    }

    public IState getTransition(IState from, ISwitch sym) {
        StateTransitions stateTransitions = transitions.get(from);
        if (stateTransitions == null) {
            throw new RuntimeException("Transition from " + from + " by " + sym + " is not defined");
        }
        IState to = stateTransitions.transitions.get(sym);
        if (to == null) {
            to = stateTransitions.def;
        }
        return to;
    }
}
