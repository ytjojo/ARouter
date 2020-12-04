package com.alibaba.android.arouter.compiler.utils;

import com.alibaba.android.arouter.facade.enums.TypeKind;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static com.alibaba.android.arouter.compiler.utils.Consts.BOOLEAN;
import static com.alibaba.android.arouter.compiler.utils.Consts.BYTE;
import static com.alibaba.android.arouter.compiler.utils.Consts.DOUBEL;
import static com.alibaba.android.arouter.compiler.utils.Consts.FLOAT;
import static com.alibaba.android.arouter.compiler.utils.Consts.INTEGER;
import static com.alibaba.android.arouter.compiler.utils.Consts.LONG;
import static com.alibaba.android.arouter.compiler.utils.Consts.PARCELABLE;
import static com.alibaba.android.arouter.compiler.utils.Consts.SERIALIZABLE;
import static com.alibaba.android.arouter.compiler.utils.Consts.SHORT;
import static com.alibaba.android.arouter.compiler.utils.Consts.STRING;
import static com.alibaba.android.arouter.compiler.utils.Consts.CHAR;

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

        parcelableType = elements.getTypeElement(PARCELABLE).asType();
        serializableType = elements.getTypeElement(SERIALIZABLE).asType();
    }

    /**
     * Diagnostics out the true java type
     *
     * @param element Raw type
     * @return Type class of java
     */
    public int typeExchange(Element element) {
        TypeMirror typeMirror = element.asType();

        // Primitive
        if (typeMirror.getKind().isPrimitive()) {
            return element.asType().getKind().ordinal();
        }

        switch (typeMirror.toString()) {
            case BYTE:
                return TypeKind.BYTE.ordinal();
            case SHORT:
                return TypeKind.SHORT.ordinal();
            case INTEGER:
                return TypeKind.INT.ordinal();
            case LONG:
                return TypeKind.LONG.ordinal();
            case FLOAT:
                return TypeKind.FLOAT.ordinal();
            case DOUBEL:
                return TypeKind.DOUBLE.ordinal();
            case BOOLEAN:
                return TypeKind.BOOLEAN.ordinal();
            case CHAR:
                return TypeKind.CHAR.ordinal();
            case STRING:
                return TypeKind.STRING.ordinal();
            default:

                if(typeMirror instanceof DeclaredType){
                    if(isSubtype(element,"java.util.List")){
                        List<? extends TypeMirror> typeArgs = ((DeclaredType) typeMirror).getTypeArguments();
                        if (typeArgs != null && !typeArgs.isEmpty()) {
                            TypeMirror argType = typeArgs.get(0);
                            if (isSubtype(argType, "java.lang.Integer")) {
                                return TypeKind.INTEGERARRAYLIST.ordinal();
                            } else if (isSubtype(argType, "java.lang.String")) {
                                return TypeKind.STRINGARRAYLIST.ordinal();
                            } else if (isSubtype(argType, "java.lang.CharSequence")) {
                                return TypeKind.CHARSEQUENCEARRAYLIST.ordinal();
                            } else if (isSubtype(argType, "android.os.Parcelable")) {
                                return TypeKind.PARCELABLEARRAYLIST.ordinal();
                            }
                        }
                    }else if(isSubtype(element, "java.lang.CharSequence")){
                        return TypeKind.CHARSEQUENCE.ordinal();
                    }else if(isSubtype(element,"android.util.SparseArray")){
                        return TypeKind.SPARSEPARCELABLEARRAY.ordinal();
                    }
                    // Other side, maybe the PARCELABLE or SERIALIZABLE or OBJECT.
                    else if (types.isSubtype(typeMirror, parcelableType)) {
                        // PARCELABLE
                        return TypeKind.PARCELABLE.ordinal();
                    } else if (types.isSubtype(typeMirror, serializableType)) {
                        // SERIALIZABLE
                        return TypeKind.SERIALIZABLE.ordinal();
                    } else {
                        return TypeKind.OBJECT.ordinal();
                    }
                }else if(typeMirror instanceof ArrayType){
                    ArrayType arrayType = (ArrayType) typeMirror;
                    TypeMirror compType = arrayType.getComponentType();
                    if (compType instanceof PrimitiveType) {
                        switch (compType.toString()){
                            case BYTE:
                                return TypeKind.BYTEARRAY.ordinal();
                            case SHORT:
                                return TypeKind.SHORTARRAY.ordinal();
                            case INTEGER:
                               break;
                            case LONG:
                               break;
                            case FLOAT:
                                return TypeKind.FLOATARRAY.ordinal();
                            case DOUBEL:
                                break;
                            case BOOLEAN:
                                break;
                            case CHAR:
                                return TypeKind.CHARARRAY.ordinal();
                            case STRING:
                               break;
                        }
                        return TypeKind.OBJECT.ordinal();
                    } else if (compType instanceof DeclaredType) {
                        Element compElement = ((DeclaredType) compType).asElement();
                        if (compElement instanceof TypeElement) {
                            if (isSubtype(compElement, "java.lang.String")) {
                                return TypeKind.SHORTARRAY.ordinal();
                            } else if (isSubtype(compElement, "java.lang.CharSequence")) {
                                return TypeKind.CHARSEQUENCEARRAY.ordinal();
                            } else if (isSubtype(compElement, "android.os.Parcelable")) {
                                return TypeKind.SPARSEPARCELABLEARRAY.ordinal();
                            }
                            return TypeKind.OBJECT.ordinal();
                        }
                    }
                }
                return TypeKind.OBJECT.ordinal();

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

}
