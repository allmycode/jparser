package parse;

import static parse.DFA.*;
import static parse.Predefined.State.BAD;
import static parse.StringDFA.State.*;
import static parse.StringDFA.Sym.*;

public class StringDFA implements ISymTranslator<Character, ISwitch>{
    DFA dfa;

    public StringDFA(IState finish) {
        dfa = define(START,
                    tr(START, BAD, p(DQ, SVDQ)),
                    tr(SVDQ, SVDQ, p(DQ, finish), p(BSL, SVDQE)),
                    tr(SVDQE, BAD, p(DQ, SVDQ)));
    }

    enum State implements IState {
        START,
        SVDQ,
        SVDQE,
    }
    enum Sym implements ISwitch {
        ANY,
        DQ,
        BSL,
    }

    public Sym translate(Character c) {
        switch (c) {
            case '"':
                return DQ;
            case '\\':
                return BSL;
        }
        return ANY;
    }

}
