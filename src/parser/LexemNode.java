package parser;

import java.util.ArrayList;
import java.util.List;

public class LexemNode {
    public LexemNode parent;
    public final LexemType type;
    public final String text;
    public final List<LexemNode> children = new ArrayList<>();

    public LexemNode(LexemType type, String text) {
        this.type = type;
        this.text = text;
    }

    public void add(LexemNode node) {
        children.add(node);
        node.parent = this;
    }

    @Override
    public String toString() {
        return "{" + type + ": " + text + (children.size() > 0 ?
                ", " + children : "") +
                '}';
    }
}
