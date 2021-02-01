package com.aws.samples.lambda.servlet.util;

import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps the "pass-through" AWS Lambda parameters into an HTTP request.
 */
@SuppressWarnings("unchecked")
public class LambdaHttpServletRequest implements HttpServletRequest {
    private static final Logger log = Logger.getLogger(LambdaHttpServletRequest.class);
    private final Optional<? extends SessionManager> sm;
    private final Map<String, Object> input;
    private final Map<String, Object> context;
    private final Map<String, String> identity;
    private final Map<String, String> querystring;
    private final Map<String, String> header;
    private String body;

    public LambdaHttpServletRequest(Map<String, Object> input, Optional<? extends SessionManager> sm) {
        this.sm = sm;
        this.input = input;
        this.context = extractContextFromInput(input, "requestContext");
        this.identity = extractDataFromInput(input, "identity");
        this.querystring = extractDataFromInput(input, "queryStringParameters");
        this.header = extractDataFromInput(input, "headers");
        this.body = (String) input.get("body");

        if (input.containsKey("isBase64Encoded")) {
            boolean isBase64Encoded = (boolean) input.get("isBase64Encoded");

            if (isBase64Encoded) {
                this.body = new String(Base64.getDecoder().decode(body));
            }
        }

        if (this.body == null) {
            this.body = "";
        }
    }

    private TreeMap<String, String> extractDataFromInput(Map<String, Object> inputMap, String key) {
        TreeMap<String, String> output = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        Map<String, String> existing = (Map<String, String>) inputMap.get(key);

        if (existing != null) {
            output.putAll(existing);
        }

        return output;
    }

    private TreeMap<String, Object> extractContextFromInput(Map<String, Object> inputMap, String key) {
        TreeMap<String, Object> output = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        Map<String, String> existing = (Map<String, String>) inputMap.get(key);

        if (existing != null) {
            output.putAll(existing);
        }

        return output;
    }

    public String getMethod() {
        return (String) input.get("httpMethod");
    }

    public String getPathInfo() {
        return (String) input.get("path");
    }

    public String getPathTranslated() {
        return getPathInfo();
    }

    public String getContextPath() {
        return "";
    }

    public String getQueryString() {
        return querystring.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    public String getRemoteUser() {
        return null;
    }

    public boolean isUserInRole(String role) {
        return false;
    }

    public java.security.Principal getUserPrincipal() {
        return null;
    }

    public String getRequestedSessionId() {
        return null;
    }

    public String getRequestURI() {
        return getPathInfo();
    }

    public StringBuffer getRequestURL() {
        return new StringBuffer("http://localhost:80/" + getPathInfo());
    }

    public String getServletPath() {
        return "";
    }

    public HttpSession getSession(boolean create) {
        return sm.map(m -> m.getSession(this, create)).orElse(null);
    }

    public HttpSession getSession() {
        return getSession(true);
    }

    public String changeSessionId() {
        return null;
    }

    public boolean isRequestedSessionIdValid() {
        return false;
    }

    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

    public boolean authenticate(HttpServletResponse response) {
        return false;
    }

    public void login(String username, String password) {
    }

    public void logout() {
    }

    public Collection<Part> getParts() {
        return Collections.emptySet();
    }

    public Part getPart(String name) {
        return null;
    }

    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
        return null;
    }

    public String getAuthType() {
        return null;
    }

    public Cookie[] getCookies() {
        String cookie = getHeader("Cookie");

        if (cookie == null) {
            return new Cookie[0];
        }
        try {
            List<Cookie> cookies = Arrays.asList(cookie.split("; ")).stream().map(c -> {
                return new Cookie(c.substring(0, c.indexOf("=")), c.substring(c.indexOf("=") + 1));
            }).collect(Collectors.toList());
            return cookies.toArray(new Cookie[cookies.size()]);
        } catch (Exception e) {
            return new Cookie[0];
        }
    }

    public long getDateHeader(String name) {
        return System.currentTimeMillis();
    }

    public String getHeader(String name) {
        return header.get(name);
    }

    public Enumeration<String> getHeaders(String name) {
        return Collections.enumeration(Collections.singleton(getHeader(name)));
    }

    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(header.keySet());
    }

    public int getIntHeader(String name) {
        return Integer.parseInt(getHeader(name));
    }

    public String getRemoteAddr() {
        return identity.get("sourceIp");
    }

    public int getContentLength() {
        return body.length();
    }

    public String getContentType() {
        return getHeader("Content-Type");
    }

    public String getCharacterEncoding() {
        return "UTF-8";
    }

    public void setCharacterEncoding(String env) {
    }

    public Locale getLocale() {
        return Locale.getDefault();
    }

    public String getScheme() {
        return "http";
    }

    public int getServerPort() {
        return 80;
    }

    public String getServerName() {
        return "lambda";
    }

    public DispatcherType getDispatcherType() {
        return DispatcherType.REQUEST;
    }

    public AsyncContext getAsyncContext() {
        throw new IllegalStateException();
    }

    public AsyncContext startAsync() throws IllegalStateException {
        throw new IllegalStateException();
    }

    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        throw new IllegalStateException();
    }

    public boolean isAsyncStarted() {
        return false;
    }

    public boolean isAsyncSupported() {
        return false;
    }

    public ServletContext getServletContext() {
        return null;
    }

    public int getLocalPort() {
        return 80;
    }

    public String getLocalAddr() {
        return "127.0.0.1";
    }

    public String getLocalName() {
        return "lo0";
    }

    public int getRemotePort() {
        return 0;
    }

    public String getRealPath(String path) {
        return "";
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    public boolean isSecure() {
        return false;
    }

    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(Collections.singleton(getLocale()));
    }

    public Object getAttribute(String name) {
        return null;
    }

    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(Collections.emptySet());
    }

    public long getContentLengthLong() {
        return getContentLength();
    }

    public ServletInputStream getInputStream() {
        return new ServletInputStream() {

            private final InputStream bodyStream = new ByteArrayInputStream(body.getBytes());
            private int next;
            private ReadListener listener;
            private boolean finished;

            public int read() throws IOException {
                if (finished) {
                    return -1;
                }
                int toReturn;
                if (next != -1) {
                    toReturn = next;
                    next = -1;
                }
                toReturn = bodyStream.read();
                if (toReturn == -1) {
                    finished = true;
                    if (listener != null) {
                        listener.onAllDataRead();
                    }
                }
                return toReturn;
            }

            public boolean isFinished() {
                try {
                    return finished || next == -1 && (next = bodyStream.read()) == -1;
                } catch (IOException e) {
                    return true;
                }
            }

            public boolean isReady() {
                return !isFinished();
            }

            public void setReadListener(ReadListener readListener) {
                listener = readListener;
                try {
                    readListener.onDataAvailable();
                } catch (IOException e) {
                }
            }
        };
    }

    public String getParameter(String name) {
        return querystring.get(name);
    }

    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(querystring.keySet());
    }

    public String[] getParameterValues(String name) {
        return new String[]{getParameter(name)};
    }

    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String name : querystring.keySet()) {
            result.put(name, getParameterValues(name));
        }
        return result;
    }

    public String getProtocol() {
        return "HTTP/1.1";
    }

    public BufferedReader getReader() {
        return new BufferedReader(new StringReader(body));
    }

    public String getRemoteHost() {
        return getRemoteAddr();
    }

    public void setAttribute(String name, Object o) {
    }

    public void removeAttribute(String name) {
    }

}
