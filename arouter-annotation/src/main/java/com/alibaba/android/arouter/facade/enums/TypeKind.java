package com.alibaba.android.arouter.facade.enums;

/**
 * Kind of field type.
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 2017-03-16 19:13:38
 */
public enum TypeKind {
    BOOLEAN("Boolean"),
    BYTE("Byte"),
    SHORT("Short"),
    INT("Int"),
    LONG("Long"),
    CHAR("Char"),
    FLOAT("Float"),
    DOUBLE("Double"),

    // Other type

    STRING("String"),
    CHARSEQUENCE("CharSequence"),
    SERIALIZABLE("Serializable"),
    PARCELABLE("Parcelable"),
    OBJECT("Object"),
    BYTEARRAY("ByteArray"),
    SHORTARRAY("ShortArray"),
    CHARARRAY("CharArray"),

    CHARSEQUENCEARRAY("CharSequenceArray"),
    CHARSEQUENCEARRAYLIST("CharSequenceArrayList"),
    FLOATARRAY("FloatArray"),
    INTEGERARRAYLIST("IntegerArrayList"),

    PARCELABLEARRAYLIST("ParcelableArrayList"),

    SPARSEPARCELABLEARRAY("SparseParcelableArray"),
    STRINGARRAYLIST("StringArrayList");

    private String statement;

    TypeKind(String statement) {
        this.statement = statement;
    }

    public String getStatement() {
        return this.statement;
    }
}
