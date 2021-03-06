package com.aws.samples.lambda.servlet.automation;

import com.aws.samples.lambda.servlet.LambdaWebServletProcessor;
import io.vavr.collection.List;
import io.vavr.control.Try;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.jar.JarFile;

public class GeneratedClassFinder {
    public List<GeneratedClassInfo> getGeneratedClassList(JarFile jarFile) {
        return Try.of(() -> jarFile.getJarEntry(LambdaWebServletProcessor.RESOURCE_FILE))
                .mapTry(jarFile::getInputStream)
                .map(this::innerGetGeneratedClassList)
                .getOrElse(List.empty());
    }

    private List<GeneratedClassInfo> getGeneratedClassList() {
        return innerGetGeneratedClassList(this.getClass().getClassLoader().getResourceAsStream(LambdaWebServletProcessor.RESOURCE_FILE));
    }

    private List<GeneratedClassInfo> innerGetGeneratedClassList(InputStream inputStream) {
        if (inputStream == null) {
            return List.empty();
        }

        java.util.List<String> lines = new ArrayList<>();
        new Scanner(inputStream).forEachRemaining(lines::add);

        return List.ofAll(lines.stream()
                .map(line -> line.split("="))
                .map(Arrays::asList)
                .map(value -> new GeneratedClassInfo(value.get(0), value.get(1))));
    }
}
