package parse;

import static parse.Predefined.END;

public class Main2 {
    public static void main(String[] args) {
        checkWrong();

    }
    public static void check(String s) {
        StringDFA sdfa = new StringDFA(END);
        DFAExecutor executor = new DFAExecutor();

        executor.executeOnString(0, 0, 0, s, sdfa, sdfa.dfa);
    }

    public static void checkRight() {
        check("\"sadfsa dsf asdf\"");
    }

    public static void checkWrong() {
        check("s\"sadfsa dsf asdf\"");
    }
}
