package modeparser;

import parse2.ParseException;
import parser.LexemNode;
import static modeparser.CharUtil.isBlank;
import static modeparser.CharUtil.isEQ;
import static modeparser.CharUtil.isGT;
import static modeparser.CharUtil.isLT;
import static modeparser.CharUtil.isSlash;
import static modeparser.CharUtil.isTagName;
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
        TagEndSlash,
        TagEnd,
        TagStartSlash,
        TagAttr,
        TagAttrValue,
        Content,
        ;
    }

    private State state;

    private int i;
    private int row;
    private int col;
    private String str;

    public StringBuilder text = new StringBuilder();

    private LexemNode current;

    public ModeParser(String str, boolean removeWhitespaces) {
        this.str = str;
        this.removeWhitespaces = removeWhitespaces;
    }

    public void clean() {
        text.delete(0, text.length());
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
    public char getCharInc() {
        char c = str.charAt(i++);
        return c;
    }

    public void parse() {
        state = Start;
        mode = Text;
        i = 0;
        while (i < str.length()) {
            if (state == Invalid) {
                throw new ParseException("Invalid state at [" + row + ":" + col +"] char '"+ getChar() + "'", row, col);
            }
            if (state == Start) {
                skipBlank();
                char c = getChar();
                if (isLT(c)) {
                    state = TagStart;
                    inc();
                } else {
                    state = Invalid;
                }

            } else if (state == TagStart) {
                char c = getChar();
                if (isSlash(c)) {
                    state = TagStartSlash;
                    inc();
                } else {
                    String blank = skipBlank();
                    String tagname = readTagName();
                    if (tagname.startsWith("ui:")) {
                        mode = OpenTag;
                    }
                    if (mode == Text) {
                        text.append('<');
                        if (!removeWhitespaces) text.append(blank);
                        text.append(tagname);
                    }
                    state = TagAttr;
                }
            } else if (state == TagStartSlash) {
                String blank = skipBlank();
                String tagname = readTagName();
                if (tagname.startsWith("ui:")) {
                    mode = CloseTag;
                }
                if (mode == Text) {
                    text.append("</");
                    if (!removeWhitespaces) text.append(blank);
                    text.append(tagname);
                }
                blank = skipBlank();
                if (mode == Text) {
                    if (!removeWhitespaces) text.append(blank);
                }
                char c = getChar();
                if (isGT(c)) {
                    state = TagEnd;
                } else {
                    state = Invalid;
                }

            } else if (state == TagEndSlash) {
                inc();
                char c = getChar();
                if (isGT(c)) {
                    state = TagEnd;
                } else {
                    state = Invalid;
                }
            } else if (state == TagEnd) {
                char c = getChar();
                if (mode == Text) {
                    text.append(c);
                }
                if (mode == OpenTag || mode == CloseTag) {
                    mode = Text;
                }
                state = Content;
                inc();
            } else if (state == Content) {
                String blank = skipBlank();
                if (mode == Text) {
                    if (!removeWhitespaces) text.append(blank);
                }
                while (i < str.length()) {
                    blank = skipBlank();
                    char c = getChar();
                    if (isLT(c)) {
                        if (mode == Text) {
                            if (!removeWhitespaces) text.append(blank);
                        }
                        state = TagStart;
                        inc();
                        break;
                    } else {
                        if (mode == Text) {
                            text.append(blank);
                            text.append(c);
                        }
                        inc();
                    }
                }

            } else if (state == TagAttr){
                String blank = skipBlank();
                char c = getChar();
                if (isSlash(c)) {
                    state = TagEndSlash;
                    if (mode == Text) {
                        if (!removeWhitespaces)
                            text.append(blank);
                    }
                    if (mode == Text) {
                        text.append(c);
                    }
                } else if (isGT(c)) {
                    if (mode == Text) {
                        if (!removeWhitespaces)
                            text.append(blank);
                    }
                    state = TagEnd;
                } else {
                    if (mode == Text) {
                        if (!removeWhitespaces)
                            text.append(blank);
                        else if (blank.length() > 0)
                            text.append(' ');
                    }
                    String attrName = readTagName();
                    if (mode == Text) {
                        text.append(attrName);
                    }
                    blank = skipBlank();
                    c = getChar();
                    if (isEQ(c)) {
                        if (mode == Text) {
                            if (!removeWhitespaces)
                                text.append(blank);
                        }
                        if (mode == Text) {
                            text.append(c);
                        }
                        state = TagAttrValue;
                        inc();
                    } else {
                        if (mode == Text) {
                            if (!removeWhitespaces)
                                text.append(blank);
                            else if (blank.length() > 0)
                                text.append(' ');
                        }
                        state = TagAttr;
                    }
                }
            } else if (state == TagAttrValue) {
                iSkipBlank();
                String attrValue = readTagName();
                if (mode == Text) {
                    text.append(attrValue);
                }
                state = TagAttr;
            }

            else {
                inc();
            }
        }
    }

    private String readTagName() {
        char c;
        int start = i;
        while (i < str.length() && isTagName(c = str.charAt(i))) {
            i++;
        }
        return str.substring(start, i);
    }

    public String skipBlank() {
        char c;
        int start = i;
        while (i < str.length() && isBlank(c = str.charAt(i))) {
            i++;
        }
        return str.substring(start, i);
    }

    public void iSkipBlank() {
        String blank = skipBlank();
        if (mode == Text && !removeWhitespaces) {
            text.append(blank);
        }
    }

    public void iSkipBlankOne() {
        String blank = skipBlank();
        if (mode == Text) {
            if (!removeWhitespaces)
                text.append(blank);
            else if (blank.length() > 0)
                text.append(' ');
        }
    }


}
