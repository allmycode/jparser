package parse2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static parse2.L2SymbolClasses.ALNUM;
import static parse2.L2SymbolClasses.BLANK;
import static parse2.HTMLDFA.States.*;
import static parse2.States.INVALID;
import static parse2.TokenType.*;
import static parse2.SymbolClasses.*;

public class HTMLDFA extends TableDFA<Character> {
    private StringBuilder sb = new StringBuilder();;
    List<Token> tokens;

    enum States implements State {
        S00,
        S0,
        S1,
        S1_,
        S2,
        S3,
        S4,
        S5,
        S6,
        S7,
        S8,
        ST,
        ST_,
        SA,
        SA_,
        SAE,
        SV,
        STR,
        STR_,
        DSING_V,
        DSING_V2,
        DSING_OPEN,
        DSING_CLOSE,
        DSING_V_,
        STSL,
        STCSL,
        STCSL_,
        STCT,
        STCT_
    }

    enum Acc {
        TEXT (Text, S0),
        COMMENT (Comment, S5, S6, S7),
        TAGNAME (Tagname, ST),
        ATTRNAME(Attrname, SA),
        ATTRVALUE(Attrvalue, SV),
        TAGOPEN(OpenLT, S1),
        TAGCLOSE(CloseGT, S8),
        TAGOPENEND(FirstBackslash, STCSL),
        TAGCLOSEEND(LastBackslash, STSL),
        TAGNAMEEND (ClosingTagname, STCT),
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
    DQStringDFA stringDFA = new DQStringDFA(STR_);

    public HTMLDFA() {
        define( tr(S00, S0, p(LT, S1), p(GT, INVALID)),
                tr(S0, S0, p(LT, S1), p(GT, INVALID)),
                tr(S1, p(BLANK, S1_), p(EXCL, S2), p(ALPHA, ST), p(SLASH, STCSL)),
                tr(S1_, p(BLANK, S1_), p(EXCL, S2), p(ALPHA, ST), p(SLASH, STCSL)),
                tr(S2, p(DASH, S3)),
                tr(S3, p(DASH, S4)),
                tr(S4, S5, p(DASH, S6)),
                tr(S5, S5, p(DASH, S6)),
                tr(S6, S5, p(DASH, S7)),
                tr(S7, S5, p(DASH, S7), p(GT, S8)),
                tr(S8, S0, p(LT, S1), p(GT, INVALID)),
                tr(ST, p(ALNUM, ST), p(BLANK, ST_), p(SLASH, STSL), p(GT, S8)),
                tr(ST_, p(ALPHA, SA), p(BLANK, ST_), p(SLASH, STSL), p(GT, S8)),
                tr(SA, p(ALNUM, SA), p(BLANK, SA_), p(EQ, SAE), p(SLASH, STSL), p(GT, S8)),
                tr(SA_, p(BLANK, SA_), p(ALPHA, SA), p(EQ, SAE), p(SLASH, STSL), p(GT, S8)),
                tr(SAE, p(ALNUM, SV), p(DQ, STR), p(SQ, STR), p(DOLLAR, DSING_V)),
                tr(SV, p(ALNUM, SV),p(SLASH, STSL), p(BLANK, ST_), p(GT, S8)),
                tr(STR_, p(BLANK, ST_), p(SLASH, STSL), p(GT, S8)),
                tr(STSL, p(GT, S8)),
                tr(STCSL, p(BLANK, STCSL_), p(ALPHA, STCT)),
                tr(STCSL_, p(BLANK, STCSL_), p(ALPHA, STCT)),
                tr(STCT, p(ALNUM, STCT), p(BLANK, STCT_), p(GT, S8)),
                tr(STCT_, p(BLANK, STCT_), p(GT, S8)),
                tr(DSING_V, DSING_V2, p(CUR_OPEN, DSING_OPEN))
        );
    }

    @Override
    public State getStart() {
        return S00;
    }

    @Override
    public boolean isFinal(State state) {
        return state == S00 || state == S0 || state == S8;
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
        if (symbol != null) {
            sb.append(symbol.val);
        }
    }

    ExpressionDFA expressionDFA = new ExpressionDFA();
    @Override
    public State handleSpecial2(State from, SymbolClass symClass, Symbol<Character> symbol, Indexable2<Symbol<Character>> it) {
        if (from == STR) {
            DFAExecutor ex = new DFAExecutor();
            it.back();
            List<Token> tokens1 = new ArrayList<>();
            ex.execute(it, SymbolClasses.translate, stringDFA, tokens1, true, true);
            tokens.add(new Token(Attrvalue, tokens1.get(0).text));
            return stringDFA.onFinish;
        }
        if (from == DSING_V2) {
            it.back();
            List<Token> tokens1 = new ArrayList<>();
            Set<SymbolClass> breakSet = new HashSet<>();
            breakSet.add(LINE_BREAK);
            breakSet.add(SPACE);
            breakSet.add(DQ);
            breakSet.add(SQ);
            breakSet.add(GT);
            breakSet.add(LT);
            expressionDFA.execute(it, SymbolClasses.translate, expressionDFA, tokens1, true, false, breakSet);
            tokens.addAll(tokens1);
            it.back();
            return ST_;

        }
        if (from == DSING_OPEN) {
            List<Token> tokens1 = new ArrayList<>();
            Set<SymbolClass> breakSet = new HashSet<>();
            breakSet.add(CUR_CLOSE);
            expressionDFA.execute(it, SymbolClasses.translate, expressionDFA, tokens1, true, false, breakSet);
            tokens.addAll(tokens1);
            return ST_;

        }
        return null;
    }
}
