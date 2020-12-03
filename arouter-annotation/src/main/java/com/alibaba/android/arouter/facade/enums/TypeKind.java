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
    BYTEARRAY("ByteArray"),
    CHAR("Char"),
    CHARARRAY("CharArray"),
    CHARSEQUENCE("CharSequence"),
    CHARSEQUENCEARRAY("CharSequenceArray"),
    CHARSEQUENCEARRAYLIST("CharSequenceArrayList"),
    DOUBLE("Double"),
    FLOAT("Float"),
    FLOATARRAY("FloatArray"),
    INT("Int"),
    INTEGERARRAYLIST("IntegerArrayList"),
    LONG("Long"),
    OBJECT("OBJECT"),
    PARCELABLE("Parcelable"),
    PARCELABLEARRAYLIST("ParcelableArrayList"),
    SERIALIZABLE("Serializable"),
    SHORT("Short"),
    SHORTARRAY("ShortArray"),
    SPARSEPARCELABLEARRAY("SparseParcelableArray"),
    STRING("String"),
    STRINGARRAYLIST("StringArrayList");

    private String statement;

    TypeKind(String statement) {
        this.statement = statement;
    }

    public String getStatement() {
        return this.statement;
    }
}
