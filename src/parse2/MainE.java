package parse2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainE {
    public static void main(String[] args) {
        checkRightBlank();
    }
    public static void check(String s, boolean breakOnBlank) {
        ExpressionDFA dfa = new ExpressionDFA();

        List<Token> tokens = new ArrayList<>();
        dfa.execute(new StringIndexable2(s), SymbolClasses.translate, dfa, tokens, true, false, Collections.emptySet());
        System.out.println(tokens);
    }

    public static void checkRight() {
        check("5 + 44.4 * x-1+'' + object.field.f2", false);
    }

    public static void checkRightBlank() {
        check("object 17", true);
    }
}
