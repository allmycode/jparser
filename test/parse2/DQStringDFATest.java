package parse2;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.*;

public class DQStringDFATest {

    public static List<Token> check(String s) {
        DQStringDFA sdfa = new DQStringDFA();
        DFAExecutor executor = new DFAExecutor();
        List<Token> tokens = new ArrayList<>();
        executor.execute(new StringIndexable2(s), SymbolClasses.translate, sdfa, tokens, true, false);
        sdfa.dump();
        return tokens;
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private void expectAt(int row, int col) {
        thrown.expect(ParseException.class);
        thrown.expect(hasProperty("row", is(row)));
        thrown.expect(hasProperty("col", is(col)));
    }

    @Test
    public void testWrong1() throws ParseException {
        expectAt(0, 0);
        List<Token> tokens = check("s\"hello\"");
    }

    @Test
    public void testWrong2() throws ParseException {
        expectAt(0, 7);
        List<Token> tokens = check("\"hello\"s");
    }

    @Test
    public void testWrong1Q() throws ParseException {
        expectAt(0, 0);
        List<Token> tokens = check("\"hello'");
    }

    @Test
    public void testRight() {
        List<Token> tokens = check("\"hello\"");
        Assert.assertEquals("hello", tokens.get(0).text);
    }


    @Test
    public void testRightQ() {
        List<Token> tokens = check("'hello'");
        Assert.assertEquals("hello", tokens.get(0).text);
    }

    @Test
    public void testRightSpace() {
        List<Token> tokens = check("\"hello world\"");
        Assert.assertEquals("hello world", tokens.get(0).text);
    }

    @Test
    public void testRightSpaceQ() {
        List<Token> tokens = check("'hello world'");
        Assert.assertEquals("hello world", tokens.get(0).text);
    }

    @Test
    public void testRightEscape1() {
        List<Token> tokens = check("\"hello \\\"world\"");
        Assert.assertEquals("hello \"world", tokens.get(0).text);
    }

    @Test
    public void testRightEscape2() {
        List<Token> tokens = check("\"hello \\\\world\"");
        Assert.assertEquals("hello \\world", tokens.get(0).text);
    }

    @Test
    public void testRightEscape3() {
        List<Token> tokens = check("\"hello \\nworld\"");
        Assert.assertEquals("hello \nworld", tokens.get(0).text);
    }

    @Test
    public void testRightEscape4() {
        List<Token> tokens = check("\"hello \\tworld\"");
        Assert.assertEquals("hello \tworld", tokens.get(0).text);
    }

    @Test
    public void testRightEscape4Q() {
        List<Token> tokens = check("'hello \\tworld'");
        Assert.assertEquals("hello \tworld", tokens.get(0).text);
    }
}
