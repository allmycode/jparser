package parse2;

import static parse2.SymbolClasses.*;

public enum L2SymbolClasses implements SymbolClass {
    BLANK(LINE_BREAK, SPACE),
    ALNUM(ALPHA, NUM),
    OP(PLUS,DASH,MUL,DOT,SL),
    OP_NODOT(PLUS,DASH,MUL,SL)
    ;

    public final SymbolClasses[] symClasses;

    L2SymbolClasses(SymbolClasses ... symClasses) {
        this.symClasses = symClasses;
    }
}
