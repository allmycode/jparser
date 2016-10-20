package parse2;

import java.util.function.Function;

public enum SymbolClasses implements SymbolClass {
    UNKNOWN,
    LINE_BREAK,
    SPACE,
    LT,
    GT,
    EXCL,
    DASH,
    DQ,
    Q,
    EQ,
    BSL,
    SL,
    ALPHA,
    NUM,
    PLUS,
    MUL,
    DOT,
    UNDERSCORE,
    DOLLAR,
    CUR_OPEN,
    CUR_CLOSE
    ;

    public static Function<Character, SymbolClass> translate = c -> {
        switch (c) {
            case '\r':
            case '\n':
                return LINE_BREAK;
            case ' ':
            case '\t':
                return SPACE;
            case '<':
                return LT;
            case '>':
                return GT;
            case '!':
                return EXCL;
            case '-':
                return DASH;
            case '"':
                return DQ;
            case '\'':
                return Q;
            case '=':
                return EQ;
            case '\\':
                return BSL;
            case '/':
                return SL;
            case '+':
                return PLUS;
            case '*':
                return MUL;
            case '.':
                return DOT;
            case '_':
                return UNDERSCORE;
            case '$':
                return DOLLAR;
            case '{':
                return CUR_OPEN;
            case '}':
                return CUR_CLOSE;
        }
        if (Character.isAlphabetic(c)) {
            return ALPHA;
        }
        if (Character.isDigit(c)) {
            return NUM;
        }
        return UNKNOWN;
    };
}
