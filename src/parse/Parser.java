package parse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static parse.Parser.State.*;
import static parse.Parser.Sym.*;

public class Parser {

    private final boolean DEBUG = true;

    enum State {
        BAD,
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
        SVDQ,
        SVDQE,
        SVDQ_,
        SVQ,
        SVQE,
        SVQ_,
        STSL,
        STCSL,
        STCSL_,
        STCT,
        STCT_
    }

    enum Acc {
        TEXT (S0),
        COMMENT (S5, S6, S7),
        TAGNAME (ST),
        ATTRNAME(SA),
        ATTRVALUE(SV),
        ATTRVALUEDQ(SVDQ, SVDQE, SVDQ_),
        ATTRVALUEQ(SVQ, SVQE, SVQ_),
        TAGOPEN(S1),
        TAGCLOSE(S8),
        TAGOPENEND(STCSL),
        TAGCLOSEEND(STSL),
        TAGNAMEEND (STCT),
        ;

        public final State[] states;
        Acc(State ... states) {
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

    enum Sym {
        UNKNOWN,
        SPACE,
        LT,
        GT,
        EXCL,
        DASH,
        DQ,
        Q,
        EQ,
        BSL,
        SL,
        ALPHA,
        DIGIT
    }


    public Sym symSym(char c) {
        switch (c) {
            case ' ':
            case '\t':
            case '\r':
            case '\n':
                return Sym.SPACE;
            case '<':
                return Sym.LT;
            case '>':
                return Sym.GT;
            case '!':
                return Sym.EXCL;
            case '-':
                return Sym.DASH;
            case '"':
                return Sym.DQ;
            case '\'':
                return Sym.Q;
            case '=':
                return Sym.EQ;
            case '\\':
                return Sym.BSL;
            case '/':
                return Sym.SL;
        }
        if (Character.isAlphabetic(c)) {
            return Sym.ALPHA;
        }
        if (Character.isDigit(c)) {
            return Sym.DIGIT;
        }
        return Sym.UNKNOWN;
    }

    public Map<State, Map<Sym, State>> trsm = transitions(
            tr(S0, S0, p(LT, S1), p(GT, BAD)),
            tr(S1, BAD, p(SPACE, S1_), p(EXCL, S2), p(ALPHA, ST), p(SL, STCSL)),
            tr(S1_, BAD, p(SPACE, S1_), p(EXCL, S2), p(ALPHA, ST), p(SL, STCSL)),
            tr(S2, BAD, p(DASH, S3)),
            tr(S3, BAD, p(DASH, S4)),
            tr(S4, S5, p(DASH, S6)),
            tr(S5, S5, p(DASH, S6)),
            tr(S6, S5, p(DASH, S7)),
            tr(S7, S5, p(DASH, S7), p(GT, S8)),
            tr(S8, S0, p(LT, S1), p(GT, BAD)),
            tr(ST, BAD, p(ALPHA, ST), p(DIGIT, ST), p(SPACE, ST_), p(SL, STSL), p(GT, S8)),
            tr(ST_, BAD, p(ALPHA, SA), p(SPACE, ST_), p(SL, STSL), p(GT, S8)),
            tr(SA, BAD, p(ALPHA, SA), p(DIGIT, SA), p(SPACE, SA_), p(EQ, SAE), p(SL, STSL), p(GT, S8)),
            tr(SA_, BAD, p(SPACE, SA_), p(ALPHA, SA), p(EQ, SAE), p(SL, STSL), p(GT, S8)),
            tr(SAE, BAD, p(ALPHA, SV), p(DIGIT, SV), p(DQ, SVDQ), p(Q, SVQ)),
            tr(SV, BAD, p(ALPHA, SV), p(DIGIT, SV),p(SL, STSL), p(SPACE, ST_), p(GT, S8)),
            tr(SVDQ, SVDQ, p(DQ, SVDQ_), p(BSL, SVDQE)),
            tr(SVDQ_, BAD, p(SPACE, ST_), p(SL, STSL), p(GT, S8)),
            tr(SVDQE, BAD, p(DQ, SVDQ)),
            tr(SVQ, SVQ, p(Q, SVQ_), p(BSL, SVQE)),
            tr(SVQ_, BAD, p(SPACE, ST_), p(SL, STSL), p(GT, S8)),
            tr(SVQE, BAD, p(Q, SVQ)),
            tr(STSL, BAD, p(GT, S8)),
            tr(STCSL, BAD, p(SPACE, STCSL_), p(ALPHA, STCT)),
            tr(STCSL_, BAD, p(SPACE, STCSL_), p(ALPHA, STCT)),
            tr(STCT, BAD, p(ALPHA, STCT), p(DIGIT, STCT), p(SPACE, STCT_), p(GT, S8)),
            tr(STCT_, BAD, p(SPACE, STCT_), p(GT, S8))


    );

    static class OneStateTransitions {
        public final State from;
        public final Map<Sym, State> transitions;

        public OneStateTransitions(State from, Map<Sym, State> transitions) {
            this.from = from;
            this.transitions = transitions;
        }
    }

    public Map<State, Map<Sym, State>> transitions(OneStateTransitions ... sts) {
        Map<State, Map<Sym, State>> res = new HashMap<>();
        for (OneStateTransitions o : sts) {
            res.put(o.from, o.transitions);
        }
        return res;
    }

    static class OneTransition {
        final Sym sym;
        final State to;

        public OneTransition(Sym sym, State to) {
            this.sym = sym;
            this.to = to;
        }
    }

    public OneTransition p(Sym sym, State to) {
        return new OneTransition(sym, to);
    }

    public OneStateTransitions tr(State from, State def, OneTransition ... ots) {
        Map<Sym, State> res = new HashMap<>();
        for (OneTransition o : ots) {
            res.put(o.sym, o.to);
        }
        for (Sym s : Sym.values()) {
            if (!res.containsKey(s)) {
                res.put(s, def);
            }
        }
        return new OneStateTransitions(from, res);
    }

    static class Token {
        public final Acc type;
        public final String text;

        public Token(Acc type, String text) {
            this.type = type;
            this.text = text;
        }

        @Override
        public String toString() {
            return type + "{" + text + "}";
        }
    }

    public List<Token> parse(String str) {
        State s = S0;
        int row = 0;
        int col = 0;
        List<Token> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            col++;
            if (c == '\n') {
                row++;
                col = 0;
            }
            Sym cc = symSym(c);
            State n = trsm.get(s).get(cc);
            if (n == BAD) {
                throw new RuntimeException("Illegal char '" + c + "' (" + cc + ") at [" + row + ":" + col + "]. State now: " + s +", new: " + n + ". sb: " + sb);
            }
            if (DEBUG) {
                System.out.println(s + " -> " + n + " on '" + c + "'" + ". sb: " + sb);
            }
            Acc ss = accs.get(s);
            Acc ns = accs.get(n);
            if (ss != ns) {
                if (ss != null) {
                    if (sb.length() > 0) {
                        tokens.add(new Token(ss, sb.toString()));
                    }
                }
                sb.delete(0, sb.length());
            }
            sb.append(c);

            s = n;
        }
        Acc ss = accs.get(s);
        if (ss != null) {
            tokens.add(new Token(ss, sb.toString()));
        }
        return tokens;
    }

    public static void main(String[] args) throws IOException {
        Parser p = new Parser();

        //System.out.println(p.parse("prefix<!--hello there -->suffix<!--comm2-->"));

        //System.out.println(p.parse("<!--comment--><tag/>"));

        //System.out.println(p.parse("<!--comment--><tag a=b c d=1 g=\"hello there\" v2='sdf sdf'/>"));

        //System.out.println(p.parse("<tag1/><tag2 a=1></tag2>"));

        System.out.println(p.parse(toString(Parser.class.getClassLoader().getResourceAsStream("one.html"))));
    }

    public static String toString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStreamReader rd= new InputStreamReader(is);
        char buf[] = new char[1024];
        int readed;
        while ((readed = rd.read(buf)) > 0) {
            sb.append(buf, 0, readed);
        }
        return sb.toString();
    }

}
