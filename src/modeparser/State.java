package modeparser;

public enum State {
    Invalid,
    Start,
    EOF,
    TagStart,
    TagStart_,
    TagName,
    TagName_,
    TagEndSlash,
    TagEnd,
    TagStartSlash,
    TagStartSlash_,
    TagAttr,
    TagAttr_,
    TagAttrEQ,
    TagAttrEQ_,
    TagAttrValue,
    TagAttrVString,
    TagAttrVString_,
    TagAttrVString__,
    C1,
    C2,
    C3
    ;
}
