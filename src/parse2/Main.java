package parse2;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        check3();
    }
    public static void check(String s) {
        HTMLDFA dfa = new HTMLDFA();
        DFAExecutor executor = new DFAExecutor();

        List<Token> tokens = new ArrayList<>();
        executor.execute(new StringIndexable2(s), SymbolClasses.translate, dfa, tokens, true, false);
        System.out.println(tokens);
    }

    public static void checkRight() {
        check("<tag a=b c=\"d t \\n5\"/>hel<g main/>lo<a>\n</a>");
    }

    public static void check2() {
        check("<tag a=b z=$rt.t>");
    }

    public static void check3() {
        check("<tag a=b z=${rt.t + 6}>");
    }
}

