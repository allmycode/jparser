package modeparser;

import java.util.EnumSet;
import java.util.Iterator;
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

    public void inc() {
        i++;
        col++;
        if (i < str.length() && str.charAt(i) == '\n') {
            row++;
            col = 0;
        }
    }

    public char getChar() {
        char c = str.charAt(i);
        return c;
    }


    private Set<State> beforeTagClose = EnumSet.of(TagName, TagName_, TagAttr, TagAttr_, TagAttrValue);
    private Set<State> trailSpace = EnumSet.of(TagStart, TagName_, TagAttr_, TagAttrValue, TagAttrEQ);
    private boolean gotSpace = false;

    public void parse() {
        state = Start;
        mode = Text;
        i = 0;
        char c = 0;
        State newState = null;
        while (i < str.length()) {
            c = getChar();
            if (state == Invalid) {
                throw new ParseException("Invalid state at [" + row + ":" + col +"] char '"+ getChar() + "'", row, col);
            }
            newState = Invalid;

            // special case SPACE
            boolean blankHandled = false;
            if (trailSpace.contains(state)) {
                if (isBlank(c)) {
                    if (!gotSpace) {
                        cleanBlankBuffer();
                    }
                    gotSpace = true;
                    putBlankBuffer(c);
                    blankHandled = true;
                } else {
                    if (gotSpace) {
                        appendBlankBuffer();
                    }
                }
            }
            if (!blankHandled) {

                // Before Tag Close States
                if (beforeTagClose.contains(state)) {
                    if (isSlash(c)) {
                        newState = TagStartSlash;
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
                    }
                    // Before Tag End
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
                    }
                    // Before Tag End
                } else if (state == TagAttrEQ) {
                    if (isAlnum(c)) {
                        newState = TagAttrValue;
                    }
                } else if (state == TagAttrValue) {
                    if (gotSpace) {
                        newState = TagName_;
                    } else if (isAlnum(c)) {
                        newState = TagAttrValue;
                    }
                    // Before Tag End
                } else if (state == TagEndSlash) {
                    if (isGT(c)) {
                        newState = TagEnd;
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
                if (state != newState) {
                    // leave state
                    switch (state) {
                        case TagStart:
                            if (mode == Text) {
                                appendTagstartBuffer();
                            }
                            break;
                        case TagName:
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
                            cleanTagstartBuffer();
                            break;
                        case TagName:
                            cleanTagnameBuffer();
                            break;
                        case TagAttr:
                            appendBlankBuffer3();
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
                        putTagstartBuffer(c);
                        break;

                    case TagAttr:
                        putTextBuffer(c);
                        break;

                    case TagAttrEQ:
                    case TagAttrValue:
                        putTextBuffer(c);
                        break;
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
            }
            inc();
        }
    }



    StringBuilder text = new StringBuilder();
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
            if (state == TagStart) {
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
