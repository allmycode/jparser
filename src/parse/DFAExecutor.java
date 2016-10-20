package parse;

import java.util.ArrayList;
import java.util.List;

import static parse.Predefined.BAD;

public class DFAExecutor {
    private final boolean DEBUG = true;

    public static class DFAExecutionResult {
        int row;
        int col;
        int offset;
    }
    public void executeOnString(int row, int col, int offset, String str, ISymTranslator<Character, ISwitch> translator, DFA dfa) {
        IState s = dfa.start;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            col++;
            if (c == '\n') {
                row++;
                col = 0;
            }
            ISwitch cc = translator.translate(c);
            IState n = dfa.getTransition(s, cc);
            if (n == BAD) {
                throw new RuntimeException("Illegal char '" + c + "' (" + cc + ") at [" + row + ":" + col + "]. State now: " + s + ". sb: " + sb);
            }
            if (DEBUG) {
                System.out.println(s + " -> " + n + " on '" + c + "'" + ". sb: " + sb);
            }
            s = n;
        }
    }
}
