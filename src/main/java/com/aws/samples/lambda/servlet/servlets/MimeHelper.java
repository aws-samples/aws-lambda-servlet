package com.aws.samples.lambda.servlet.servlets;

import io.vavr.control.Option;

import java.io.File;
import java.io.InputStream;

public interface MimeHelper {
    String detect(File file);

    String detect(InputStream file);

    String detect(Option<String> filename, InputStream file);
}
