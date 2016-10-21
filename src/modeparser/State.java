package modeparser;

public enum State {
    Invalid,
    Start,
    EOF,
    TagStart,
    TagStart_(true),
    TagName,
    TagName_(true),
    TagEndSlash,
    TagEnd,
    TagStartSlash,
    TagStartSlash_(true),
    TagAttr,
    TagAttr_(true),
    TagAttrEQ,
    TagAttrEQ_(true),
    TagAttrValue,
    TagAttrVString,
    TagAttrVString_,
    TagAttrVString__(true),
    C1,
    C2,
    C3
    ;

    private boolean blank;
    private boolean dumpToText;

    State() {
        dumpToText = true;
    }

    State(boolean blank) {
        this.blank = blank;
        this.dumpToText = false;
    }

    public boolean isBlank() {
        return blank;
    }

    public boolean isDumpToText() {
        return dumpToText;
    }
}
