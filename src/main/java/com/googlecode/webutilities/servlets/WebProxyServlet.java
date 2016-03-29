package com.googlecode.webutilities.servlets;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import static com.googlecode.webutilities.util.Utils.*;

public class WebProxyServlet extends HttpServlet {

    public static final String INIT_PARAM_BASE_URI = "baseUri";

    public static final String INIT_PARAM_ENABLE_CORS = "enableCors";

    public static final String INIT_PARAM_INJECT_REQUEST_HEADERS = "injectRequestHeaders";

    public static final String INIT_PARAM_INJECT_RESPONSE_HEADERS = "injectResponseHeaders";

    private String baseUri;

    private boolean enableCors;

    private Map<String, String> requestHeadersToInject;

    private Map<String, String> responseHeadersToInject;

    private static final Logger LOGGER = LoggerFactory.getLogger(WebProxyServlet.class.getName());


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        this.baseUri = readString(config.getInitParameter(INIT_PARAM_BASE_URI), "");

        this.enableCors = readBoolean(config.getInitParameter(INIT_PARAM_ENABLE_CORS), false);

        this.requestHeadersToInject = buildHeadersMapFromString(
                readString(config.getInitParameter(INIT_PARAM_INJECT_REQUEST_HEADERS), ""));

        this.responseHeadersToInject = buildHeadersMapFromString(
                readString(config.getInitParameter(INIT_PARAM_INJECT_RESPONSE_HEADERS), ""));

        LOGGER.debug("Servlet initialized: {\n\t{}:{},\n\t{}:{}\n}",
                INIT_PARAM_BASE_URI, this.baseUri,
                INIT_PARAM_ENABLE_CORS, this.enableCors
        );
    }


    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        if (this.enableCors) {
            if ("OPTIONS".equals(req.getMethod())) {
                resp.setStatus(200);
                this.setCorsHeaders(resp);
                return;
            }
        }

        this.makeProxyRequest(req, resp);
        this.injectResponseHeader(resp);
    }

    private void injectResponseHeader(HttpServletResponse resp) {
        this.responseHeadersToInject.forEach(resp::setHeader);
    }

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        resp.setHeader("Access-Control-Allow-Headers",
                "X-Requested-With, X-Requested-By, Content-Type, origin, authorization, accept, client-security-token");
    }

    private HttpUriRequest getRequest(String method, String url) {
        if ("POST".equals(method)) {
            return new HttpPost(url);
        } else if ("PUT".equals(method)) {
            return new HttpPut(url);
        } else if ("DELETE".equals(method)) {
            return new HttpDelete(url);
        } else if ("OPTIONS".equals(method)) {
            return new HttpOptions(url);
        } else if ("HEAD".equals(method)) {
            return new HttpHead(url);
        }
        return new HttpGet(url);
    }

    private void makeProxyRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String thisServletPath = req.getServletPath();
        String query = req.getQueryString();
        String url = req.getRequestURI();

        String targetUrl = this.baseUri + (url.substring(url.indexOf(thisServletPath) + thisServletPath.length()));
        targetUrl += "?" + query;

        HttpUriRequest request = getRequest(req.getMethod(), targetUrl);
        // Inject response headers
        this.requestHeadersToInject.forEach(request::setHeader);

        // Proxy
        this.copyResponse(HttpClients.createDefault().execute(request), resp);

        // Inject response headers
        this.responseHeadersToInject.forEach(resp::setHeader);

    }

    private void copyResponse(CloseableHttpResponse fromResponse, HttpServletResponse toResponse) throws IOException {
        toResponse.setStatus(fromResponse.getStatusLine().getStatusCode());
        for (Header header : fromResponse.getAllHeaders()) {
            toResponse.setHeader(header.getName(), header.getValue());
        }
        toResponse.setLocale(fromResponse.getLocale());
        HttpEntity entity = fromResponse.getEntity();
        toResponse.setContentType(entity.getContentType().getValue());
        toResponse.setContentLength((int) entity.getContentLength());
        IOUtils.copy(entity.getContent(), toResponse.getOutputStream());
    }

}

