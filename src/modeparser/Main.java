package modeparser;

public class Main {
    public static void main(String[] args) {

        checkTag();
        checkTag2();


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
}
