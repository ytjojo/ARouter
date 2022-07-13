package com.alibaba.android.arouter.compiler.utils;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public class AnnotationElementUtil {

    public static List<TypeElement>  getAnnotationTypes(Element element, Class<? extends Annotation> annotation,String key){
        Optional<AnnotationMirror> annotationMirror=  MoreElements.getAnnotationMirror(element, annotation);
        if(annotationMirror.isPresent()){
            Iterable<TypeMirror> klasses = MoreAnnotationMirrors.getTypeValue(annotationMirror.get(), key);
            List<TypeElement> typeElements = FluentIterable.from(klasses).transform(
                    new Function<TypeMirror, TypeElement>() {
                        @Override
                        public TypeElement apply(TypeMirror klass) {
                            return MoreTypes.asTypeElement(klass);
                        }
                    }).toList();
            return typeElements;
        }
        return null;
    }
}
