package com.aws.samples.lambda.servlet.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.vavr.control.Try;

import javax.servlet.*;
import java.util.*;

/**
 * Lambda handler implementation delegating the request to the given Servlet instance.
 */
public class ServletRequestHandler<T extends Servlet> implements RequestHandler<Map<String, Object>, Object> {
    protected final String contextPath;
    protected final T servlet;
    protected final Optional<? extends SessionManager> sm;
    protected final List<Filter> filters;

    public ServletRequestHandler(String contextPath, T servlet) {
        this(contextPath, servlet, Optional.empty(), Collections.emptyList());
    }

    public ServletRequestHandler(String contextPath, T servlet, Optional<? extends SessionManager> sm, Filter... filters) {
        this(contextPath, servlet, sm, Arrays.asList(filters));
    }

    public ServletRequestHandler(String contextPath, T servlet, Optional<? extends SessionManager> sm, List<Filter> filters) {
        try {
            this.contextPath = contextPath.replaceAll("^/*([^/]*)/*$", "/$1");
            this.filters = filters;
            this.sm = sm;
            this.servlet = servlet;
            this.servlet.init(new ServletConfig() {
                public String getServletName() {
                    return null;
                }

                public ServletContext getServletContext() {
                    return new DummyLambdaServletContext();
                }

                public String getInitParameter(String name) {
                    return null;
                }

                public Enumeration<String> getInitParameterNames() {
                    return Collections.enumeration(Collections.emptySet());
                }
            });
        } catch (ServletException e) {
            throw new AssertionError("Failed to initialize lambda handler", e);
        }
    }

    protected FilterChain createFilterChain(List<Filter> filters) {
        return (request, response) -> {
            if (filters.isEmpty()) {
                servlet.service(request, response);
            } else {
                filters.get(0).doFilter(
                        request,
                        response,
                        createFilterChain(
                                filters.size() > 1 ? filters.subList(1, filters.size()) : Collections.emptyList()
                        )
                );
            }
        };
    }

    protected Map<String, Object> removeContextPath(Map<String, Object> input) {
        String path = (String) (input.get("path") != null ? input.get("path") : "/");
        input.put("path", path.replaceAll("^" + this.contextPath + "/?", "/"));
        return input;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            LambdaHttpServletRequest request = new LambdaHttpServletRequest(removeContextPath(input), sm);
            InMemoryHttpServletResponse response = new InMemoryHttpServletResponse();

            // Set the logger from the Lambda context
            ((DummyLambdaServletContext) this.servlet.getServletConfig().getServletContext()).setLogger(context.getLogger());

            // Likely we have no filters so this really just calls the function and services the request
            createFilterChain(filters).doFilter(request, response);

            return new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER) {
                {
                    int code = response.getStatus();
                    put("statusCode", code < 100 ? 200 : code);
                    TreeMap<String, String> headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {{
                        for (String h : response.getHeaderNames()) {
                            put(h, response.getHeader(h));
                        }
                    }};

                    put("headers", headers);

                    if (likelyBinary(this)) {
                        // Need to base 64 encode this
                        put("body", Base64.getEncoder().encodeToString(response.getBytes()));
                        put("isBase64Encoded", true);
                    } else {
                        put("body", response.toString());
                    }
                }
            };
        } catch (Exception exception) {
            exception.printStackTrace();
            Try.run(() -> context.getLogger().log("Exception: " + exception.getMessage() + ", Failed to handle the request"))
                    .orElseRun(throwable -> System.err.println("Exception 1: " + exception.getMessage() + ", Exception 2: " + throwable.getMessage()));
            return lambdaHttpResponse(500, "Internal Server Error: " + exception.getMessage());
        }
    }

    boolean likelyBinary(TreeMap<String, Object> response) {
        TreeMap<String, String> headers = (TreeMap<String, String>) response.get("headers");

        if ("gzip".equals(headers.get("content-encoding"))) {
            // Binary, gzip compressed
            return true;
        }

        String contentType = headers.get("content-type");

        if (contentType == null) {
            // No way to tell, no content type
            return false;
        }

        if (contentType.contains("image")) {
            // Images are binary
            return true;
        }

        // Octet streams are binary, everything else is assumed to not be
        return contentType.contains("octet");
    }

    protected Map<String, Object> lambdaHttpResponse(int code, String message) {
        return new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER) {{
            put("statusCode", code);
            put("headers", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER) {{
                put("Content-Type", "text/plain");
            }});
            put("body", message);
        }};
    }
}
