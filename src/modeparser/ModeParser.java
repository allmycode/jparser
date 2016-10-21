package modeparser;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parse2.L2SymbolClasses;
import parse2.ParseException;
import parse2.SymbolClass;
import parse2.SymbolClasses;
import parser.LexemNode;
import static modeparser.CharUtil.isAlnum;
import static modeparser.CharUtil.isBlank;
import static modeparser.CharUtil.isEQ;
import static modeparser.CharUtil.isGT;
import static modeparser.CharUtil.isLT;
import static modeparser.CharUtil.isSlash;
import static modeparser.CharUtil.isAlnum_Col;
import static modeparser.CharUtil.isStringBound;
import static modeparser.ModeParser.Mode.*;
import static modeparser.ModeParser.State.*;

import static parse2.L2SymbolClasses.*;
import static parse2.SymbolClasses.*;

public class ModeParser {
    enum Mode {
        Text,
        OpenTag,
        CloseTag,
        OpenStatement,
        CloseStatement,
        Expression
    }

    private Mode mode;

    public enum State {
        Invalid,
        Start,
        EOF,
        TagStart,
        TagStart_,
        TagName,
        TagName_,
        TagEndSlash,
        TagEnd,
        TagStartSlash,
        TagAttr,
        TagAttr_,
        TagAttrEQ,
        TagAttrEQ_,
        TagAttrValue,
        TagAttrVString,
        TagAttrVString_,
        TagAttrVString__,
        C1,
        C2,
        C3
        ;
    }
    public boolean removeWhitespaces;

    private boolean stringSubmode;
    private char stringBound;
    private char stringEscape;

    State state;
    State newState;

    private int i;
    private int row;
    private int col;
    private String str;

    private LexemNode current;

    public ModeParser(String str, boolean removeWhitespaces) {
        this.str = str;
        this.removeWhitespaces = removeWhitespaces;
    }

    static class StateTransitions {
        public final State from;
        public final Map<SymbolClass, State> transitions;

        public StateTransitions(State from, Map<SymbolClass, State> transitions) {
            this.from = from;
            this.transitions = transitions;
        }
    }
    public Map<State, Map<SymbolClass, State>> define(StateTransitions... sts) {
        Map<State, Map<SymbolClass, State>> res = new HashMap<>();
        for (StateTransitions o : sts) {
            res.put(o.from, o.transitions);
        }
        return res;
    }

    static class Transition {
        final SymbolClass sym;
        final State to;

        public Transition(SymbolClass sym, State to) {
            this.sym = sym;
            this.to = to;
        }
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

