package modeparser;

public class Main {
    public static void main(String[] args) {
        //check("<ggg>");
        //check("<ggg >");
        //check("< ggg>");
        //check("< ggg >");
        check("< g a >");
        check("< g a b>");


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

    public static void check(String s) {
        ModeParser p = new ModeParser(s, true);
        p.parse();
        System.out.println(p.text);

        p = new ModeParser(s, false);
        p.parse();
        System.out.println(p.text);

        System.out.println("---------");
    }
}
