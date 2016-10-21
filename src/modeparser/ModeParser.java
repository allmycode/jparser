package modeparser;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import parse2.ParseException;
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
        TagStart,
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

    private State state;

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

    private Set<State> trailSpace = EnumSet.of(TagStart, TagStartSlash, TagName, TagAttr, TagAttrValue, TagAttrEQ);
    Map<State, State> of(State ... states) {
        Map<State, State> res = new EnumMap<>(State.class);
        for (int i = 0; i < states.length; i+=2) {
            res.put(states[i], states[i+1]);
        }
        return res;
    }
    private Map<State, State> trailSpaceMap = of(TagName, TagName_, TagAttr, TagAttr_, TagAttrEQ, TagAttrEQ_);
    private boolean gotSpace = false;
    public boolean handleSpace(char c) {
        // special case SPACE
        if (trailSpace.contains(state)) {
            if (isBlank(c)) {
                if (!gotSpace) {
                    cleanBlankBuffer();
                }
                gotSpace = true;
                putBlankBuffer(c);
                return true;
            } else {
                if (gotSpace) {
                    appendBlankBuffer();
                }
            }
        }
        return false;
    }

    private Set<State> beforeTagClose = EnumSet.of(TagName, TagAttr, TagAttrValue);


    public void parse() {
        state = Start;
        mode = Text;
        i = 0;
        char c = 0;
        State newState;
        while (i < str.length()) {
            c = str.charAt(i);
            if (state == Invalid) {
                throw new ParseException("Invalid state at [" + row + ":" + col +"] char '"+ c + "'", row, col);
            }
            newState = Invalid;

            if (handleSpace(c)) {
                advance();
                continue;
            }
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
                } else if (isSlash(c) ) {
                    newState = TagStartSlash;
                }
                // Before Tag End
            } else if (state == TagName) {
                if (gotSpace && newState == Invalid) {
                    newState = TagAttr;
                } else if (isAlnum_Col(c)) {
                    newState = TagName;
                } else if (isBlank(c) ) {
                    newState = TagAttr;
                }
                // Before Tag End
            } else if (state == TagName_) {
                if (isAlnum(c)) {
                    newState = TagAttr;
                }
                // Before Tag End
            } else if (state == TagAttr) {
                if (isAlnum(c)) {
                    newState = TagAttr;
                } else if (isEQ(c)) {
                    newState = TagAttrEQ;
                }
                // Before Tag End
            } else if (state == TagAttr_) {
                if (isAlnum(c)) {
                    newState = TagAttr;
                } else if (isEQ(c)) {
                    newState = TagAttrEQ;
                }
                // Before Tag End
            } else if (state == TagAttrEQ) {
                if (isAlnum(c)) {
                    newState = TagAttrValue;
                }
            } else if (state == TagAttrValue) {
                if (gotSpace) {
                    newState = TagAttr;
                } else if (isAlnum(c)) {
                    newState = TagAttrValue;
                }
                // Before Tag End
            } else if (state == TagEndSlash) {
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
                if (mode == OpenTag || mode == CloseTag) {
                    mode = Text;
                }
            } else if (state == C1) {
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
            }
            if (state != newState || gotSpace) {
                // leave state
                switch (state) {
                    case TagName:
                        if (mode == Text) {
                            appendTagstartBuffer();
                        }
                        String tagname = getTagname();
                        appendTagstartBuffer();
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
                        //if (state != TagStart && removeWhitespaces)
                        //    appendBlankBuffer3();
                        break;
                    case C1:
                    case C3:
                        cleanBlankBuffer();
                        break;

                }
                gotSpace = false;

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
                    if (!isBlank(c) && !gotSpace && removeWhitespaces) {
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


            state = newState;
            advance();
        }

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
            if (state == TagStart || state == TagName) {
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
