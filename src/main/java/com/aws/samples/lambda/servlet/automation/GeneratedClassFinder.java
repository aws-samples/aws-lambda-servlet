package com.aws.samples.lambda.servlet.automation;

import com.aws.samples.lambda.servlet.LambdaWebServlet;
import com.aws.samples.lambda.servlet.LambdaWebServletProcessor;
import io.micronaut.core.io.scan.ClassPathAnnotationScanner;
import io.vavr.control.Try;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GeneratedClassFinder {
    public List<Class<?>> getGeneratedClassList(File jarFile, String packageName) {
        URLClassLoader urlClassLoader = new URLClassLoader(
                new URL[]{Try.of(() -> jarFile.toURI().toURL()).get()},
                this.getClass().getClassLoader()
        );

        ClassPathAnnotationScanner classPathAnnotationScanner = new ClassPathAnnotationScanner(urlClassLoader);
        List<Class> comClassesWithLambdaWebServletAnnotation = classPathAnnotationScanner
                .scan(LambdaWebServlet.class.getName(), packageName)
                .collect(Collectors.toList());

        List<Class<?>> generatedClassList = new ArrayList<>();

        for (Class baseClass : comClassesWithLambdaWebServletAnnotation) {
            String baseName = baseClass.getName() + LambdaWebServletProcessor.ADAPTER;

            int loop = 0;
            Try<? extends Class<?>> classTry = null;

            do {
                String generatedName = baseName + loop;
                loop++;
                classTry = Try.of(() -> urlClassLoader.loadClass(generatedName));
                classTry.map(generatedClassList::add);
            } while (classTry.isSuccess());
        }

        return generatedClassList;
    }
}
