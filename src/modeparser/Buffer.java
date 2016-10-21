package modeparser;

public class Buffer {
    private StringBuilder sb = new StringBuilder();

    public void append(char c) {
        sb.append(c);
    }

    public void append(Buffer b) {
        sb.append(b.sb);
        b.clean();
    }

    public void append(StringBuilder b) {
        sb.append(b);
    }

    public void clean() {
        sb.delete(0, sb.length());
    }

    public int length() {
        return sb.length();
    }

    public String toString() {
        return sb.toString();
    }
}
