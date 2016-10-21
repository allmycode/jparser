package modeparser;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parse2.ParseException;
import parse2.Token;
import parser.LexemNode;
import static modeparser.CharUtil.isAlnum;
import static modeparser.CharUtil.isBlank;
import static modeparser.CharUtil.isEQ;
import static modeparser.CharUtil.isGT;
import static modeparser.CharUtil.isLT;
import static modeparser.CharUtil.isSlash;
import static modeparser.CharUtil.isAlnum_Col;
import static modeparser.ModeParser.Mode.*;
import static modeparser.ModeParser.State.*;
public class ModeParser {

    public boolean removeWhitespaces;

    private boolean stringSubmode;
    private char stringBound;

    enum Mode {
        Text,
        OpenTag,
        CloseTag,
        OpenStatement,
        CloseStatement,
        Expression
    }

    private Mode mode;

    enum State {
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
        C1,
        C2,
        C3
        ;
    }

    State state;
    State lookupState;
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

    public void advance() {
        i++;
        col++;
        if (i < str.length() && str.charAt(i) == '\n') {
            row++;
            col = 0;
        }
    }

    Map<State, State> of(State ... states) {
        Map<State, State> res = new EnumMap<>(State.class);
        for (int i = 0; i < states.length; i+=2) {
            res.put(states[i], states[i+1]);
        }
        return res;
    }
    private Map<State, State> trailSpaceMap = of(TagStart, TagStart_, TagName, TagName_, TagAttr, TagAttr_, TagAttrEQ, TagAttrEQ_, TagAttrValue, TagAttr);
    private Map<State, State> trailSpaceBackMap = of(TagStart_, TagStart, TagName_, TagAttr, TagAttr_, TagAttr, TagAttrEQ_, TagAttrValue);

    public boolean handleSpace(char c) {
        // special case SPACE
        if (trailSpaceMap.containsKey(state) || trailSpaceMap.containsValue(state)) {
            if (isBlank(c)) {
                if (trailSpaceMap.containsKey(state)) {
                    newState = trailSpaceMap.get(state);
                    cleanBlankBuffer();
                } else {
                    newState = state;
                }
                putBlankBuffer(c);
                return true;
            } else {
                if (trailSpaceBackMap.containsKey(state)) {
                    appendBlankBuffer();
                    //State sv = newState;
                    lookupState = trailSpaceBackMap.get(state);
                    //runActions(c);
                    //state = newState;
                    //newState = sv;
                }
            }
        }
        return false;
    }

    private Set<State> beforeTagClose = EnumSet.of(TagName, TagAttr, TagAttrValue);
    TokenRange ct;

    public List<TokenRange> tokens = new ArrayList<>();

    public void runActions(char c) {
        if (state != newState) {
            if (ct != null) {
                ct.end = i;
                tokens.add(ct);
            }
            ct = new TokenRange(newState, i, i);
            // leave state
            switch (state) {
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

            }
        }
        // in state
        switch (newState) {
            case TagName:
                putTagnameBuffer(c);
            case TagStart:
            case TagStartSlash:
                putTagstartBuffer(c);
                break;

            case TagAttr:
                if (!isBlank(c)  && removeWhitespaces) {
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
            lookupState = state;
            newState = Invalid;

            if (!handleSpace(c)) {

                // Before Tag Close States
                if (beforeTagClose.contains(lookupState)) {
                    if (isSlash(c)) {
                        newState = TagEndSlash;
                    } else if (isGT(c)) {
                        newState = TagEnd;
                    }
                }

                if (lookupState == Start) {
                    if (isLT(c)) {
                        newState = TagStart;
                    }
                } else if (lookupState == TagStart) {
                    if (isAlnum_Col(c)) {
                        newState = TagName;
                    } else if (isSlash(c)) {
                        newState = TagStartSlash;
                    }
                    // Before Tag End
                } else if (lookupState == TagName) {
                    if (isAlnum_Col(c)) {
                        newState = TagName;
                    } else if (isBlank(c)) {
                        newState = TagAttr;
                    }
                    // Before Tag End
                } else if (lookupState == TagAttr) {
                    if (isAlnum(c)) {
                        newState = TagAttr;
                    } else if (isEQ(c)) {
                        newState = TagAttrEQ;
                    }
                    // Before Tag End
                } else if (lookupState == TagAttrEQ) {
                    if (isAlnum(c)) {
                        newState = TagAttrValue;
                    }
                } else if (lookupState == TagAttrValue) {
                    if (isAlnum(c)) {
                        newState = TagAttrValue;
                    }
                    // Before Tag End
                } else if (lookupState == TagEndSlash) {
                    if (isGT(c)) {
                        newState = TagEnd;
                    }
                } else if (lookupState == TagStartSlash) {
                    if (isAlnum_Col(c)) {
                        newState = TagName;
                    }
                } else if (lookupState == TagEnd) {
                    if (isBlank(c)) {
                        newState = C1;
                    } else if (isLT(c)) {
                        newState = TagStart;
                    } else {
                        newState = C2;
                    }
                } else if (lookupState == C1) {
                    if (isBlank(c)) {
                        newState = C1;
                    } else if (isLT(c)) {
                        newState = TagStart;
                    } else {
                        newState = C2;
                    }

                } else if (lookupState == C2) {
                    if (isBlank(c)) {
                        newState = C3;
                    } else if (isLT(c)) {
                        newState = TagStart;
                    } else {
                        newState = C2;
                    }
                } else if (lookupState == C3) {
                    if (isBlank(c)) {
                        newState = C3;
                    } else if (isLT(c)) {
                        newState = TagStart;
                    } else {
                        newState = C2;
                    }
                }
            }
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
