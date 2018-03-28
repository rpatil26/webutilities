/*
 * Copyright 2010-2016 Rajendra Patil
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.webutilities.servlets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

import static com.googlecode.webutilities.common.Constants.*;
import static com.googlecode.webutilities.util.Utils.*;


/**
 * The <code>JSCSSMergeServet</code> is the Http Servlet to combine multiple JS or CSS static resources in one HTTP request.
 * using YUICompressor.
 * <p>
 * Using <code>JSCSSMergeServet</code> the multiple JS or CSS resources can grouped together (by adding comma) in one HTTP call.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * Put the <b>webutilities-x.y.z.jar</b> in your classpath (WEB-INF/lib folder of your webapp).
 * </p>
 * <p>
 * Declare this servlet in your <code>web.xml</code> ( web descriptor file)
 * </p>
 * <pre>
 * ...
 * &lt;servlet&gt;
 * 	&lt;servlet-name&gt;JSCSSMergeServet&lt;/servlet-name&gt;</b>
 * 	&lt;servlet-class&gt;<b>com.googlecode.webutilities.JSCSSMergeServet</b>&lt;/servlet-class&gt;
 * 	&lt;!-- This init param is optional and default value is minutes for 7 days in future. To expire in the past use negative value. --&gt;
 * 	&lt;init-param&gt;
 * 		&lt;param-name&gt;expiresMinutes&lt;/param-name&gt;
 * 		&lt;param-value&gt;7200&lt;/param-value&gt; &lt;!-- 5 days --&gt;
 * 	&lt;/init-param&gt;
 * 	&lt;!-- This init param is also optional and default value is true. Set it false to override. --&gt;
 * 	&lt;init-param&gt;
 * 		&lt;param-name&gt;useCache&lt;/param-name&gt;
 * 		&lt;param-value&gt;false&lt;/param-value&gt;
 * 	&lt;/init-param&gt;
 *  &lt;/servlet&gt;
 * ...
 * </pre>
 * Map this servlet to serve your JS and CSS resources
 * <pre>
 * ...
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;JSCSSMergeServet&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;<b>*.js</b>&lt;/url-pattern&gt;
 *   &lt;url-pattern&gt;<b>*.json</b>&lt;/url-pattern&gt;
 *   &lt;url-pattern&gt;<b>*.css</b>&lt;/url-pattern&gt;
 * &lt;/servlet-mapping>
 * ...
 * </pre>
 * <p>
 * In your web pages (HTML or JSP files) combine your multiple JS or CSS in one request as shown below.
 * </p>
 * <p>To serve multiple JS files through one HTTP request</p>
 * <pre>
 * &lt;script language="JavaScript" src="<b>/myapp/js/prototype,controls,dragdrop,myapp.js</b>"&gt;&lt;/script&gt;
 * </pre>
 * <p>To serve multiple CSS files through one HTTP request</p>
 * <pre>
 * &lt;link rel="StyleSheet" href="<b>/myapp/css/infra,calendar,aquaskin.css</b>"/&gt;
 * </pre>
 * <p>
 * Also if you wanted to serve them minified all together then you can add <code>YUIMinFilter</code> on them. See <code>YUIMinFilter</code> from <code>webutilities.jar</code> for details.
 * </p>
 * <h3>Init Parameters</h3>
 * <p>
 * Both init parameters are optional.
 * </p>
 * <p>
 * <b>expiresMinutes</b> has default value of 7 days. This value is relative from current time. Use negative value to expire early in the past.
 * Ideally you should never be using negative value otherwise you won't be able to <b>take advantage of browser caching for static resources</b>.
 * </p>
 * <pre>
 *  <b>expiresMinutes</b> - Relative number of minutes (added to current time) to be set as Expires header
 *  <b>useCache</b> - to cache the earlier merged contents and serve from cache. Default true.
 *  <b>overrideExistingHeaders</b> - override headers if they exist. Default true.
 * </pre>
 * <h3>Dependency</h3>
 * <p>Servlet and JSP api (mostly provided by servlet container eg. Tomcat).</p>
 * <p><b>servlet-api.jar</b> - Must be already present in your webapp classpath</p>
 * <h3>Notes on Cache</h3>
 * <p>If you have not set useCache parameter to false then cache will be used and contents will be always served from cache if found.
 * Sometimes you may not want to use cache or you may want to evict the cache then using URL parameters you can do that.
 * </p>
 * <h4>URL Parameters to skip or evict the cache</h4>
 * <pre>
 * <b>_skipcache_</b> - The JS or CSS request URL if contains this parameters the cache will not be used for it.
 * <b>_dbg_</b> - same as above _skipcache_ parameters.
 * <b>_expirecache_</b> - The cache will be cleaned completely. All existing cached contents will be cleaned.
 * </pre>
 * <pre>
 * <b>Eg.</b>
 * &lt;link rel="StyleSheet" href="/myapp/css/infra,calendar,aquaskin.css<b>?_dbg=1</b>"/&gt;
 * or
 * &lt;script language="JavaScript" src="/myapp/js/prototype,controls,dragdrop,myapp.js<b>?_expirecache_=1</b>"&gt;&lt;/script&gt;
 * </pre>
 * <h3>Limitations</h3>
 * <p>
 * The multiple JS or CSS files <b>can be combined together in one request if they are in same parent path</b>. eg. <code><b>/myapp/js/a.js</b></code>, <code><b>/myapp/js/b.js</b></code> and <code><b>/myapp/js/c.js</b></code>
 * can be combined together as <code><b>/myapp/js/a,b,c.js</b></code>. If they are not in infra path then they can not be combined in one request. Same applies for CSS too.
 * </p>
 * <p>
 * Visit http://code.google.com/p/webutilities/wiki/JSCSSMergeServlet for more details.
 * Also visit http://code.google.com/p/webutilities/wiki/AddExpiresHeader for details about how to use for setting
 * expires/Cache control header.
 *
 * @author rpatil
 * @version 2.0
 */
