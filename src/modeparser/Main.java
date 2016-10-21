package modeparser;

public class Main {
    public static void main(String[] args) {
        checkTR("<n class='bdy'>");
    }


    public static void checkTR(String s) {
        System.out.println(s);
        ModeParser p = new ModeParser(s, true);
        p.parse();
        System.out.println(p.text);
        for (TokenRange tr : p.tokens) {
            System.out.print(tr + "'" + s.substring(tr.start, tr.end) + "', ");
        }
        System.out.print("\nnew TokenRange[]{");
        for (int i = 0; i < p.tokens.size(); i++) {
            TokenRange tr = p.tokens.get(i);
            System.out.print("tr(" + tr.state + ", " + tr.start + ", " + tr.end + ")");
            if (i < p.tokens.size() - 1) {
                System.out.print(", ");
            }
        }
        System.out.print("}");
        System.out.println("\n==================\n");
    }
}
