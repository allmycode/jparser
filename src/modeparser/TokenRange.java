package modeparser;

import parse2.Token;

public class TokenRange {
    public ModeParser.State state;
    public int start;
    public int end;

    public TokenRange(ModeParser.State state, int start, int end) {
        this.state = state;
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return state + "[" + start + ", " + end + ")";
    }
}
