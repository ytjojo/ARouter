package com.alibaba.android.arouter.compiler.utils;

import com.alibaba.android.arouter.compiler.processor.BaseProcessor;
import com.alibaba.android.arouter.facade.annotation.Action;
import com.alibaba.android.arouter.facade.annotation.Flags;
import com.alibaba.android.arouter.facade.annotation.Query;
import com.alibaba.android.arouter.facade.annotation.RequestCode;
import com.alibaba.android.arouter.facade.enums.TypeKind;
import com.alibaba.android.arouter.facade.model.RouteMeta;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;

import static com.alibaba.android.arouter.compiler.utils.Consts.NAME_OF_METHODEINVOKER;
import static com.alibaba.android.arouter.compiler.utils.Consts.PACKAGE_OF_GENERATE_FILE;
import static com.alibaba.android.arouter.compiler.utils.Consts.SEPARATOR;
import static com.alibaba.android.arouter.compiler.utils.Consts.WARNING_TIPS;
import static javax.lang.model.element.Modifier.PUBLIC;

public class MethodInvokerGenerator {


    public static MethodSpec.Builder getInvokeMethodSpec(ClassName context, ClassName postcard) {
        ParameterSpec contextSpec = ParameterSpec.builder(context, "context").build();
        ParameterSpec postcardSpec = ParameterSpec.builder(postcard, "postcard").build();
        MethodSpec.Builder invoke = MethodSpec.methodBuilder("invoke")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(contextSpec)
                .returns(ClassName.get(Object.class))
                .addParameter(postcardSpec);
        return invoke;
    }

    public static MethodSpec.Builder addStatement(MethodSpec.Builder invokeMethodSpec, RouteMeta routeMeta, BaseProcessor processor) {
        if (invokeMethodSpec == null) {
            invokeMethodSpec = MethodInvokerGenerator.getInvokeMethodSpec(processor.className(Consts.CONTEXT), processor.className(Consts.POSTCARD));
            invokeMethodSpec.addStatement("final String path = postcard.getPath()");
            invokeMethodSpec.beginControlFlow("if($S.equals(path))", routeMeta.getPath());
        } else {
            invokeMethodSpec.nextControlFlow("else if($S.equals(path))", routeMeta.getPath());
        }
        ExecutableElement executableElement = (ExecutableElement) routeMeta.getRawType();
        List<? extends VariableElement> parameters = executableElement.getParameters();
        ClassName typeClassName = processor.className(((TypeElement) executableElement.getEnclosingElement()).getQualifiedName().toString());
        if (CollectionUtils.isEmpty(parameters)) {
            if (executableElement.getKind() == ElementKind.CONSTRUCTOR) {
                invokeMethodSpec.addStatement("return new $T()", typeClassName);
            } else {
                if (executableElement.getReturnType().getKind() == javax.lang.model.type.TypeKind.VOID) {
                    invokeMethodSpec.addStatement("$T.$L()", typeClassName, executableElement.getSimpleName().toString());
                    invokeMethodSpec.addStatement("return null");
                } else {
                    invokeMethodSpec.addStatement("return $T.$L()", typeClassName, executableElement.getSimpleName().toString());
                }
            }

        } else {
            StringBuilder sb = new StringBuilder();
            ArrayList args = new ArrayList();
            args.add(typeClassName);
            boolean addReturnNull = false;
            if (executableElement.getKind() == ElementKind.CONSTRUCTOR) {
                sb.append("return new $T(");
            } else {
                args.add(executableElement.getSimpleName().toString());
                if (executableElement.getReturnType().getKind() == javax.lang.model.type.TypeKind.VOID) {
                    sb.append("$T.$L(");
                    addReturnNull = true;
                } else {
                    sb.append("return $T.$L(");
                }

            }
            int index = 0;
            for (VariableElement var : parameters) {
                Query query = var.getAnnotation(Query.class);
                String varName = var.getSimpleName().toString();
                boolean isPostcard = false;
                boolean isContext = false;
                boolean isUri = false;
                boolean isAction = false;
                boolean isNavigationCallback = false;
                boolean isFlags = false;
                boolean isRequestCode = false;
                if (processor.isSubType(var.asType(), Consts.POSTCARD)) {
                    isPostcard = true;
                } else if (processor.isSubType(var.asType(), Consts.CONTEXT)) {
                    isContext = true;
                } else if (processor.isSubType(var.asType(), Consts.URI)) {
                    isUri = true;
                } else if (processor.isSubType(var.asType(), Consts.NAVIGATIONCALLBACK)) {
                    isNavigationCallback = true;
                } else if (var.getAnnotation(Action.class) != null && processor.isSubType(var.asType(), Consts.STRING)) {
                    isAction = true;
                } else if (var.getAnnotation(Flags.class) != null && (var.asType().getKind().isPrimitive() || processor.isSubType(var.asType(), Consts.INTEGER))) {
                    isFlags = true;
                } else if (var.getAnnotation(RequestCode.class) != null && (var.asType().getKind().isPrimitive() || processor.isSubType(var.asType(), Consts.INTEGER))) {
                    isRequestCode = true;
                }
                if (index != 0) {
                    sb.append(", ");
                }
                if (query == null) {
                    if (isPostcard) {
                        sb.append("postcard");

                    } else if (isContext) {
                        if(processor.isSubType(var.asType(), Consts.ACTIVITY)){
                            sb.append("($T)context");
                            args.add(processor.className(Consts.ACTIVITY));
                        }else {
                            sb.append("context");
                        }

                    } else if (isAction) {
                        sb.append("postcard.getAction() == null ? $S : postcard.getAction()");
                        args.add(var.getAnnotation(Action.class).value());
                    } else if (isFlags) {
                        sb.append("postcard.getFlags()");
                    } else if (isRequestCode) {
                        sb.append("postcard.getRequestCode()");
                    } else if (isUri) {
                        sb.append("postcard.getIntentData()");
                    } else if (isNavigationCallback) {
                        sb.append("postcard.getNavigationCallback()");
                    } else {
                        getValueStatment(varName, sb, var, processor, args);
                    }

                } else {
                    String key = StringUtils.isEmpty(query.value()) ? varName : query.value();
                    getValueStatment(key, sb, var, processor, args);
                }
                index++;
            }
            sb.append(")");
            invokeMethodSpec.addStatement(sb.toString(), args.toArray());
            if (addReturnNull) {
                invokeMethodSpec.addStatement("return null");
            }
        }
        return invokeMethodSpec;
    }

