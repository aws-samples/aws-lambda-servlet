package com.aws.samples.lambda.servlet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
public @interface LambdaWebServlet {
}
