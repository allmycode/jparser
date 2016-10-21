package parse2;

import static parse2.SymbolClasses.*;

public enum L2SymbolClasses implements SymbolClass {
    BLANK(LINE_BREAK, SPACE),
    ALNUM(ALPHA, NUM),
    ALNUM_(ALPHA, NUM, UNDERSCORE),
    ALNUM_COL(ALPHA, NUM, UNDERSCORE, COLON),
    OP(PLUS,DASH,MUL,DOT, SLASH),
    OP_NODOT(PLUS,DASH,MUL, SLASH)
    ;

    public final SymbolClasses[] symClasses;

    L2SymbolClasses(SymbolClasses ... symClasses) {
        this.symClasses = symClasses;
    }
}