    private static void getValueStatment(String key, StringBuilder sb, VariableElement var, BaseProcessor processor, ArrayList args) {
        int type = getExchangeType(var, processor);
        TypeKind typeKind = TypeKind.values()[type];
        if(typeKind == TypeKind.OBJECT){
            sb.append("($T)postcard.get");

            args.add(getClassname(var));
        }else {
            if(type >= TypeKind.SERIALIZABLE.ordinal()){
                sb.append("($T)");
                args.add(getClassname(var));

            }
            sb.append("postcard.getExtras().get");
        }
        sb.append(typeKind.getStatement());
        sb.append("($S)");
        args.add(key);
    }
    public static ClassName getClassname(VariableElement var){
        Element element = ((DeclaredType)var.asType()).asElement();
        String  packageName = element.getEnclosingElement().asType().toString();
        String name = element.getSimpleName().toString();
        return ClassName.get(packageName,name);

    }

    public static int getExchangeType(VariableElement variableElement, BaseProcessor processor) {
        return processor.typeUtils.typeExchange(variableElement.asType());
    }

    public static void writeMethodInvokeFile(String moduleName, MethodSpec.Builder invokeSpecBuilder, BaseProcessor processor) throws IOException {
        JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                TypeSpec.classBuilder(NAME_OF_METHODEINVOKER + SEPARATOR + moduleName)
                        .addModifiers(PUBLIC)
                        .addJavadoc(WARNING_TIPS)
                        .addMethod(invokeSpecBuilder.build())
                        .addSuperinterface(processor.className(Consts.IMETHODERINVOKER))
                        .build()
        ).build().writeTo(processor.mFiler);
    }
}