    Map<State, Map<SymbolClass, State>> transitions = define(
            tr(Start,
                    $(LT, TagStart)),
            tr(TagStart,
                    $(ALNUM_COL, TagName),
                    $(SLASH, TagStartSlash),
                    $(BLANK, TagStart_)),
            tr(TagStart_,
                    $(ALNUM_COL, TagName),
                    $(SLASH, TagStartSlash),
                    $(BLANK, TagStart_)),
            tr (TagName,
                    $(ALNUM_COL, TagName),
                    $(SLASH, TagEndSlash),
                    $(GT, TagEnd),
                    $(BLANK, TagName_)),
            tr (TagName_,
                    $(ALNUM_COL, TagAttr),
                    $(SLASH, TagEndSlash),
                    $(GT, TagEnd),
                    $(BLANK, TagName_)),
            tr (TagAttr,
                    $(ALNUM_, TagAttr),
                    $(EQ, TagAttrEQ),
                    $(SLASH, TagEndSlash),
                    $(GT, TagEnd),
                    $(BLANK, TagAttr_)),
            tr (TagAttr_,
                    $(ALNUM_, TagAttr),
                    $(EQ, TagAttrEQ),
                    $(SLASH, TagEndSlash),
                    $(GT, TagEnd),
                    $(BLANK, TagAttr_)),
            tr (TagAttrEQ,
                    $(ALNUM_, TagAttrValue),
                    $(SQ, TagAttrVString),
                    $(DQ, TagAttrVString),
                    $(BLANK, TagAttrEQ_)),
            tr (TagAttrEQ_,
                    $(ALNUM_, TagAttrValue),
                    $(SQ, TagAttrVString),
                    $(DQ, TagAttrVString),
                    $(BLANK, TagAttrEQ_)),
            tr (TagAttrValue,
                    $(ALNUM_, TagAttrValue),
                    $(SLASH, TagEndSlash),
                    $(GT, TagEnd),
                    $(BLANK, TagAttr_)),
            // String TagAttrVString treated specially
            tr (TagAttrVString,
                    $(TagAttrVString)),
            tr (TagAttrVString_,
                    $(SLASH, TagEndSlash),
                    $(GT, TagEnd),
                    $(BLANK, TagAttrVString__)),
            tr (TagAttrVString__,
                    $(ALNUM_, TagAttr),
                    $(SLASH, TagEndSlash),
                    $(GT, TagEnd),
                    $(BLANK, TagAttrVString__)),
            tr(TagEndSlash,
                    $(GT, TagEnd)),
            // Maybe need blank
            tr(TagStartSlash,
                    $(ALNUM_COL, TagName)),
            tr(TagEnd,
                    $(LT, TagStart),
                    $(BLANK, C1),
                    $(C2)),
            tr(C1,
                    $(LT, TagStart),
                    $(BLANK, C1),
                    $(C2)),
            tr(C2,
                    $(LT, TagStart),
                    $(BLANK, C3),
                    $(C2)),
            tr(C3,
                    $(LT, TagStart),
                    $(BLANK, C3),
                    $(C2))
    );

    public State getNext(State from, SymbolClass by) {
        Map<SymbolClass, State> stateTransitions = transitions.get(from);
        if (stateTransitions == null) {
            throw new ParseException("No transitions from " + from, row, col);
        }
        return stateTransitions.get(by);
    }

    public void advance() {
        i++;
        col++;
        if (i < str.length() && str.charAt(i) == '\n') {
            row++;
            col = 0;
        }
    }
    private Set<State> beforeTagClose = EnumSet.of(TagName, TagName_, TagAttr, TagAttr_, TagAttrValue, TagAttrVString_, TagAttrVString__);
    TokenRange ct;

    public List<TokenRange> tokens = new ArrayList<>();

    public void runActions(char c) {
        if (state != newState && !(state == TagAttrVString && newState == TagAttrVString_)) {
            if (ct != null) {
                ct.end = i;
                tokens.add(ct);
            }
            ct = new TokenRange(newState, i, i);
            // leave state
            switch (state) {
                case TagStart_:
                case TagName_:
                case TagAttr_:
                case TagAttrEQ_:
                    appendBlankBuffer();
                    break;

                case TagName:
                    String tagname = getTagname();
                    appendTagstartBuffer();
                    break;
                case TagEnd:
                    if (mode == OpenTag || mode == CloseTag) {
                        mode = Text;
                    }
                    break;
                case C1:
                    appendBlankBuffer();
                    break;
                case C3:
                    if (newState == C2) {
                        appendBlankBuffer2();
                    }
                    break;

            }
            // - - - - - -
            // enter state
            switch (newState) {
                case TagStart:
                    appendBlankBuffer();
                    cleanTagstartBuffer();
                    break;
                case TagStart_:
                case TagName_:
                case TagAttr_:
                case TagAttrEQ_:
                    cleanBlankBuffer();
                    break;
                case TagName:
                    cleanTagnameBuffer();
                    break;
                case TagAttr:
                    appendBlankBuffer();
                    break;
                case C1:
                case C3:
                    cleanBlankBuffer();
                    break;

                case TagAttrVString:
                    stringBound = c;
                    break;

            }
        }
        // in state
        switch (newState) {
            case TagStart_:
            case TagName_:
            case TagAttr_:
            case TagAttrEQ_:
                putBlankBuffer(c);
                break;
            case TagName:
                putTagnameBuffer(c);
            case TagStart:
            case TagStartSlash:
                putTagstartBuffer(c);
                break;

            case TagAttr:
                if (!isBlank(c) && removeWhitespaces) {
                    putTextBuffer(' ');
                }
                if (!isBlank(c)) {
                    putTextBuffer(c);
                }
                break;
            case TagAttrEQ:
            case TagAttrValue:
            case TagEndSlash:
            case TagEnd:
                putTextBuffer(c);
                break;

            case C1:
                putBlankBuffer(c);
                break;
            case C2:
                putTextBuffer(c);
                break;
            case C3:
                putBlankBuffer(c);
                break;
        }
    }

