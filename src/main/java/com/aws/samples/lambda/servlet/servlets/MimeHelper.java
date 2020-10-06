package com.aws.samples.lambda.servlet.servlets;

import java.io.File;

public interface MimeHelper {
    String detect(File file);
}
