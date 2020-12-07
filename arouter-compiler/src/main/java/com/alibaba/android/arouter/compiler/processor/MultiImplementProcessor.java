package com.alibaba.android.arouter.compiler.processor;

import com.alibaba.android.arouter.compiler.utils.AnnotationElementUtil;
import com.alibaba.android.arouter.compiler.utils.Consts;
import com.alibaba.android.arouter.facade.annotation.MultiImplement;
import com.alibaba.android.arouter.facade.enums.RouteType;
import com.alibaba.android.arouter.facade.model.RouteMeta;
import com.google.auto.common.AnnotationMirrors;
import com.google.auto.common.AnnotationValues;
import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.google.common.base.Optional;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor7;

import static com.alibaba.android.arouter.compiler.utils.Consts.ANNOTATION_TYPE_MULTIIMPLEMENT;
import static com.alibaba.android.arouter.compiler.utils.Consts.IINTERCEPTOR;
import static com.alibaba.android.arouter.compiler.utils.Consts.IINTERCEPTOR_GROUP;
import static com.alibaba.android.arouter.compiler.utils.Consts.METHOD_LOAD_INTO;
import static com.alibaba.android.arouter.compiler.utils.Consts.NAME_OF_INTERCEPTOR;
import static com.alibaba.android.arouter.compiler.utils.Consts.NAME_OF_MULTIIMPLEMENT;
import static com.alibaba.android.arouter.compiler.utils.Consts.PACKAGE_OF_GENERATE_FILE;
import static com.alibaba.android.arouter.compiler.utils.Consts.SEPARATOR;
import static com.alibaba.android.arouter.compiler.utils.Consts.WARNING_TIPS;
import static javax.lang.model.element.Modifier.PUBLIC;

@AutoService(Processor.class)
@SupportedAnnotationTypes(ANNOTATION_TYPE_MULTIIMPLEMENT)
public class MultiImplementProcessor extends BaseProcessor {
    /**
     * {@inheritDoc}
     *
     * @param annotations
     * @param roundEnv
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (CollectionUtils.isNotEmpty(annotations)) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(MultiImplement.class);
            try {
                parseInterceptors(elements);
            } catch (Exception e) {
                logger.error(e);
            }
            return true;
        }

        return false;
    }

    /**
     * Parse tollgate.
     *
     * @param elements elements of tollgate.
     */
    private void parseInterceptors(Set<? extends Element> elements) throws IOException {
        if (CollectionUtils.isNotEmpty(elements)) {
            logger.info(">>> Found interceptors, size is " + elements.size() + " <<<");


            // Interface of ARouter.
            TypeElement type_ITollgateGroup = elementUtils.getTypeElement(Consts.MULTIIMPLEMENT_GROUP);

            // Build input param name.
            ParameterSpec tollgateParamSpec = ParameterSpec.builder(className(Consts.IMULTIIMPLEMENTREGISTER), "register").build();

            // Build method : 'loadInto'
            MethodSpec.Builder loadIntoMethodOfTollgateBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(tollgateParamSpec);
            ClassName routeMetaCn = ClassName.get(RouteMeta.class);
            ClassName routeTypeCn = ClassName.get(RouteType.class);

            // Verify and cache, sort incidentally.
            for (Element element : elements) {
                if (element.asType().getKind() != TypeKind.DECLARED) {
                    continue;
                }
                logger.info("A interceptor verify over, its " + element.asType());
                MultiImplement multiImplement = element.getAnnotation(MultiImplement.class);

                List<TypeElement> annotationTypeElements = AnnotationElementUtil.getAnnotationTypes(element,MultiImplement.class, "value");
                if (!CollectionUtils.isEmpty(annotationTypeElements)) {
                    TypeElement keyElement = annotationTypeElements.get(0);
                    if (!isSubType(element, keyElement.asType())) {
                        logger.error("not subtype of A interceptor verify failed, its " + element.asType());
                    }
                    // Generate
                    if (null != keyElement) {
                        // Build method body
                        ClassName keyClassName = className(getClassName(keyElement.asType()));
                        loadIntoMethodOfTollgateBuilder.addStatement("register.add($T.class" + ", $T.build($T." + RouteType.MULTIIMPLEMENTS + ", $T.class  " + ", $T.class,  " + multiImplement.priority() + "))",
                                keyClassName,
                                routeMetaCn,
                                routeTypeCn,
                                className(getClassName(element.asType())),
                                keyClassName
                                );

                    }

                } else {
                    logger.error("no value for MultiImplement, A interceptor verify failed, its " + element.asType());
                }


            }
            // Write to disk(Write file even interceptors is empty.)
            JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                    TypeSpec.classBuilder(NAME_OF_MULTIIMPLEMENT + SEPARATOR + moduleName)
                            .addModifiers(PUBLIC)
                            .addJavadoc(WARNING_TIPS)
                            .addMethod(loadIntoMethodOfTollgateBuilder.build())
                            .addSuperinterface(ClassName.get(type_ITollgateGroup))
                            .build()
            ).build().writeTo(mFiler);

            logger.info(">>> Interceptor group write over. <<<");
        }
    }
}
