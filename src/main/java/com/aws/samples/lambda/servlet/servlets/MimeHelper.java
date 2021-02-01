package com.aws.samples.lambda.servlet.servlets;

import java.io.File;
import java.io.InputStream;

public interface MimeHelper {
    String detect(File file);

    String detect(InputStream file);

    String detect(String filename, InputStream file);
}
