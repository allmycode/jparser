package modeparse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Test;

import modeparser.ModeParser;
import modeparser.TokenRange;

public class ModeParserTest3 {


    private void check(String resource, boolean removeWs) {
        try {
            String sourceHtml = readResource(resource + "_.html");
            String cleanHtml = readResource(resource + (removeWs ? "" : "_") + ".html");
            ModeParser mp = new ModeParser(sourceHtml, removeWs);
            mp.parse();
            String result = mp.text.toString();
            Assert.assertEquals(cleanHtml, result);
            for (TokenRange tr : mp.tokens) {
                System.out.print(tr + "'" + sourceHtml.substring(tr.start, tr.end) + "', ");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkAll(String resource) {
        check(resource, false);
        check(resource, true);
    }

    private String readResource(String s) throws IOException {
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(s);
        InputStreamReader rd = new InputStreamReader(resourceAsStream);
        char[] buf = new char[1024];
        int readed;
        StringBuilder res = new StringBuilder();
        while ((readed = rd.read(buf)) > 0) {
            res.append(buf, 0, readed);
        }
        return res.toString();
    }

    @Test
    public void test0() {
        checkAll("test0");
    }

    @Test
    public void test1() {
        checkAll("test1");
    }

}
