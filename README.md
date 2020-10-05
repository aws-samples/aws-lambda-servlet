## AWS Lambda servlet

This project makes it easier to reuse servlet code inside of AWS Lambda. It is based on the [bleshik/aws-lambda-servlet public domain project](https://github.com/bleshik/aws-lambda-servlet).

## How is this different than the public domain project?

We have fixed a few minor bugs in the original project related to case sensitivity. We also added annotation processing
that creates the servlet wrapper class automatically.

## How do I include it in my Gradle project?

1. Add the jitpack repo to the repositories section

    ```
    maven { url 'https://jitpack.io' }
    ```

2. Add the dependency version [(replace x.y.z with the appropriate version from the JitPack site)](https://jitpack.io/#aws-samples/aws-lambda-servlet)

    ```
    def awsLambdaServlet = 'x.y.z'
    ```

3. Add the annotation processor to the dependencies section

    ```
    annotationProcessor "com.github.aws-samples:aws-lambda-servlet:$awsLambdaServlet"
    ```

## Can I see an example?

Yes, an example is coming in the [IoT reference architectures repository](https://github.com/aws-samples/iot-reference-architectures/)
soon!

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This library is licensed under the MIT-0 License. See the LICENSE file.

