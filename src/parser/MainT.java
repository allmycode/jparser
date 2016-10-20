package parser;

public class MainT {
    public static void main(String[] args) {
        String s1 = "<hello/>there<a></a>";
        String s2 = "<hello/><ui:if>there</ui:if><a></a>";
        String s3 = "<hello><ui:if>there</ui:if><a>sdf<ui:label>sdfdasf <d/></ui:label></a></hello>";
        System.out.println(new TemplateParser().parser(s3));
    }
}
