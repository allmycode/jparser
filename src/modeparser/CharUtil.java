package modeparser;

public class CharUtil {
    public static boolean isBlank(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    public static boolean isAlpha(char c) {
        return Character.isAlphabetic(c);
    }

    public static boolean isNum(char c) {
        return Character.isDigit(c);
    }

    public static boolean isAlnum(char c) {
        return isAlpha(c) || isNum(c);
    }

    public static boolean isTagName(char c) {
        return isAlnum(c) || c ==':' || c == '_';
    }

    public static boolean isLT(char c) {
        return c == '<';
    }

    public static boolean isGT(char c) {
        return c == '>';
    }

    public static boolean isSlash(char c) {
        return c == '/';
    }

    public static boolean isEQ(char c) {
        return c == '=';
    }

    public static boolean isStringBound(char c) {
        return c == '"' || c == '\'';
    }

    public static boolean isExpStart(char c) {
        return c == '$';
    }

    public static boolean isHash(char c) {
        return c == '#';
    }

    public static boolean isCurOpen(char c) {
        return c == '{';
    }

    public static boolean isCurClose(char c) {
        return c == '}';
    }

    public static boolean isParOpen(char c) {
        return c == '(';
    }

    public static boolean isParClose(char c) {
        return c == ')';
    }
}
