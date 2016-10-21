package modeparser;

public class Main {
    public static void main(String[] args) {
        checkLX("<n class='bdy' d jj=1><ui:label/>none<ui:label><h>");
    }


    public static void checkLX(String s) {
        System.out.println(s);
        ModeParser p = new ModeParser(s, true);
        p.parse();
        System.out.println(p.text);
        System.out.println(p.rootLexemNode);

        System.out.println("\n==================\n");
    }
}