    public void parse() {
        state = Start;
        mode = Text;
        i = 0;
        char c = 0;
        while (i < str.length()) {
            c = str.charAt(i);
            SymbolClass symClass = SymbolClasses.translate.apply(c);
            newState = Invalid;

            if (state == TagAttrVString) {
                if (stringEscape > 0) {
                    if (c == 'n') {
                        c = '\n';
                    }
                    if (c == 'r') {
                        c = '\r';
                    }
                    if (c == 't') {
                        c = '\t';
                    }
                } else {
                    if (c == stringBound) {
                        newState = TagAttrVString_;
                    } else if (c == '\\') {
                        stringEscape = c;
                    }
                }
            }
            State candidate = getNext(state, symClass);
            if (candidate != null && newState == Invalid) {
                newState = candidate;
            }

            /*
            // Before Tag Close States
            if (beforeTagClose.contains(state)) {
                if (isSlash(c)) {
                    newState = TagEndSlash;
                } else if (isGT(c)) {
                    newState = TagEnd;
                }
            }

            if (state == Start) {
                if (isLT(c)) {
                    newState = TagStart;
                }
            } else if (state == TagStart) {
                if (isAlnum_Col(c)) {
                    newState = TagName;
                } else if (isSlash(c)) {
                    newState = TagStartSlash;
                } else if (isBlank(c)) {
                    newState = TagStart_;
                }
            }
            else if (state == TagStart_) {
                if (isAlnum_Col(c)) {
                    newState = TagName;
                } else if (isSlash(c)) {
                    newState = TagStartSlash;
                } else if (isBlank(c)) {
                    newState = TagStart_;
                }
            } else if (state == TagName) {
                if (isAlnum_Col(c)) {
                    newState = TagName;
                } else if (isBlank(c)) {
                    newState = TagName_;
                }
                // Before Tag End
            } else if (state == TagName_) {
                if (isAlnum(c)) {
                    newState = TagAttr;
                } else if (isBlank(c)) {
                    newState = TagName_;
                }
                // Before Tag End
            } else if (state == TagAttr) {
                if (isAlnum(c)) {
                    newState = TagAttr;
                } else if (isEQ(c)) {
                    newState = TagAttrEQ;
                } else if (isBlank(c)) {
                    newState = TagAttr_;
                }
                // Before Tag End
            } else if (state == TagAttr_) {
                if (isAlnum(c)) {
                    newState = TagAttr;
                } else if (isEQ(c)) {
                    newState = TagAttrEQ;
                } else if (isBlank(c)) {
                    newState = TagAttr_;
                }
                // Before Tag End
            } else if (state == TagAttrEQ) {
                if (isAlnum(c)) {
                    newState = TagAttrValue;
                } else if (isStringBound(c)) {
                    newState = TagAttrVString;
                } else if (isBlank(c)) {
                    newState = TagAttrEQ_;
                }
            } else if (state == TagAttrEQ_) {
                if (isAlnum(c)) {
                    newState = TagAttrValue;
                } else if (isStringBound(c)) {
                    newState = TagAttrVString;
                } else if (isBlank(c)) {
                    newState = TagAttrEQ_;
                }
            } else if (state == TagAttrValue) {
                if (isAlnum(c)) {
                    newState = TagAttrValue;
                } else if (isBlank(c)) {
                    newState = TagAttr_;
                }
                // Before Tag End
            } else if (state == TagAttrVString) {
                if (c != stringBound) {
                    newState = TagAttrVString;
                }
                // Before Tag End
            } else if (state == TagAttrVString_) {
                if (isBlank(c)) {
                    newState = TagAttrVString__;
                }
                // Before Tag End
            }
            else if (state == TagAttrVString__) {
                if (isBlank(c)) {
                    newState = TagAttrVString__;
                } else if (isAlnum(c)) {
                    newState = TagAttr;
                }
                // Before Tag End
            }else if (state == TagEndSlash) {
                if (isGT(c)) {
                    newState = TagEnd;
                }
            } else if (state == TagStartSlash) {
                if (isAlnum_Col(c)) {
                    newState = TagName;
                }
            } else if (state == TagEnd) {
                if (isBlank(c)) {
                    newState = C1;
                } else if (isLT(c)) {
                    newState = TagStart;
                } else {
                    newState = C2;
                }
            }
            // Text
            if (state == C1) {
                if (isBlank(c)) {
                    newState = C1;
                } else if (isLT(c)) {
                    newState = TagStart;
                } else {
                    newState = C2;
                }
            } else if (state == C2) {
                if (isBlank(c)) {
                    newState = C3;
                } else if (isLT(c)) {
                    newState = TagStart;
                } else {
                    newState = C2;
                }
            } else if (state == C3) {
                if (isBlank(c)) {
                    newState = C3;
                } else if (isLT(c)) {
                    newState = TagStart;
                } else {
                    newState = C2;
                }
            }*/
            runActions(c);


            state = newState;
            if (state == Invalid) {
                throw new ParseException("Invalid state at [" + row + ":" + col +"] char '"+ c + "'", row, col);
            }
            advance();
        }
        newState = EOF;
        runActions('Z');

    }



