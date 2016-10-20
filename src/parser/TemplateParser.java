package parser;

import parse2.SymbolClass;
import parse2.SymbolClasses;

public class TemplateParser {

    private static int INVALID = -1;
    private static int START = 0;
    private static int TAG_OPEN = 1;
    private static int TEXT = 2;
    private static int TAG_NAME = 3;
    private static int TAG_ATTR_NAME = 4;
    private static int TAG_ATTR_VALUE = 5;
    private static int TAG_BACKSLASH_LAST = 6;
    private static int TAG_CLOSE = 7;
    private static int TAG_BACKSLASH_FIRST = 8;

    private static int TAG_ATTR_DQ_STRING = 9;
    private static int TAG_ATTR_SQ_STRING = 10;


    int i;
    String str;
    String currentTag = null;
    public LexemNode parser(String str) {
        LexemNode root = new LexemNode(LexemType.Root, null);
        LexemNode current = root;
        int state = START;
        int lastStaticBreak = 0;
        int beforeLastTag = 0;
        i = 0;
        this.str = str;
        while(i < str.length()) {
            char c = str.charAt(i);
            SymbolClass symClass = SymbolClasses.translate.apply(c);
            if (state == START) {
                if (symClass != SymbolClasses.LT) {
                    state = INVALID;
                    continue;
                }
                state = TAG_OPEN;
                skipSpaces();
            }
            else if (state == TAG_OPEN) {
                beforeLastTag = i - 1;
                if (symClass == SymbolClasses.ALPHA) {
                    state = TAG_NAME;
                    int from = i;
                    while (Character.isAlphabetic(c) || Character.isDigit(c) || c == ':') {
                        i++;
                        c = str.charAt(i);
                    }
                    String tagName = str.substring(from, i);
                    i--;
                    if (tagName.equals("ui:label") || tagName.equals("ui:if")) {
                        String staticText = str.substring(lastStaticBreak, beforeLastTag);
                        current.add(new LexemNode(LexemType.Static, staticText));
                        LexemNode tagNode = new LexemNode(LexemType.Tag, tagName);
                        current.add(tagNode);
                        current = tagNode;
                        currentTag = tagName;
                    }
                    skipSpaces();
                } else if (symClass == SymbolClasses.SL) {
                    state = TAG_BACKSLASH_FIRST;
                    skipSpaces();
                } else if (symClass == SymbolClasses.GT) {
                    state = TAG_CLOSE;
                } else {
                    state = INVALID;
                }
            }
            else if (state == TAG_NAME) {
                if (symClass == SymbolClasses.SL) {
                    state = TAG_BACKSLASH_LAST;
                } else if (symClass == SymbolClasses.GT) {
                    state = TAG_CLOSE;
                } else {
                    state = INVALID;
                }
            }
            else if (state == TAG_BACKSLASH_LAST) {
                if (symClass != SymbolClasses.GT) {
                    state = INVALID;
                    continue;
                }
                state = TAG_CLOSE;

            }
            else if (state == TAG_CLOSE) {
                if (current.type == LexemType.Tag) {
                    lastStaticBreak = i;
                }
                boolean moved = false;
                while (symClass != SymbolClasses.LT) {
                    i++;
                    c = str.charAt(i);
                    symClass = SymbolClasses.translate.apply(c);
                    moved = true;
                }
                if (moved) {
                   // i--;
                }
                state = TAG_OPEN;
            }
            else if (state == TAG_BACKSLASH_FIRST) {
                int from = i;
                while (Character.isAlphabetic(c) || Character.isDigit(c) || c == ':') {
                    i++;
                    c = str.charAt(i);
                }
                String tagName = str.substring(from, i);
                boolean inTag = tagName.equals(currentTag);
                if (inTag) {
                    current.add(new LexemNode(LexemType.Static, str.substring(lastStaticBreak, beforeLastTag)));
                    current = current.parent;
                }

                skipSpaces();
                c = str.charAt(i);
                symClass = SymbolClasses.translate.apply(c);
                if (symClass != SymbolClasses.GT) {
                    state = INVALID;
                    continue;
                }
                if (inTag) {
                    lastStaticBreak = i + 1;
                }
                state = TAG_CLOSE;
            }
            else if (state == INVALID) {
                throw new RuntimeException("Invalid state at " + i);
            }
            i++;
        }
        current.add(new LexemNode(LexemType.Static, str.substring(lastStaticBreak, i)));
        return root;
    }

    private void skipSpaces() {
        if (i < str.length() - 1) {
            char c = str.charAt(i + 1);
            boolean moved = false;
            while (isBlank(c)) {
                i++;
                moved = true;
            }
            if (moved) {
                i--;
            }
        }
    }

    private boolean isBlank(char c) {
        return c == ' ' || c == '\n' || c == '\t' || c == '\r';
    }
}
