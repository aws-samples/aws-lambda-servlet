package com.aws.samples.lambda.servlet.servlets;

import io.vavr.control.Option;
import io.vavr.control.Try;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

public abstract class AbstractStaticFileServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Combine the input request URI with the static asset prefix, then remove the leading slash if necessary
        String requestUri = req.getRequestURI();

        if (requestUri.startsWith(req.getContextPath())) {
            requestUri = requestUri.substring(req.getContextPath().length());
        }

        if (requestUri.equals("/")) {
            requestUri = "/index.html";
        }

        String baseFilename = String.join("", getPrefix(), requestUri);

        // This is the filename that AWS Lambda would use
        String lambdaFilename = baseFilename.replaceFirst("/+", "");

        // This is the filename that a local debug environment would use
        String localFilename = baseFilename.replaceFirst("//", "/");

        // Always try to get the AWS Lambda file first since performance counts the most there
        Option<InputStream> inputStreamOption = Option.of(getClass().getClassLoader().getResourceAsStream(lambdaFilename));

        if (inputStreamOption.isEmpty()) {
            // Didn't find the AWS Lambda file, maybe we are debugging locally
            inputStreamOption = Option.of(getServletContext().getResource(localFilename))
                    .map(url -> Try.of(url::openStream).getOrNull());
        }

        if (inputStreamOption.isEmpty()) {
            // Didn't find the file in either place
            resp.setStatus(404);
            return;
        }

        InputStream inputStream = inputStreamOption.get();

        Optional<String> optionalMimeType = Optional.empty();

        if (requestUri.endsWith(".js")) {
            // For some reason the "*.nocache.js" file gets picked up by Tika as "text/x-matlab"
            optionalMimeType = Optional.of("application/javascript");
        } else if (requestUri.endsWith(".html")) {
            optionalMimeType = Optional.of("text/html");
        } else if (requestUri.endsWith(".png")) {
            optionalMimeType = Optional.of("image/png");
        } else if (requestUri.endsWith(".jpg")) {
            optionalMimeType = Optional.of("image/jpeg");
        } else if (requestUri.endsWith(".css")) {
            optionalMimeType = Optional.of("text/css");
        } else {
            // No MIME type detected, use the optional MIME helper if possible
            String finalRequestUri = requestUri;

            optionalMimeType = Try.of(this::getOptionalMimeHelper)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(mimeHelper -> mimeHelper.detect(finalRequestUri, inputStream))
                    .map(Optional::of)
                    .getOrElse(Optional::empty);
        }

        // Only set the MIME type if we found it
        optionalMimeType.ifPresent(resp::setContentType);

        resp.setStatus(200);

        // Throw an exception if the stream copy fails
        Try.run(() -> copyStream(inputStream, resp.getOutputStream())).get();
    }

    private void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = inputStream.read(buf)) > 0) {
            outputStream.write(buf, 0, length);
        }
    }

    public Optional<MimeHelper> getOptionalMimeHelper() {
        // Override this if you want to use Apache Tika or another MIME type helper
        return Optional.empty();
    }

    public abstract String getPrefix();
}
