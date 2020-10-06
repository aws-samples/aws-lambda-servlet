package com.aws.samples.lambda.servlet.servlets;

import io.vavr.control.Try;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public abstract class StaticFileServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(StaticFileServlet.class);

    private Optional<MimeHelper> optionalMimeHelper = Optional.empty();

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

        String filename = String.join("", getPrefix(), requestUri).replaceFirst("^/", "");

        URL url = Try.of(() -> this.getClass().getClassLoader().getResource(filename))
                .onFailure(throwable -> log.error(throwable.getMessage())).get();

        if (url == null) {
            resp.setStatus(404);
            return;
        }

        URI uri = Try.of(url::toURI)
                .onFailure(throwable -> log.error(throwable.getMessage())).get();
        Path path = Paths.get(uri);
        Optional<String> optionalMimeType = Optional.empty();

        String pathString = path.toString();

        if (pathString.endsWith(".js")) {
            // For some reason the "*.nocache.js" file gets picked up by Tika as "text/x-matlab"
            optionalMimeType = Optional.of("application/javascript");
        } else if (pathString.endsWith(".html")) {
            optionalMimeType = Optional.of("text/html");
        } else if (pathString.endsWith(".png")) {
            optionalMimeType = Optional.of("image/png");
        } else if (pathString.endsWith(".jpg")) {
            optionalMimeType = Optional.of("image/jpeg");
        } else if (pathString.endsWith(".css")) {
            optionalMimeType = Optional.of("text/css");
        } else if (optionalMimeHelper.isPresent()) {
            // No MIME type detected, use the optional MIME helper
            optionalMimeType = Optional.of(optionalMimeHelper.get().detect(path.toFile()));
        }

        // Only set the MIME type if we found it
        optionalMimeType.ifPresent(resp::setContentType);
        byte[] data = Files.readAllBytes(path);

        resp.setStatus(200);
        resp.getOutputStream().write(data);
    }

    public Optional<MimeHelper> getOptionalMimeHelper() {
        // Override this if you want to use Apache Tika or another MIME type helper
        return Optional.empty();
    }

    public abstract String getPrefix();
}
