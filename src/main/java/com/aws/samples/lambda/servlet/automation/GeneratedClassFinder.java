package com.aws.samples.lambda.servlet.automation;

import com.aws.samples.lambda.servlet.LambdaWebServlet;
import com.aws.samples.lambda.servlet.LambdaWebServletProcessor;
import io.micronaut.core.io.scan.ClassPathAnnotationScanner;
import io.vavr.control.Try;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class GeneratedClassFinder {
    public List<GeneratedClassInfo> getGeneratedClassList() {
        InputStream inputStream = this.getClass().getResourceAsStream(LambdaWebServletProcessor.RESOURCE_FILE);

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
