package parse2;

import org.junit.Assert;
import org.junit.Test;

import modeparser.ModeParser;

public class ModeParserTest1 {
    public static String check(String s, boolean removeWhitespaces) {
        System.out.println(s);
        ModeParser p = new ModeParser(s, removeWhitespaces);
        p.parse();
        return p.text.toString();
    }

    public void checkE(String s) {
        Assert.assertEquals(s, check(s, false));
    }

    public void checkR(String s, String r) {
        Assert.assertEquals(r, check(s, true));
    }

    @Test
    public void test1() {
        checkE("<ggg>");
        checkE("<ggg/>");
        checkE("< ggg/>");
        checkE("< ggg />");
        checkE("< ggg> hello");
        checkE("< ggg> hello </ggg>");
        checkE("< ggg> hello world </ggg>");
    }

    @Test
    public void test1R() {
        checkR("<ggg>", "<ggg>");
        checkR("<ggg/>", "<ggg/>");
        checkR("< ggg/>", "<ggg/>");
        checkR("< ggg />", "<ggg/>");
        checkR("< ggg> hello", "<ggg>hello");
        checkR("< ggg> hello </ggg>", "<ggg>hello</ggg>");
        checkR("< ggg> hello world </ggg>", "<ggg>hello world</ggg>");
    }

    @Test
    public void test2() {
        checkE("<ggg>");
        checkE("<ggg >");
        checkE("< ggg>");
        checkE("< ggg >");
        checkE("< g    a   >");
        checkE("< g a b=1>");
        checkE("< g a b =1>");
        checkE("< g a b = 1 f=g>");
    }

    @Test
    public void test2R() {
        checkR("<ggg>", "<ggg>");
        checkR("<ggg >", "<ggg>");
        checkR("< ggg>", "<ggg>");
        checkR("< ggg >", "<ggg>");
        checkR("< g    a   >", "<g a>");
        checkR("< g a b=1>", "<g a b=1>");
        checkR("< g a b =1>", "<g a b=1>");
        checkR("< g a b = 1 f=g>", "<g a b=1 f=g>");
    }
}
