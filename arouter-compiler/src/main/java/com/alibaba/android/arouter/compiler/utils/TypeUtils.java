package com.alibaba.android.arouter.compiler.utils;

import com.alibaba.android.arouter.compiler.processor.BaseProcessor;
import com.alibaba.android.arouter.facade.enums.TypeKind;
import com.google.auto.common.MoreElements;


import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static com.alibaba.android.arouter.facade.enums.TypeKind.*;
import static com.alibaba.android.arouter.facade.enums.TypeKind.BYTE;


/**
 * Utils for type exchange
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/2/21 下午1:06
 */
public class TypeUtils {

    private Types types;
    private TypeMirror parcelableType;
    private TypeMirror serializableType;
    private Elements elements;

    public TypeUtils(Types types, Elements elements) {
        this.types = types;
        this.elements = elements;

        parcelableType = elements.getTypeElement(Consts.PARCELABLE).asType();
        serializableType = elements.getTypeElement(Consts.SERIALIZABLE).asType();
    }

    /**
     * Diagnostics out the true java type
     *
     * @param typeMirror Raw TypeMirror
     * @return Type class of java
     */
    public int typeExchange(final TypeMirror typeMirror) {

        // Primitive
        if (typeMirror.getKind().isPrimitive()) {
            return typeMirror.getKind().ordinal();
        }

        switch (typeMirror.toString()) {
            case Consts.BYTE:
                return BYTE.ordinal();
            case Consts.SHORT:
                return SHORT.ordinal();
            case Consts.INTEGER:
                return INT.ordinal();
            case Consts.LONG:
                return LONG.ordinal();
            case Consts.FLOAT:
                return FLOAT.ordinal();
            case Consts.DOUBEL:
                return DOUBLE.ordinal();
            case Consts.BOOLEAN:
                return BOOLEAN.ordinal();
            case Consts.CHAR:
                return CHAR.ordinal();
            case Consts.STRING:
                return STRING.ordinal();
            default:

                if (typeMirror.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
                    if (typeMirror.toString().startsWith("java.util.ArrayList")||typeMirror.toString().startsWith("java.util.List")) {
                        List<? extends TypeMirror> typeArgs = ((DeclaredType) typeMirror).getTypeArguments();
                        if (typeArgs != null && typeArgs.size() == 1) {
                            TypeMirror argType = typeArgs.get(0);
                            if (isSubtype(argType, "java.lang.Integer")) {
                                return INTEGERARRAYLIST.ordinal();
                            } else if (isSubtype(argType, "java.lang.String")) {
                                return STRINGARRAYLIST.ordinal();
                            } else if (isSubtype(argType, "java.lang.CharSequence")) {
                                return CHARSEQUENCEARRAYLIST.ordinal();
                            } else if (isSubtype(argType, "android.os.Parcelable")) {
                                return PARCELABLEARRAYLIST.ordinal();
                            }
                        }
                    } else if (isSubtype(typeMirror, "java.lang.CharSequence")) {
                        return CHARSEQUENCE.ordinal();
                    } else if (typeMirror.toString().startsWith("android.util.SparseArray")) {
                        return SPARSEPARCELABLEARRAY.ordinal();
                    }
                    // Other side, maybe the PARCELABLE or SERIALIZABLE or OBJECT.
                    else if (types.isSubtype(typeMirror, parcelableType)) {
                        // PARCELABLE
                        return PARCELABLE.ordinal();
                    } else if (types.isSubtype(typeMirror, serializableType)) {
                        // SERIALIZABLE
                        return SERIALIZABLE.ordinal();
                    } else {
                        return OBJECT.ordinal();
                    }
                } else if (typeMirror instanceof ArrayType) {
                    ArrayType arrayType = (ArrayType) typeMirror;
                    TypeMirror compType = arrayType.getComponentType();
                    TypeKind compTypeKind = values()[typeExchange(compType)];
                    if (compType.getKind().isPrimitive()) {
                        switch (compTypeKind) {
                            case BYTE:
                                return BYTEARRAY.ordinal();
                            case SHORT:
                                return SHORTARRAY.ordinal();
                            case INT:
                                break;
                            case LONG:
                                break;
                            case FLOAT:
                                return FLOATARRAY.ordinal();
                            case DOUBLE:
                                break;
                            case BOOLEAN:
                                break;
                            case CHAR:
                                return CHARARRAY.ordinal();
                            default:
                                break;
                        }
                        return OBJECT.ordinal();
                    } else if (compType instanceof DeclaredType) {
                        Element compElement = ((DeclaredType) compType).asElement();
                        if (compElement instanceof TypeElement) {
                            if (isSubtype(compElement, "java.lang.String")) {
                                return STRINGARRAYLIST.ordinal();
                            } else if (isSubtype(compElement, "java.lang.CharSequence")) {
                                return CHARSEQUENCEARRAY.ordinal();
                            } else if (isSubtype(compElement, "android.os.Parcelable")) {
                                return SPARSEPARCELABLEARRAY.ordinal();
                            }
                            return OBJECT.ordinal();
                        }
                    }
                }else {
                }
                return OBJECT.ordinal();

        }
    }



    private boolean isSubtype(Element typeElement, String type) {
        return types.isSubtype(typeElement.asType(),
                elements.getTypeElement(type).asType());
    }

    private boolean isSubtype(TypeMirror typeMirror, String type) {
        return types.isSubtype(typeMirror,
                elements.getTypeElement(type).asType());
    }

    private boolean isAssignable(TypeMirror typeMirror, String type) {
        return types.isAssignable(typeMirror,
                elements.getTypeElement(type).asType());
    }



}