public class JSCSSMergeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String INIT_PARAM_EXPIRES_MINUTES = "expiresMinutes";

    public static final String INIT_PARAM_CACHE_CONTROL = "cacheControl";

    public static final String INIT_PARAM_AUTO_CORRECT_URLS_IN_CSS = "autoCorrectUrlsInCSS";

    public static final String INIT_PARAM_TURN_OFF_E_TAG = "turnOffETag";

    public static final String INIT_PARAM_TURN_OFF_URL_FINGERPRINTING = "turnOffUrlFingerPrinting";

    public static final String INIT_PARAM_CUSTOM_CONTEXT_PATH_FOR_CSS_URLS = "customContextPathForCSSUrls";

    public static final String INIT_PARAM_OVERRIDE_EXISTING_HEADERS = "override";

    private long expiresMinutes = DEFAULT_EXPIRES_MINUTES; //default value 7 days

    private String cacheControl = DEFAULT_CACHE_CONTROL; //default

    private boolean autoCorrectUrlsInCSS = true; //default

    private boolean overrideExistingHeaders = true; // default

    private boolean turnOffETag = false; //default enable eTag

    private static final Logger LOGGER = LoggerFactory.getLogger(JSCSSMergeServlet.class.getName());

    private String customContextPathForCSSUrls; // filling this will replace the default value: request.getContextPath()

    private boolean turnOffUrlFingerPrinting = false; //default enabled fingerprinting

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.expiresMinutes = readLong(config.getInitParameter(INIT_PARAM_EXPIRES_MINUTES), this.expiresMinutes);
        this.cacheControl = config.getInitParameter(INIT_PARAM_CACHE_CONTROL) != null ? config.getInitParameter(INIT_PARAM_CACHE_CONTROL) : this.cacheControl;
        this.autoCorrectUrlsInCSS = readBoolean(config.getInitParameter(INIT_PARAM_AUTO_CORRECT_URLS_IN_CSS), this.autoCorrectUrlsInCSS);
        this.turnOffETag = readBoolean(config.getInitParameter(INIT_PARAM_TURN_OFF_E_TAG), this.turnOffETag);
        this.turnOffUrlFingerPrinting = readBoolean(config.getInitParameter(INIT_PARAM_TURN_OFF_URL_FINGERPRINTING), this.turnOffUrlFingerPrinting);
        this.customContextPathForCSSUrls = config.getInitParameter(INIT_PARAM_CUSTOM_CONTEXT_PATH_FOR_CSS_URLS);
        this.overrideExistingHeaders = readBoolean(config.getInitParameter(INIT_PARAM_OVERRIDE_EXISTING_HEADERS), this.overrideExistingHeaders);
        LOGGER.debug("Servlet initialized: {\n\t{}:{},\n\t{}:{},\n\t{}:{},\n\t{}:{}\n\t{}:{}\n:{}\n}",
                INIT_PARAM_EXPIRES_MINUTES, String.valueOf(this.expiresMinutes),
                INIT_PARAM_CACHE_CONTROL, this.cacheControl,
                INIT_PARAM_AUTO_CORRECT_URLS_IN_CSS, String.valueOf(this.autoCorrectUrlsInCSS),
                INIT_PARAM_TURN_OFF_E_TAG, String.valueOf(this.turnOffETag),
                INIT_PARAM_TURN_OFF_URL_FINGERPRINTING, String.valueOf(this.turnOffUrlFingerPrinting),
                INIT_PARAM_OVERRIDE_EXISTING_HEADERS, String.valueOf(this.overrideExistingHeaders)
        );
    }

    /**
     * @param extensionOrFile  - .css or .js etc. (lower case) or the absolute path of the file in case of image files
     * @param resourcesToMerge - from request
     * @param hashForETag      - from request
     * @param resp             - response object
     */

    private void addAppropriateResponseHeaders(String extensionOrFile, List<String> resourcesToMerge, String hashForETag, HttpServletResponse resp) {
        String mime = selectMimeForExtension(extensionOrFile);
        if (mime != null) {
            LOGGER.trace("Setting MIME to {}", mime);
            resp.setContentType(mime);
        }
        long lastModifiedFor = getLastModifiedFor(resourcesToMerge, this.getServletContext());

        if (this.overrideExistingHeaders) {
            resp.setDateHeader(HEADER_EXPIRES, new Date().getTime() + expiresMinutes * 60 * 1000);
            resp.setHeader(HTTP_CACHE_CONTROL_HEADER, this.cacheControl);
            resp.setDateHeader(HEADER_LAST_MODIFIED, lastModifiedFor);
            if (hashForETag != null && !this.turnOffETag) {
                resp.setHeader(HTTP_ETAG_HEADER, hashForETag);
            }
        } else {
            resp.addDateHeader(HEADER_EXPIRES, new Date().getTime() + expiresMinutes * 60 * 1000);
            resp.addHeader(HTTP_CACHE_CONTROL_HEADER, this.cacheControl);
            resp.addDateHeader(HEADER_LAST_MODIFIED, lastModifiedFor);
            if (hashForETag != null && !this.turnOffETag) {
                resp.addHeader(HTTP_ETAG_HEADER, hashForETag);
            }
        }
        resp.addHeader(HEADER_X_OPTIMIZED_BY, X_OPTIMIZED_BY_VALUE);
        LOGGER.trace("Added expires, last-modified & ETag headers");
    }

    /* (non-Javadoc)
    * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
    */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = this.getURL(req);

        LOGGER.debug("Started processing request : {}", url);

        List<String> resourcesToMerge = findResourcesToMerge(req.getContextPath(), url);
        String extensionOrPath = detectExtension(url);//in case of non js/css files it null
        if (extensionOrPath.isEmpty()) {
            extensionOrPath = resourcesToMerge.get(0);//non grouped i.e. non css/js file, we refer it's path in that case
        }

        //If not modified, return 304 and stop
        ResourceStatus status = JSCSSMergeServlet.isNotModified(this.getServletContext(), req, resourcesToMerge, this.turnOffETag);
        if (status.isNotModified()) {
            LOGGER.trace("Resources Not Modified. Sending 304.");
            String ETag = !this.turnOffETag ? status.getActualETag() : null;
            JSCSSMergeServlet.sendNotModified(resp, extensionOrPath, ETag, this.expiresMinutes, this.cacheControl, this.overrideExistingHeaders);
            return;
        }


        //Add appropriate headers
        this.addAppropriateResponseHeaders(extensionOrPath, resourcesToMerge, status.getActualETag(), resp);

        OutputStream outputStream = resp.getOutputStream();
        String contextPathForCss = customContextPathForCSSUrls != null ?
                customContextPathForCSSUrls : req.getContextPath();
        ProcessedResult processedResult = this.processResources(contextPathForCss, outputStream, resourcesToMerge);
        int resourcesNotFound = processedResult.getNumberOfMissingResources();

        if (resourcesNotFound > 0 && resourcesNotFound == resourcesToMerge.size()) { //all resources not found
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            LOGGER.warn("All resources are not found. Sending 404.");
            return;
        }
        if (outputStream != null) {
            try {
                resp.setStatus(HttpServletResponse.SC_OK);
                outputStream.close();
            } catch (Exception e) {
                // ignore
            }
        }
        resp.setHeader("Content-Length", String.valueOf(processedResult.contentLength));
        LOGGER.debug("Finished processing Request : {}", url);
    }

    public static void sendNotModified(HttpServletResponse response, String extensionOrFile, String hashForETag,
                                       long expiresMinutes, String cacheControl) {
        sendNotModified(response, extensionOrFile, hashForETag, expiresMinutes, cacheControl, false);
    }

    /**
     * @param response httpServletResponse
     */
    public static void sendNotModified(HttpServletResponse response, String extensionOrFile, String hashForETag,
                                       long expiresMinutes, String cacheControl, boolean overrideExistingHeaders) {
        response.setContentLength(0);
        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        String mime = selectMimeForExtension(extensionOrFile);
        if (mime != null) {
            LOGGER.trace("Setting MIME to {}", mime);
            response.setContentType(mime);
        }
        if (overrideExistingHeaders) {
            response.setDateHeader(HEADER_EXPIRES, new Date().getTime() + expiresMinutes * 60 * 1000);
        } else {
            response.addDateHeader(HEADER_EXPIRES, new Date().getTime() + expiresMinutes * 60 * 1000);
        }
        if (cacheControl != null) {
            if (overrideExistingHeaders) {
                response.setHeader(HTTP_CACHE_CONTROL_HEADER, cacheControl);
            } else {
                response.addHeader(HTTP_CACHE_CONTROL_HEADER, cacheControl);
            }
        }
        if (hashForETag != null /*&& !this.turnOffETag*/) {
            if (overrideExistingHeaders) {
                response.setHeader(HTTP_ETAG_HEADER, hashForETag);
            } else {
                response.addHeader(HTTP_ETAG_HEADER, hashForETag);
            }
        }
        response.addHeader(HEADER_X_OPTIMIZED_BY, X_OPTIMIZED_BY_VALUE);
        LOGGER.trace("Added expires, last-modified & ETag headers");
    }

    /**
     * @param request HttpServletRequest
     * @return URL with fingerprint removed if had any
     */
    private String getURL(HttpServletRequest request) {
        return removeFingerPrint(request.getRequestURI());
    }

    /**
     * @param request          - HttpServletRequest
     * @param resourcesToMerge - list of resources relative paths
     * @return true if not modified based on if-None-Match and If-Modified-Since
     */
    public static ResourceStatus isNotModified(ServletContext context, HttpServletRequest request, List<String> resourcesToMerge, boolean turnOffETag) {
        //If-Modified-Since
        String ifModifiedSince = request.getHeader(HTTP_IF_MODIFIED_SINCE);
        if (ifModifiedSince != null) {
            Date date = readDateFromHeader(ifModifiedSince);
            if (date != null) {
                if (!isAnyResourceModifiedSince(resourcesToMerge, date.getTime(), context)) {
                    return new ResourceStatus(null, true);
                }
            }
        }
        //If-None-match
        String requestETag = request.getHeader(HTTP_IF_NONE_MATCH_HEADER);
        String actualETag = turnOffETag ? null : buildETagForResources(resourcesToMerge, context);
        if (!turnOffETag && !isAnyResourceETagModified(resourcesToMerge, requestETag, actualETag, context)) {
            return new ResourceStatus(actualETag, true);
        }
        return new ResourceStatus(actualETag, false);
    }

    /**
     * @param contextPath      HttpServletRequest context path
     * @param outputStream     - OutputStream
     * @param resourcesToMerge list of resources to merge
     * @return number of non existing, unprocessed resources
     */

    private ProcessedResult processResources(String contextPath, OutputStream outputStream, List<String> resourcesToMerge) {

        int missingResourcesCount = 0;

        long contentLength = 0;

        ServletContext context = this.getServletContext();
        boolean addNewLine = false;
        for (String resourcePath : resourcesToMerge) {

            LOGGER.trace("Processing resource : {}", resourcePath);

            InputStream is = null;
            byte[] newLineBytes = "\n".getBytes();
            try {
                is = context.getResourceAsStream(resourcePath);
                if (is == null) {
                    missingResourcesCount++;
                    continue;
                }
                if (this.isCSS(resourcePath) && autoCorrectUrlsInCSS) { //Need to deal with images url in CSS

                    contentLength += this.processCSS(contextPath, resourcePath, is, outputStream);

                } else {
                    byte[] buffer = new byte[128];
                    int c;
                    //Add extra new line to avoid merging issues when there is single line comment in the end of file
                    if (addNewLine) {
                        outputStream.write(newLineBytes);
                        contentLength += newLineBytes.length;
                    }
                    while ((c = is.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, c);
                        contentLength += c;
                    }
                    addNewLine = true;
                }
            } catch (IOException e) {
                LOGGER.error("Error while reading resource : {}", resourcePath);
                LOGGER.error("IOException: ", e);
            }

            try {
                is.close();
            } catch (IOException ex) {
                LOGGER.warn("Failed to close stream:", ex);
            }
            try {
                outputStream.flush();
            } catch (IOException ex) {
                LOGGER.error("Failed to flush out: {}", outputStream);
            }

        }
        return new ProcessedResult(missingResourcesCount, contentLength);
    }

    protected boolean isCSS(String resourcePath) {
        return resourcePath != null && resourcePath.endsWith(EXT_CSS);
    }

    /**
     * @param cssFilePath  - css file path
     * @param contextPath  - context path or custom configured context path
     * @param inputStream  - input stream
     * @param outputStream - output stream
     * @throws IOException - thrown in case anything (IO read/write) goes wrong
     */
    private long processCSS(String contextPath, String cssFilePath, InputStream inputStream, OutputStream outputStream) throws IOException {
        ServletContext context = this.getServletContext();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, DEFAULT_CHARSET));
        String line;
        long bytesWritten = 0;
        StringBuffer buffer = new StringBuffer();
        while ((line = bufferedReader.readLine()) != null) {
            buffer.setLength(0);
            buffer.append(line);
            line = this.processCSSLine(context, contextPath, cssFilePath, buffer);
            byte[] bytes = (line + "\n").getBytes(DEFAULT_CHARSET);
            outputStream.write(bytes);
            bytesWritten += bytes.length;
        }
        return bytesWritten;
    }

    /**
     * @param context     - ServletContext
     * @param contextPath - APP context path or any custom configured context path
     * @param cssFilePath - css file path
     * @param input        - single line css file
     * @return - processed line with img path if it had any replaced to appropriate path
     */
    private String processCSSLine(ServletContext context, String contextPath, String cssFilePath, StringBuffer input) {
        Matcher matcher = CSS_IMG_URL_PATTERN.matcher(input);
        String cssRealPath = context.getRealPath(cssFilePath);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String refImgPath = matcher.group(1);
            if (!isProtocolURL(refImgPath)) { //ignore absolute protocol paths
                String fullMatch = matcher.group(0);
                LOGGER.debug("before match {}", fullMatch);
                String resolvedImgPath = refImgPath;
                if (!refImgPath.startsWith("/")) {
                    resolvedImgPath = buildProperPath(getParentPath(cssFilePath), refImgPath);
                }
                String imgRealPath = context.getRealPath(resolvedImgPath);
                updateReferenceMap(cssRealPath, imgRealPath);
                LOGGER.trace("before path {}", resolvedImgPath);
                String fullPath = contextPath + (this.turnOffUrlFingerPrinting ? resolvedImgPath
                        : addFingerPrint(buildETagForResource(resolvedImgPath, context),
                        resolvedImgPath));
                LOGGER.trace("after path {}", fullPath);
                String matchReplace = fullMatch.replace(refImgPath, fullPath);
                LOGGER.debug("after replace {}", matchReplace);
                matcher.appendReplacement(output, matchReplace);
            }
        }
        matcher.appendTail(output);
        input.setLength(0);
        return output.toString();
    }

    /**
     * Class to store resource ETag and modified status
     */
    public static class ResourceStatus {

        private String actualETag;

        private boolean notModified = true;

        ResourceStatus(String actualETag, boolean notModified) {
            this.actualETag = actualETag;
            this.notModified = notModified;
        }

        public String getActualETag() {
            return actualETag;
        }

        public boolean isNotModified() {
            return notModified;
        }

    }

    private class ProcessedResult {

        private int numberOfMissingResources;

        private long contentLength;

        private ProcessedResult(int numberOfMissingResources, long contentLength) {
            this.numberOfMissingResources = numberOfMissingResources;
            this.contentLength = contentLength;
        }

        public int getNumberOfMissingResources() {
            return numberOfMissingResources;
        }

        public long getContentLength() {
            return contentLength;
        }
    }
}

