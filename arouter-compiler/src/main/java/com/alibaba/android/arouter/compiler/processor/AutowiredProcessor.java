package com.alibaba.android.arouter.compiler.processor;

import com.alibaba.android.arouter.compiler.utils.Consts;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.enums.TypeKind;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static com.alibaba.android.arouter.compiler.utils.Consts.*;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * Processor used to create autowired helper
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/2/20 下午5:56
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({ANNOTATION_TYPE_AUTOWIRED})
public class AutowiredProcessor extends BaseProcessor {
    private Map<TypeElement, List<Element>> parentAndChild = new HashMap<>();   // Contain field need autowired and his super class.
    private static final ClassName ARouterClass = ClassName.get("com.alibaba.android.arouter.launcher", "ARouter");
    private static final ClassName AndroidLog = ClassName.get("android.util", "Log");

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        logger.info(">>> AutowiredProcessor init. <<<");
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (CollectionUtils.isNotEmpty(set)) {
            try {
                logger.info(">>> Found autowired field, start... <<<");
                categories(roundEnvironment.getElementsAnnotatedWith(Autowired.class));
                generateHelper();

            } catch (Exception e) {
                logger.error(e);
            }
            return true;
        }

        return false;
    }

    private void generateHelper() throws IOException, IllegalAccessException {
        TypeElement type_ISyringe = elementUtils.getTypeElement(ISYRINGE);
        TypeElement type_JsonService = elementUtils.getTypeElement(JSON_SERVICE);
        TypeMirror iProvider = elementUtils.getTypeElement(Consts.IPROVIDER).asType();
        TypeMirror activityTm = elementUtils.getTypeElement(Consts.ACTIVITY).asType();
        TypeMirror fragmentTm = elementUtils.getTypeElement(Consts.FRAGMENT).asType();
        TypeMirror fragmentTmV4 = elementUtils.getTypeElement(Consts.FRAGMENT_V4).asType();

        // Build input param name.
        ParameterSpec objectParamSpec = ParameterSpec.builder(TypeName.OBJECT, "target").build();

        if (MapUtils.isNotEmpty(parentAndChild)) {
            for (Map.Entry<TypeElement, List<Element>> entry : parentAndChild.entrySet()) {
                // Build method : 'inject'
                MethodSpec.Builder injectMethodBuilder = MethodSpec.methodBuilder(METHOD_INJECT)
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(objectParamSpec);

                TypeElement parent = entry.getKey();
                List<Element> childs = entry.getValue();

                String qualifiedName = parent.getQualifiedName().toString();
                String packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
                String fileName = parent.getSimpleName() + NAME_OF_AUTOWIRED;

                logger.info(">>> Start process " + childs.size() + " field in " + parent.getSimpleName() + " ... <<<");

                TypeSpec.Builder helper = TypeSpec.classBuilder(fileName)
                        .addJavadoc(WARNING_TIPS)
                        .addSuperinterface(ClassName.get(type_ISyringe))
                        .addModifiers(PUBLIC);

                FieldSpec jsonServiceField = FieldSpec.builder(TypeName.get(type_JsonService.asType()), "serializationService", Modifier.PRIVATE).build();
                helper.addField(jsonServiceField);

                injectMethodBuilder.addStatement("serializationService = $T.getInstance().navigation($T.class)", ARouterClass, ClassName.get(type_JsonService));
                injectMethodBuilder.addStatement("$T substitute = ($T)target", ClassName.get(parent), ClassName.get(parent));


                if (types.isSubtype(parent.asType(), activityTm)) {  // Activity, then use getIntent()

                    injectMethodBuilder.addStatement("$T extras = substitute.getIntent().getExtras()", className(BUNDLE));
                } else if (types.isSubtype(parent.asType(), fragmentTm) || types.isSubtype(parent.asType(), fragmentTmV4)) {   // Fragment, then use getArguments()
                    injectMethodBuilder.addStatement("$T extras = substitute.getArguments()", className(BUNDLE));
                } else {
                    throw new IllegalAccessException("The field  need autowired from intent, its parent must be activity or fragment!");
                }


                ArrayList<Element> fieldElements = new ArrayList<>();
                // Generate method body, start inject.
                for (Element element : childs) {
                    Autowired fieldConfig = element.getAnnotation(Autowired.class);
                    String fieldName = element.getSimpleName().toString();
                    if (types.isSubtype(element.asType(), iProvider)) {  // It's provider
                        if ("".equals(fieldConfig.name())) {    // User has not set service path, then use byType.

                            // Getter
                            injectMethodBuilder.addStatement(
                                    "substitute." + fieldName + " = $T.getInstance().navigation($T.class)",
                                    ARouterClass,
                                    ClassName.get(element.asType())
                            );
                        } else {    // use byName
                            // Getter
                            injectMethodBuilder.addStatement(
                                    "substitute." + fieldName + " = ($T)$T.getInstance().build($S).navigation()",
                                    ClassName.get(element.asType()),
                                    ARouterClass,
                                    fieldConfig.name()
                            );
                        }

                        // Validator
                        if (fieldConfig.required()) {
                            injectMethodBuilder.beginControlFlow("if (substitute." + fieldName + " == null)");
                            injectMethodBuilder.addStatement(
                                    "throw new RuntimeException(\"The field '" + fieldName + "' is null, in class '\" + $T.class.getName() + \"!\")", ClassName.get(parent));
                            injectMethodBuilder.endControlFlow();
                        }
                    } else {    // It's normal intent value
                        fieldElements.add(element);
                    }
                }
                injectMethodBuilder.beginControlFlow("if (null == extras)");
                injectMethodBuilder.addStatement("return");
                injectMethodBuilder.endControlFlow();
                ClassName extraUtils = className("com.alibaba.android.arouter.utils.ExtraUtils");

                for (Element element : fieldElements) {
                    Autowired fieldConfig = element.getAnnotation(Autowired.class);
                    String fieldName =  element.getSimpleName().toString();
                    String extraKey = !StringUtils.isEmpty(fieldConfig.name()) ?
                            fieldConfig.name() : element.getSimpleName().toString();
                    String originalValue = "substitute." + fieldName;
                    String statement = "substitute." + fieldName + " = " + buildCastCode(element) + "extras.";
                    int exchangeType = typeUtils.typeExchange(element.asType());
                    if(fieldName.equals("map")){
                        logger.info(exchangeType + "exchangeType" + TypeKind.values()[exchangeType]);
                    }
                    statement = buildStatement(originalValue, statement, exchangeType, isKtClass(parent));
                    if (statement.startsWith("serializationService.")) {   // Not mortals
                        injectMethodBuilder.beginControlFlow("if (null != serializationService && $T.containsKey(extras, $S))", extraUtils, fieldName);
                        injectMethodBuilder.addStatement(
                                "substitute." + fieldName + " = " + statement,
                                (StringUtils.isEmpty(fieldConfig.name()) ? fieldName : fieldConfig.name()),
                                ClassName.get(element.asType())
                        );
                        injectMethodBuilder.nextControlFlow("else");
                        injectMethodBuilder.addStatement(
                                "$T.e(\"" + Consts.TAG + "\", \"You want automatic inject the field '" + fieldName + "' in class '$T' , then you should implement 'SerializationService' to support object auto inject!\")", AndroidLog, ClassName.get(parent));
                        injectMethodBuilder.endControlFlow();
                    } else {
                        boolean shouldCheckKey = exchangeType > TypeKind.CHARSEQUENCE.ordinal();
                        if (shouldCheckKey) {
                            injectMethodBuilder.beginControlFlow("if ($T.containsKey(extras, $S))", extraUtils, fieldName);
                        }
                        injectMethodBuilder.addStatement(statement, StringUtils.isEmpty(fieldConfig.name()) ? fieldName : fieldConfig.name());
                        if (shouldCheckKey) {
                            injectMethodBuilder.endControlFlow();
                        }
                    }
                    if (!ArrayUtils.isEmpty(fieldConfig.alternate())) {
                        for (String alterate : fieldConfig.alternate()) {
                            buildStatment(injectMethodBuilder, parent, extraUtils, element, alterate, statement, exchangeType,extraKey);
                        }
                    }

                    // Validator
                    if (fieldConfig.required() && !element.asType().getKind().isPrimitive()) {  // Primitive wont be check.
                        injectMethodBuilder.beginControlFlow("if (null == substitute." + fieldName + ")");
                        injectMethodBuilder.addStatement(
                                "$T.e(\"" + Consts.TAG + "\", \"The field '" + fieldName + "' is null, in class '\" + $T.class.getName() + \"!\")", AndroidLog, ClassName.get(parent));
                        injectMethodBuilder.endControlFlow();
                    }
                }

                helper.addMethod(injectMethodBuilder.build());

                // Generate autowire helper
                JavaFile.builder(packageName, helper.build()).build().writeTo(mFiler);

                logger.info(">>> " + parent.getSimpleName() + " has been processed, " + fileName + " has been generated. <<<");
            }

            logger.info(">>> Autowired processor stop. <<<");
        }
    }

    private void buildStatment(MethodSpec.Builder injectMethodBuilder,
                               TypeElement parent,
                               ClassName extraUtils,
                               Element element,
                               String alterate,
                               String statement,
                               int exchangeType,String defaultName) {
        String fieldName = element.getSimpleName().toString();
        if (statement.startsWith("serializationService.")) {   // Not mortals
            injectMethodBuilder.beginControlFlow("if (null != serializationService && $T.containsKey(extras, $S))", extraUtils, alterate);
            injectMethodBuilder.addStatement(
                    "substitute." + fieldName + " = " + statement,
                    (!StringUtils.isEmpty(alterate) ? alterate : defaultName),
                    ClassName.get(element.asType())
            );
            injectMethodBuilder.nextControlFlow("else");
            injectMethodBuilder.addStatement(
                    "$T.e(\"" + Consts.TAG + "\", \"You want automatic inject the field '" + alterate + "' in class '$T' , then you should implement 'SerializationService' to support object auto inject!\")", AndroidLog, ClassName.get(parent));
            injectMethodBuilder.endControlFlow();
        } else {
            boolean shouldCheckKey = exchangeType > TypeKind.CHARSEQUENCE.ordinal();
            if (shouldCheckKey) {
                injectMethodBuilder.beginControlFlow("if ($T.containsKey(extras, $S))", extraUtils, alterate);
            }
            injectMethodBuilder.addStatement(statement, !StringUtils.isEmpty(alterate) ? alterate : defaultName);
            if (shouldCheckKey) {
                injectMethodBuilder.endControlFlow();
            }
        }
    }


    private boolean isKtClass(Element element) {
        for (AnnotationMirror annotationMirror : elementUtils.getAllAnnotationMirrors(element)) {
            if (annotationMirror.getAnnotationType().toString().contains("kotlin")) {
                return true;
            }
        }

        return false;
    }

    private String buildCastCode(Element element) {
        if (typeUtils.typeExchange(element.asType()) == TypeKind.SERIALIZABLE.ordinal()) {
            return CodeBlock.builder().add("($T) ", ClassName.get(element.asType())).build().toString();
        }
        return "";
    }

    /**
     * Build param inject statement
     */
    private String buildStatement(String originalValue, String statement, int type, boolean isKt) {
        TypeKind typeKind = TypeKind.values()[type];
        if (typeKind == TypeKind.OBJECT) {
            statement = "serializationService.parseObject(extras.getString($S)" + ", new " + TYPE_WRAPPER + "<$T>(){}.getType())";
        } else {
            if (type <= TypeKind.CHARSEQUENCE.ordinal()) {
                statement += "get" + typeKind.getStatement() + "($S, " + originalValue + ")";
            } else {
                statement += "get" + typeKind.getStatement() + "($S )";
            }
        }
        return statement;
    }

    /**
     * Categories field, find his papa.
     *
     * @param elements Field need autowired
     */
    private void categories(Set<? extends Element> elements) throws IllegalAccessException {
        if (CollectionUtils.isNotEmpty(elements)) {
            for (Element element : elements) {
                TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

                if (element.getModifiers().contains(Modifier.PRIVATE)) {
                    throw new IllegalAccessException("The inject fields CAN NOT BE 'private'!!! please check field ["
                            + element.getSimpleName() + "] in class [" + enclosingElement.getQualifiedName() + "]");
                }

                if (parentAndChild.containsKey(enclosingElement)) { // Has categries
                    parentAndChild.get(enclosingElement).add(element);
                } else {
                    List<Element> childs = new ArrayList<>();
                    childs.add(element);
                    parentAndChild.put(enclosingElement, childs);
                }
            }

            logger.info("categories finished.");
        }
    }
}
