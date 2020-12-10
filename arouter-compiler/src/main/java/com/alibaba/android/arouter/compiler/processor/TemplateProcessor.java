package com.alibaba.android.arouter.compiler.processor;

import com.alibaba.android.arouter.compiler.utils.Consts;
import com.alibaba.android.arouter.facade.annotation.Action;
import com.alibaba.android.arouter.facade.annotation.Flags;
import com.alibaba.android.arouter.facade.annotation.Query;
import com.alibaba.android.arouter.facade.annotation.RequestCode;
import com.alibaba.android.arouter.facade.annotation.TargetPath;
import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import static com.alibaba.android.arouter.compiler.utils.Consts.ANNOTATION_TYPE_TAEGETPATH;
import static com.alibaba.android.arouter.compiler.utils.Consts.METHOD_LOAD_INTO;
import static com.alibaba.android.arouter.compiler.utils.Consts.NAME_OF_MULTIIMPLEMENT;
import static com.alibaba.android.arouter.compiler.utils.Consts.NAME_OF_TEMPLATE;
import static com.alibaba.android.arouter.compiler.utils.Consts.PACKAGE_OF_GENERATE_FILE;
import static com.alibaba.android.arouter.compiler.utils.Consts.PROJECT;
import static com.alibaba.android.arouter.compiler.utils.Consts.SEPARATOR;
import static com.alibaba.android.arouter.compiler.utils.Consts.WARNING_TIPS;
import static javax.lang.model.element.Modifier.PUBLIC;