    public StringBuilder text = new StringBuilder();
    void cleanTextBuffer() {
        text.delete(0, text.length());
    }
    void putTextBuffer(char c) {
        if (mode == Text) {
            text.append(c);
        }
    }

    StringBuilder blankBuffer = new StringBuilder();
    void cleanBlankBuffer() {
        blankBuffer.delete(0, blankBuffer.length());
    }
    void putBlankBuffer(char c) {
        if (mode == Text) {
            blankBuffer.append(c);
        }
    }
    void appendBlankBuffer() {
        if (mode == Text && !removeWhitespaces) {
            if (state == TagStart || state == TagStart_ || state == TagName) {
                tagstartBuffer.append(blankBuffer);
            } else {
                text.append(blankBuffer);
            }
            cleanBlankBuffer();
        }
    }
    void appendBlankBuffer2() {
        text.append(blankBuffer);
        cleanBlankBuffer();
    }

    void appendBlankBuffer3() {
        if (mode == Text) {
            if (!removeWhitespaces) {
                text.append(' ');
            } else {
                text.append(' ');
            }
        }
        cleanBlankBuffer();
    }

    StringBuilder tagstartBuffer = new StringBuilder();
    void cleanTagstartBuffer() {
        tagstartBuffer.delete(0, tagstartBuffer.length());
    }
    void putTagstartBuffer(char c) {
        if (mode == Text) {
            tagstartBuffer.append(c);
        }
    }
    void appendTagstartBuffer() {
        if (mode == Text) {
            text.append(tagstartBuffer);
            cleanTagstartBuffer();
        }
    }

    StringBuilder tagnameBuffer = new StringBuilder();
    void cleanTagnameBuffer() {
        tagnameBuffer.delete(0, tagnameBuffer.length());
    }
    void putTagnameBuffer(char c) {
        if (mode == Text) {
            tagnameBuffer.append(c);
        }
    }
    String getTagname() {
        return tagnameBuffer.toString();
    }
}
