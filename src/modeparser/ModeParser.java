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

    public Buffer tagstartBuffer = new Buffer();
    public Buffer tagnameBuffer = new Buffer();

    public Buffer text = new Buffer();

    public Buffer blankBuffer = new Buffer();

    void appendBlankBuffer() {
        if (!removeWhitespaces) {
            if (state == TagStart || state == TagStart_ || state == TagName) {
                tagstartBuffer.append(blankBuffer);
            } else {
                text.append(blankBuffer);
            }
        }
    }

    void appendBlankBufferOrSpace() {
        if (!removeWhitespaces) {
            text.append(blankBuffer);
        } else if (blankBuffer.length() > 0){
            text.append(' ');
            blankBuffer.clean();
        }
    }

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
                    // String bounds and escape are treated specially
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

    public void advance() {
        i++;
        col++;
        if (i < str.length() && str.charAt(i) == '\n') {
            row++;
            col = 0;
        }
    }

    public void parse() {
        state = Start;
        mode = Text;
        i = 0;
        char c;
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
                    if (c == '\\') {
                        c = '\\';
                    }
                    stringEscape = 0;
                } else {
                    if (c == stringBound) {
                        newState = TagAttrVString_;
                    } else if (c == '\\') {
                        stringEscape = c;
                        advance();
                        continue;
                    }
                }
            }

            if (newState == Invalid) {
                newState = getNext(transitions, state, symClass, row, col);
            }

            processStateTransition(c, state, newState);
            state = newState;

            if (state == Invalid) {
                throw new ParseException("Invalid state at [" + row + ":" + col +"] char '"+ c + "'", row, col);
            }

            advance();
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
            if (state.isBlank()) {
                appendBlankBuffer();
            }
            switch (state) {
                case TagName:
                    String tagname = tagnameBuffer.toString();
                    if (tagname.startsWith("ui:")) {
                        customTagname = tagname;
                        processModeTransition(OpenTag);
                    } else {
                        text.append(tagstartBuffer);
                    }
                    break;
                case TagEnd:
                    if (mode == OpenTag || mode == CloseTag) {
                        processModeTransition(Text);
                    }
                    break;

                case TagAttrValue:
                case TagAttrVString_:
                    //System.out.println("Found attribute:" + attributeBuffer + " = " + valueBuffer);
                    attributeBuffer = new StringBuilder();
                    valueBuffer = new StringBuilder();
                    break;


                case C3:
                    if (newState == C2) {
                        text.append(blankBuffer);
                    }
                    break;

            }
            // - - - - - -
            // enter state
            if (newState.isBlank()) {
                blankBuffer.clean();
            }
            switch (newState) {
                case TagStart:
                    appendBlankBuffer();
                    tagstartBuffer.clean();
                    break;
                case TagName:
                    tagnameBuffer.clean();
                    break;
                case TagAttr:
                    if (attributeBuffer.length() > 0) {
                        System.out.println("Found attribute:" + attributeBuffer);
                    }
                    attributeBuffer = new StringBuilder();
                    break;
                case C3:
                    blankBuffer.clean();
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
        if (newState.isBlank()) {
            blankBuffer.append(c);
        }
        switch (newState) {
            case TagName:
                tagnameBuffer.append(c);
            case TagStart:
            case TagStartSlash:
            case TagEndSlash:
                tagstartBuffer.append(c);
                break;

            case TagAttr:
                appendBlankBufferOrSpace();
                attributeBuffer.append(c);
                break;

            case TagAttrValue:
                valueBuffer.append(c);
                break;

            case TagAttrVString:
                valueBuffer.append(c);
                break;

            case TagAttrVString_:
                valueBuffer.append(c);
                break;

            case C3:
                blankBuffer.append(c);
                break;
        }
        if (newState.isDumpToText()) {
            text.append(c);
        }
    }

    void processModeTransition(Mode newMode) {
        if (newMode != null) {
            if (mode != newMode) {
                if (mode == Text && newMode == OpenTag) {
                    currentLexemNode.add(new LexemNode(LexemType.Static, text.toString()));
                    text.clean();
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

}