@AutoService(Processor.class)
@SupportedAnnotationTypes(ANNOTATION_TYPE_TAEGETPATH)
public class TemplateProcessor extends BaseProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (CollectionUtils.isNotEmpty(annotations)) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(TargetPath.class);
            try {
                parseAnnotations(elements);
            } catch (Exception e) {
                logger.error(e);
            }
            return true;
        }

        return false;
    }

    private void parseAnnotations(Set<? extends Element> elements) throws IOException {
        if (CollectionUtils.isEmpty(elements)) {
            return;
        }
        HashMap<TypeElement, LinkedHashSet<ExecutableElement>> typeMap = new HashMap<>();
        for (Element element : elements) {
            ExecutableElement executableElement = MoreElements.asExecutable(element);
            TypeElement typeElement = MoreElements.asType(executableElement.getEnclosingElement());
            LinkedHashSet<ExecutableElement> methods = typeMap.get(typeElement);
            if (methods == null) {
                methods = new LinkedHashSet<>();
                typeMap.put(typeElement, methods);
            }
            methods.add(executableElement);
        }
        ClassName arouter = className(Consts.AROUTER);
        ClassName postcard = className(Consts.POSTCARD);
        ParameterizedTypeName inputTemplateMapTypeOfGroup = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(Class.class),
                ClassName.get(Class.class)
        );
        ParameterSpec templateParamSpec = ParameterSpec.builder(inputTemplateMapTypeOfGroup, "templates").build();
        MethodSpec.Builder loadIntoMethodOfRootBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(templateParamSpec);
        for (Map.Entry<TypeElement, LinkedHashSet<ExecutableElement>> entry : typeMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            Set<ExecutableElement> methods = entry.getValue();
            if (typeElement.getKind() != ElementKind.INTERFACE) {
                logger.error("TargetPath must be used in interface");
            }
            String generatePackage = getPackage(typeElement).toString();
            String generateName = PROJECT + SEPARATOR + typeElement.getSimpleName().toString() + "Impl";
            TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(generateName)
                    .addModifiers(PUBLIC)
                    .addJavadoc(WARNING_TIPS)
                    .addSuperinterface(ClassName.get(typeElement));
            loadIntoMethodOfRootBuilder.addStatement("templates.put($T.class,$T.class)", ClassName.get(typeElement), ClassName.get(generatePackage, generateName));
            for (ExecutableElement executableElement : methods) {
                TargetPath targetPath = executableElement.getAnnotation(TargetPath.class);
                MethodSpec.Builder methodBuilder = getMethod(executableElement);
                List<? extends VariableElement> parameters = executableElement.getParameters();
                boolean hasReturnType = executableElement.getReturnType().getKind() != TypeKind.VOID;
                TypeMirror returnType = executableElement.getReturnType();
                if (CollectionUtils.isEmpty(parameters)) {
                    if (hasReturnType) {
                        if (isSubType(returnType, Consts.POSTCARD)) {
                            methodBuilder.addStatement("return ($T)$T.getInstance().build($S)", ClassName.get(returnType), arouter, targetPath.value());
                        } else if (isSubType(returnType, Consts.INTENT)) {
                            methodBuilder.addStatement("return ($T)$T.getInstance().build($S).setForIntent().navigation()", ClassName.get(returnType), arouter, targetPath.value());
                        } else {
                            methodBuilder.addStatement("return ($T)$T.getInstance().build($S).navigation()", ClassName.get(returnType), arouter, targetPath.value());
                        }
                    } else {
                        methodBuilder.addStatement("$T.getInstance().build($S).navigation()", arouter, targetPath.value());
                    }

                } else {
                    methodBuilder.addStatement("$T postcard = $T.getInstance().build($S)", postcard, arouter, targetPath.value());
                    Flags intentFlags = null;
                    Action action = null;
                    VariableElement activity = null;
                    VariableElement navigationCallback = null;
                    VariableElement context = null;
                    VariableElement requestCodeVar = null;
                    for (VariableElement var : parameters) {
                        String varName = var.getSimpleName().toString();
                        RequestCode requestCode = var.getAnnotation(RequestCode.class);

                        if (intentFlags == null) {
                            intentFlags = var.getAnnotation(Flags.class);

                        }
                        if (action == null) {
                            action = var.getAnnotation(Action.class);

                        }
                        if (requestCode != null) {
                            requestCodeVar = var;
                        } else if (intentFlags != null) {
                            methodBuilder.addStatement("postcard.withFlags($L)", varName);
                        } else if (action != null) {
                            methodBuilder.addStatement("postcard.withAction($L)", varName);
                        } else {
                            if (isSubType(var.asType(), Consts.ACTIVITY)) {
                                activity = var;
                            } else if (isSubType(var.asType(), Consts.CONTEXT)) {
                                context = var;
                            } else if (isSubType(var.asType(), Consts.NAVIGATIONCALLBACK)) {
                                navigationCallback = var;
                            } else if (isSubType(var.asType(), Consts.URI)) {
                                methodBuilder.addStatement("postcard.withIntentData($L)", varName);
                            } else {
                                int type = typeUtils.typeExchange(var.asType());
                                com.alibaba.android.arouter.facade.enums.TypeKind typeKind = com.alibaba.android.arouter.facade.enums.TypeKind.values()[type];
                                Query query = var.getAnnotation(Query.class);
                                String key = query != null ? query.value() : varName;
                                methodBuilder.addStatement("postcard.with$L($S, $L)", typeKind.getStatement(), key, varName);
                            }
                        }
                    }
                    if (action == null) {
                        action = executableElement.getAnnotation(Action.class);
                        if (action != null) {
                            methodBuilder.addStatement("postcard.withAction($S)", action.value());
                        }
                    }
                    String returnCode = hasReturnType ? "return (" + returnType.toString() + ")" : "";
                    if (hasReturnType && isSubType(returnType,Consts.INTENT)) {
                        methodBuilder.addStatement("postcard.setForIntent()");
                    }
                    if (hasReturnType && isSubType(returnType, Consts.POSTCARD)) {
                        methodBuilder.addStatement("return postcard");
                    } else if (activity != null) {
                        if (requestCodeVar == null) {
                            RequestCode requestCode = executableElement.getAnnotation(RequestCode.class);
                            if (requestCode != null) {
                                methodBuilder.addStatement("postcard.navigation($L, $L, $L)", activity.getSimpleName().toString(), requestCode.value(), navigationCallback == null ? "null" : navigationCallback.getSimpleName().toString());
                                if (hasReturnType) {
                                    methodBuilder.addStatement("return null");
                                }
                            } else {
                                methodBuilder.addStatement(returnCode + "postcard.navigation($L, $L)", activity.getSimpleName().toString(), navigationCallback == null ? "null" : navigationCallback.getSimpleName().toString());
                            }
                        } else {
                            methodBuilder.addStatement("postcard.navigation($L, $L, $L)", activity.getSimpleName().toString(), requestCodeVar.getSimpleName().toString(), navigationCallback == null ? "null" : navigationCallback.getSimpleName().toString());
                            if (hasReturnType) {
                                methodBuilder.addStatement("return null");
                            }
                        }
                    } else {
                        methodBuilder.addStatement(returnCode + "postcard.navigation($L, $L)", context == null ? "null" : context.getSimpleName().toString(), navigationCallback == null ? "null" : navigationCallback.getSimpleName().toString());
                    }

                }
                typeBuilder.addMethod(methodBuilder.build());
            }


            JavaFile.builder(generatePackage,
                    typeBuilder.build()
            ).build().writeTo(mFiler);
        }

        JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                TypeSpec.classBuilder(NAME_OF_TEMPLATE + SEPARATOR + moduleName)
                        .addModifiers(PUBLIC)
                        .addJavadoc(WARNING_TIPS)
                        .addMethod(loadIntoMethodOfRootBuilder.build())
                        .addSuperinterface(className(Consts.TEMPLATE_GROUP))
                        .build()
        ).build().writeTo(mFiler);


    }

    public Element getPackage(TypeElement typeElement) {
        Element enclosing = typeElement.getEnclosingElement();
        while (enclosing != null) {
            if (enclosing != null && enclosing.getKind() == ElementKind.PACKAGE) {
                return enclosing;
            } else {
                enclosing = enclosing.getEnclosingElement();
            }
        }
        return null;
    }

    public MethodSpec.Builder getMethod(ExecutableElement executableElement) {
        return MethodSpec.overriding(executableElement);

    }
}
