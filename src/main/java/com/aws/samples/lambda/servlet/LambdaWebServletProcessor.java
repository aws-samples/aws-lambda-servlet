package com.aws.samples.lambda.servlet;

import com.aws.samples.lambda.servlet.util.ServletRequestHandler;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.servlet.annotation.WebServlet;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.aws.samples.lambda.servlet.LambdaWebServlet")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class LambdaWebServletProcessor extends AbstractProcessor {

    public static final String ADAPTER = "Adapter";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (TypeElement annotation : annotations) {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    WebServlet webServletAnnotation = element.getAnnotation(WebServlet.class);
                    String[] urlPatterns = webServletAnnotation.urlPatterns();
                    if ((urlPatterns == null) || (urlPatterns.length == 0)) {
                        throw new RuntimeException("URL patterns for a Lambda web servlet cannot be NULL");
                    }

                    int loop = 0;

                    // Loop through all of the URL patterns and make a numbered adapter class for each
                    for (String urlPattern : urlPatterns) {
                        Filer filer = processingEnv.getFiler();
                        String simpleAdapterName = element.getSimpleName() + ADAPTER + loop;
                        String packageName = element.getEnclosingElement().toString();
                        // Simply call the superclasses constructor with the URL pattern and a new, fully qualified instance of this class
                        String constructorStatement = "super(\"" + urlPattern + "\", new " + element.toString() + "())";
                        TypeSpec typeSpec = TypeSpec
                                .classBuilder(simpleAdapterName)
                                .addModifiers(Modifier.PUBLIC)
                                .superclass(ServletRequestHandler.class)
                                .addMethod(MethodSpec
                                        .constructorBuilder()
                                        .addModifiers(Modifier.PUBLIC)
                                        .addStatement(constructorStatement)
                                        .build())
                                .build();
                        JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
                        javaFile.writeTo(filer);
                    }
                }
            }

            return true;
        } catch (Exception e) {
            // We can't really deal with any kinds of failures here
            throw new RuntimeException(e);
        }
    }
}
