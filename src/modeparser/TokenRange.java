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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TokenRange that = (TokenRange) o;

        if (start != that.start) return false;
        if (end != that.end) return false;
        return state == that.state;

    }

    @Override
    public int hashCode() {
        int result = state != null ? state.hashCode() : 0;
        result = 31 * result + start;
        result = 31 * result + end;
        return result;
    }
}
