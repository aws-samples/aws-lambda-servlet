package com.aws.samples.lambda.servlet.automation;

import com.aws.samples.lambda.servlet.LambdaWebServletProcessor;
import io.vavr.control.Try;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class GeneratedClassFinder {
    public List<GeneratedClassInfo> getGeneratedClassList(JarFile jarFile) {
        return Try.of(() -> jarFile.getJarEntry(LambdaWebServletProcessor.WAR_RESOURCE_FILE))
                .mapTry(jarFile::getInputStream)
                .map(this::innerGetGeneratedClassList)
                .get();
    }

    private List<GeneratedClassInfo> getGeneratedClassList() {
        return innerGetGeneratedClassList(this.getClass().getClassLoader().getResourceAsStream(LambdaWebServletProcessor.WAR_RESOURCE_FILE));
    }

    private List<GeneratedClassInfo> innerGetGeneratedClassList(InputStream inputStream) {
        if (inputStream == null) {
            return new ArrayList<>();
        }

        List<String> lines = new ArrayList<>();
        new Scanner(inputStream).forEachRemaining(lines::add);

        return lines.stream()
                .map(line -> line.split("="))
                .map(Arrays::asList)
                .map(value -> new GeneratedClassInfo(value.get(0), value.get(1)))
                .collect(Collectors.toList());
    }
}
