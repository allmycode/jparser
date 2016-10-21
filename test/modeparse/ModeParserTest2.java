package modeparse;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import modeparser.ModeParser;
import modeparser.TokenRange;
import static modeparser.State.*;

public class ModeParserTest2 {

    static TokenRange tr(modeparser.State state, int start, int end) {
        return new TokenRange(state, start, end);
    }

    private void check(String s, TokenRange[] tokenRanges) {
        ModeParser p = new ModeParser(s, false);
        p.parse();
        Assert.assertArrayEquals(tokenRanges, p.tokens.toArray());
    }

    public static void checkTR(String s) {
        System.out.println(s);
        ModeParser p = new ModeParser(s, false);
        p.parse();
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

    //@Test
    public void noTest() {
        checkTR("<n>");
        checkTR("<n >");
        checkTR("<name >");
        checkTR("<name a>");
        checkTR("<name a/>");
        checkTR("<name a/> hello world <no_name>");
        checkTR("<ui:name>text</ui:name s>");
        checkTR("<n a=\"v\" d>");
    }

    @Test
    public void test0() {
        check("<n>", new TokenRange[]{tr(TagStart, 0, 1), tr(TagName, 1, 2), tr(TagEnd, 2, 3), });
    }

    @Test
    public void test1() {
        check("<n >", new TokenRange[]{tr(TagStart, 0, 1), tr(TagName, 1, 2), tr(TagName_, 2, 3), tr(TagEnd, 3, 4), });
    }

    @Test
    public void test01() {
        check("<name >", new TokenRange[]{tr(TagStart, 0, 1), tr(TagName, 1, 5), tr(TagName_, 5, 6), tr(TagEnd, 6, 7)});
    }

    @Test
    public void test011() {
        check("<name a/>", new TokenRange[]{tr(TagStart, 0, 1), tr(TagName, 1, 5), tr(TagName_, 5, 6), tr(TagAttr, 6, 7), tr(TagEndSlash, 7, 8), tr(TagEnd, 8, 9)});
    }

    @Test
    public void testLong1() {
        check("<name a/> hello world <no_name>", new TokenRange[]{tr(TagStart, 0, 1), tr(TagName, 1, 5), tr(TagName_, 5, 6), tr(TagAttr, 6, 7), tr(TagEndSlash, 7, 8), tr(TagEnd, 8, 9), tr(C1, 9, 10), tr(C2, 10, 15), tr(C3, 15, 16), tr(C2, 16, 21), tr(C3, 21, 22), tr(TagStart, 22, 23), tr(TagName, 23, 30), tr(TagEnd, 30, 31)});
    }
    @Test
    public void testLong2() {
        check("<ui:name>text</ui:name s>", new TokenRange[]{tr(TagStart, 0, 1), tr(TagName, 1, 8), tr(TagEnd, 8, 9), tr(C2, 9, 13), tr(TagStart, 13, 14), tr(TagStartSlash, 14, 15), tr(TagName, 15, 22), tr(TagName_, 22, 23), tr(TagAttr, 23, 24), tr(TagEnd, 24, 25)});
    }

    @Test
    public void testString0() {
        check("<n a=\"v\" d>", new TokenRange[]{tr(TagStart, 0, 1), tr(TagName, 1, 2), tr(TagName_, 2, 3), tr(TagAttr, 3, 4), tr(TagAttrEQ, 4, 5), tr(TagAttrVString, 5, 8), tr(TagAttrVString__, 8, 9), tr(TagAttr, 9, 10), tr(TagEnd, 10, 11)});
    }


}
