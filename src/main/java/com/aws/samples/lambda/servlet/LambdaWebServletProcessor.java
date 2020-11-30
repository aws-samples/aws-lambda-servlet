package com.aws.samples.lambda.servlet;

import com.aws.samples.cdk.constructs.iam.permissions.HasIamPermissions;
import com.aws.samples.lambda.servlet.util.ServletRequestHandler;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import io.vavr.control.Try;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.servlet.annotation.WebServlet;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.aws.samples.lambda.servlet.LambdaWebServlet")
public class LambdaWebServletProcessor extends AbstractProcessor {
    public static final String ADAPTER = "Adapter";

    public static final String RESOURCE_FILE = "META-INF/services/" + LambdaWebServletProcessor.class.getName();

    private Map<String, String> classToUrl = new HashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            generateConfigFiles();
            classToUrl = new HashMap<>();
        } else {
            processAnnotations(annotations, roundEnv);
        }

        return true;
    }

    // Some guidance from https://github.com/google/auto/blob/6bed859f25a8f164506b9fa7437bdbd32ccf1cd0/service/processor/src/main/java/com/google/auto/service/processor/AutoServiceProcessor.java#L162
    private void generateConfigFiles() {
        Filer filer = processingEnv.getFiler();

        List<String> existingServlets = Try.of(() -> filer.getResource(StandardLocation.CLASS_OUTPUT, "", RESOURCE_FILE))
                .mapTry(FileObject::openInputStream)
                .map(this::readFile)
                .getOrElse(new ArrayList<>());

        // Throw an exception if opening the output stream fails
        OutputStream outputStream = Try.of(() -> filer.createResource(StandardLocation.CLASS_OUTPUT, "", RESOURCE_FILE))
                .mapTry(FileObject::openOutputStream)
                .get();

        List<String> newServlets = classToUrl.entrySet().stream()
                .map(entry -> String.join("=", entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        List<String> finalServlets = new ArrayList<>();
        finalServlets.addAll(newServlets);
        finalServlets.addAll(existingServlets);
        finalServlets = finalServlets.stream().distinct().collect(Collectors.toList());

        String output = String.join("\n", finalServlets);
        Try.run(() -> outputStream.write(output.getBytes(StandardCharsets.UTF_8))).get();
        Try.run(outputStream::close);
    }

    private List<String> readFile(InputStream inputStream) {
        List<String> output = new ArrayList<>();
        new Scanner(inputStream).forEachRemaining(output::add);

        return output;
    }

    private boolean processAnnotations(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
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
                        String fullAdapterName = String.join(".", packageName, simpleAdapterName);
                        classToUrl.put(fullAdapterName, urlPattern);
                        TypeSpec.Builder typeSpecBuilder = TypeSpec
                                .classBuilder(simpleAdapterName)
                                .addModifiers(Modifier.PUBLIC)
                                .superclass(ServletRequestHandler.class)
                                .addSuperinterface(HasIamPermissions.class);

                        typeSpecBuilder = addConstructor(typeSpecBuilder, urlPattern, element);
                        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(List.class, HasIamPermissions.class);
                        typeSpecBuilder.addMethod(MethodSpec.methodBuilder("getPermissions")
                                .returns(parameterizedTypeName)
                                .addCode(getReturnPermissionsCodeBlock(element))
                                .build());

                        JavaFile javaFile = JavaFile.builder(packageName, typeSpecBuilder.build()).build();
                        javaFile.writeTo(filer);
                        loop++;
                    }
                }
            }

            return true;
        } catch (Exception e) {
            // We can't really deal with any kinds of failures here
            throw new RuntimeException(e);
        }
    }

    private TypeSpec.Builder addConstructor(TypeSpec.Builder typeSpecBuilder, String urlPattern, Element element) {
        // Simply call the superclasses constructor with the URL pattern and a new, fully qualified instance of this class
        String constructorStatement = "super(\"" + urlPattern + "\", new " + element.toString() + "())";
        return typeSpecBuilder
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement(constructorStatement)
                        .build());
    }
    
    private CodeBlock getReturnPermissionsCodeBlock(Element element) {
        return CodeBlock
                .builder()
                .beginControlFlow("if (HasIamPermissions.class.isAssignableFrom(" + element.toString() + "))")
                .addStatement("return new " + element.toString() + "().getPermissions()")
                .endControlFlow()
                .addStatement("return new ArrayList<>()")
                .build();
    }
}