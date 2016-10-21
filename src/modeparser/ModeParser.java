package modeparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import parse2.ParseException;
import parse2.SymbolClass;
import parse2.SymbolClasses;
import parser.Attribute;
import parser.LexemNode;
import parser.LexemType;
import static modeparser.CharUtil.isBlank;
import static modeparser.ModeParser.Mode.*;
import static modeparser.State.*;

import static modeparser.TableDFAHelper.*;
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

    public boolean removeWhitespaces;

    private boolean stringSubmode;
    private char stringBound;
    private char stringEscape;

    // State variables
    State state;
    State newState;

    // Mode variables
    private Mode mode;
    private Mode newMode;

    // Lexem tree
    public LexemNode rootLexemNode = new LexemNode(LexemType.Root, "");
    public LexemNode currentLexemNode = rootLexemNode;

    // Token ranges
    public List<TokenRange> tokens = new ArrayList<>();
    TokenRange currentTokenRange;

    private int i;
    private int row;
    private int col;
    private String str;

    public ModeParser(String str, boolean removeWhitespaces) {
        this.str = str;
        this.removeWhitespaces = removeWhitespaces;
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
            tr (TagAttrVString,
                    // String bounds and escape treated specially
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
            tr(TagStartSlash,
                    $(ALNUM_COL, TagName),
                    $(BLANK, TagStartSlash_)),
            tr(TagStartSlash_,
                    $(ALNUM_COL, TagName),
                    $(BLANK, TagStartSlash_)),
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

    public void parse() {
        state = Start;
        mode = Text;
        i = 0;
        char c = 0;
        while (i < str.length()) {
            c = str.charAt(i);
            SymbolClass symClass = SymbolClasses.translate.apply(c);
            newState = Invalid;
            newMode = null;

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
                    stringEscape = 0;
                } else {
                    if (c == stringBound) {
                        newState = TagAttrVString_;
                    } else if (c == '\\') {
                        stringEscape = c;
                    }
                }
            }

            if (newState == Invalid) {
                newState = getNext(transitions, state, symClass, row, col);
            }

            processStateTransition(c, state, newState);
            state = newState;

            //processModeTransition();
            //if (newMode != null) {
            //    mode = newMode;
           // }

            if (state == Invalid) {
                throw new ParseException("Invalid state at [" + row + ":" + col +"] char '"+ c + "'", row, col);
            }

            i++;
            col++;
            if (i < str.length() && str.charAt(i) == '\n') {
                row++;
                col = 0;
            }
        }
        processStateTransition('Z', state, EOF);

        // Final mode
        if (mode == Text) {
            pushLexemNode(new LexemNode(LexemType.Static, text.toString()));
        }

    }

    public void processStateTransition(char c, State state, State newState) {
        if (state != newState && !(state == TagAttrVString && newState == TagAttrVString_)) {
            if (currentTokenRange != null) {
                currentTokenRange.end = i;
                tokens.add(currentTokenRange);
            }
            currentTokenRange = new TokenRange(newState, i, i);
            // leave state
            switch (state) {
                // BLANK STATES
                case TagStart_:
                case TagName_:
                case TagAttr_:
                case TagAttrEQ_:
                case TagAttrVString__:
                case TagStartSlash_:
                    appendBlankBuffer();
                    break;
                // BLANK STATES END

                case TagName:
                    String tagname = getTagname();
                    if (tagname.startsWith("ui:")) {
                        customTagname = tagname;
                        processModeTransition(OpenTag);
                    } else {
                        appendTagstartBuffer();
                    }
                    break;
                case TagEnd:
                    if (mode == OpenTag || mode == CloseTag) {
                        processModeTransition(Text);
                    }
                    break;

                case TagAttrValue:
                case TagAttrVString_:
                    System.out.println("Found attribute:" + attributeBuffer + " = " + valueBuffer);
                    attributeBuffer = new StringBuilder();
                    valueBuffer = new StringBuilder();
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
                // BLANK STATES
                case TagStart_:
                case TagName_:
                case TagAttr_:
                case TagAttrEQ_:
                case TagAttrVString__:
                case TagStartSlash_:
                    cleanBlankBuffer();
                    break;
                // BLANK STATES END

                case TagStart:
                    appendBlankBuffer();
                    cleanTagstartBuffer();
                    break;
                case TagName:
                    cleanTagnameBuffer();
                    break;
                case TagAttr:
                    if (attributeBuffer.length() > 0) {
                        System.out.println("Found attribute:" + attributeBuffer);
                    }
                    attributeBuffer = new StringBuilder();
                    attributeBuffer = new StringBuilder();
                    appendBlankBuffer();
                    break;
                case C1:
                case C3:
                    cleanBlankBuffer();
                    break;

                case TagAttrVString:
                    stringBound = c;
                    valueBuffer = new StringBuilder();
                    break;

                case TagAttrValue:
                    valueBuffer = new StringBuilder();
                    break;


            }
        }
        // in state
        switch (newState) {
            // BLANK STATES
            case TagStart_:
            case TagName_:
            case TagAttr_:
            case TagAttrEQ_:
            case TagAttrVString__:
            case TagStartSlash_:
                putBlankBuffer(c);
                break;
            // BLANK STATES END

            case TagName:
                putTagnameBuffer(c);
            case TagStart:
            case TagStartSlash:
                putTagstartBuffer(c);
                break;

            case TagEndSlash:
                putTagstartBuffer(c);
                putTextBuffer(c);
                break;


            case TagAttr:
                if (!isBlank(c) && removeWhitespaces) {
                    appendBlankBuffer3();
                }
                if (!isBlank(c)) {
                    putTextBuffer(c);
                }
                attributeBuffer.append(c);
                break;
            case TagAttrEQ:
            case TagEnd:
                putTextBuffer(c);
                break;

            case TagAttrValue:
                putTextBuffer(c);
                valueBuffer.append(c);
                break;

            case TagAttrVString:
                if (stringEscape == 0) {
                    putTextBuffer(c);
                    valueBuffer.append(c);
                }
                break;

            case TagAttrVString_:
                putTextBuffer(c);
                valueBuffer.append(c);
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

    void processModeTransition(Mode newMode) {
        if (newMode != null) {
            if (mode != newMode) {
                if (mode == Text && newMode == OpenTag) {
                    currentLexemNode.add(new LexemNode(LexemType.Static, text.toString()));
                    cleanTextBuffer();
                    pushLexemNode(new LexemNode(LexemType.Tag, "TAG"));
                }
                if (mode == Text && newMode == CloseTag) {
                    popLexemNode(new LexemNode(LexemType.Static, text.toString()));
                }
                if (mode == OpenTag && newMode == Text) {
                    currentLexemNode = currentLexemNode.parent;
                }
                if (mode == CloseTag && newMode == Text) {
                    //popLexemNode(new LexemNode(LexemType.Static, "CLOSE_TAG"));
                }
                this.mode = newMode;
            }
        }
    }

    public void pushLexemNode(LexemNode lexemNode) {
        currentLexemNode.add(lexemNode);
        currentLexemNode = lexemNode;
    }

    public void popLexemNode(LexemNode lexemNode) {
        currentLexemNode.add(lexemNode);
        currentLexemNode = currentLexemNode.parent;
    }

    public String customTagname = null;
    public List<Attribute> customTagAttributes = new ArrayList<>();
    public StringBuilder attributeBuffer = new StringBuilder();
    public StringBuilder valueBuffer = new StringBuilder();

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
                text.append(blankBuffer);
            } else if (blankBuffer.length() > 0){
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
