package modeparse;

import org.junit.Assert;
import org.junit.Test;

import modeparser.ModeParser;

public class ModeParserTest1 {
    public static String check(String s, boolean removeWhitespaces) {
        ModeParser p = new ModeParser(s, removeWhitespaces);
        p.parse();
        return p.text.toString();
    }

    public void checkE(String s) {
        Assert.assertEquals(s, check(s, false));
    }

    public void checkE(String s, String e) {
        Assert.assertEquals(e, check(s, false));
    }

    public void checkR(String s, String r) {
        Assert.assertEquals(r, check(s, true));
    }

    @Test
    public void test0() {
        checkE("<ggg>");
    }
    @Test
    public void test01() {
        checkE("< ggg>");
    }
    @Test
    public void test01R() {
        checkR("< g>", "<g>");
    }
    @Test
    public void test02() {
        checkE("<ggg >");
    }
    @Test
    public void test02R() {
        checkR("<g >", "<g>");
    }
    @Test
    public void test03() {
        checkE("< g >");
    }
    @Test
    public void test03R() {
        checkR("< g >", "<g>");
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
    public void testLong() {
        checkE("< g a b = 1 f=g>");
    }

    @Test
    public void testLongR() {
        checkR("< g a b = 1 f=g>" ,"<g a b=1 f=g>");
    }

    @Test
    public void testSpaceBetweenNameAttrR() {
        checkR("< g    a   >", "<g a>");
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

    @Test public void testStringAttrDQ_() {
        checkE("<n a=\"v'\\n\" d>", "<n a=\"v'\n\" d>");
    }

    @Test public void testStringAttrDQ() {
        checkR("<n a=\"v'\\n\" d>", "<n a=\"v'\n\" d>");
    }

    @Test public void testStringAttrDQ2() {
        checkR("<n   a=  \"v'\\n\"   d >", "<n a=\"v'\n\" d>");
    }

    @Test public void testStringAttrDQ3() {
        checkR("<n   a=  \"v'\\n\"   d >  hello   world  </  n >", "<n a=\"v'\n\" d>hello   world</n>");
    }

    @Test public void testStringAttrSQ_() {
        checkE("<n a='v\"\\n' d>", "<n a='v\"\n' d>");
    }

    @Test public void testStringAttrSQ() {
        checkR("<n a='v\"\\n' d>", "<n a='v\"\n' d>");
    }

    @Test public void testStringAttrSQ2() {
        checkR("<n   a  =  'v\"\\n'    d>", "<n a='v\"\n' d>");
    }


    @Test public void testStringAttrSQ_ESC_() {
        checkE("<n a='a\\\\b' d>", "<n a='a\\b' d>");
    }

}
