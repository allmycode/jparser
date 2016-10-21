package modeparser;

public class Main {
    public static void main(String[] args) {
        checkTR("<n>");
        checkTR("<n >");
        checkTR("<name >");
        checkTR("<name a>");
        checkTR("<name a/>");
        checkTR("<name a/> hello world <no_name>");
        checkTR("<ui:name>text</ui:name s>");
    }

    public static void checkTag() {
        check("<ggg>");
        check("<ggg/>");
        check("< ggg/>");
        check("< ggg />");
        check("< ggg> hello");
        check("< ggg> hello </ggg>");
        check("< ggg> hello world </ggg>");
    }

    public static void checkTag2() {
        check("<ggg>");
        check("<ggg >");
        check("< ggg>");
        check("< ggg >");
        check("< g    a   >");
        check("< g a b=1>");
        check("< g a b =1>");
        check("< g a b = 1 f=g>");
    }

    public static void check(String s) {
        System.out.println(s);
        ModeParser p = new ModeParser(s, false);
        p.parse();
        System.out.println(p.text);

        p = new ModeParser(s, true);
        p.parse();
        System.out.println(p.text);

        System.out.println("---------");
    }

    public static void checkTR(String s) {
        System.out.println(s);
        ModeParser p = new ModeParser(s, false);
        p.parse();
        for (TokenRange tr : p.tokens) {
            System.out.print(tr + "'" + s.substring(tr.start, tr.end) + "', ");
        }
        System.out.println("\n==================\n");
    }
}
