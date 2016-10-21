package modeparser;

public enum State {
    Invalid,
    Start,
    EOF(false, false),
    TagStart(false, false),
    TagStart_(true),
    TagName(false, false),
    TagName_(true),
    TagEndSlash,
    TagEnd,
    TagStartSlash(false, false),
    TagStartSlash_(true),
    TagAttr,
    TagAttr_(true),
    TagAttrEQ,
    TagAttrEQ_(true),
    TagAttrValue,
    TagAttrVString,
    TagAttrVString_,
    TagAttrVString__(true),
    C1(true),
    C2,
    C3(false, false)
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

    State(boolean blank, boolean dumpToText) {
        this.blank = blank;
        this.dumpToText = dumpToText;
    }

    public boolean isBlank() {
        return blank;
    }

    public boolean isDumpToText() {
        return dumpToText;
    }
}
