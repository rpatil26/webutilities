/*
 * Copyright 2010-2011 Rajendra Patil
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.googlecode.webutilities.modules.ne;

import com.googlecode.webutilities.modules.infra.ModuleRequest;
import com.googlecode.webutilities.modules.infra.ModuleResponse;
import com.googlecode.webutilities.util.Utils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;

import static com.googlecode.webutilities.common.Constants.*;

public class JSCSSMergeModule implements IModule {

    public static final Logger LOGGER = LoggerFactory.getLogger(JSCSSMergeModule.class.getName());

    @Override
    public DirectivePair parseDirectives(String ruleString) {

        DirectivePair pair = null;

        int index = 0;

        boolean autoCorrectUrlsInCss = true;

        String[] splits = ruleString.split("\\s+");

        assert splits.length >= 1;

        if (!splits[index++].equals(JSCSSMergeModule.class.getSimpleName())) return pair;

        if (splits.length > 1) {

            if ("autoCorrectUrlsInCss".equals(splits[index++])) {
                if (splits.length > 2) {
                    autoCorrectUrlsInCss = Utils.readBoolean(splits[index], true);
                }
            }
        }
        pair = new DirectivePair(new JSCSSMergeDirective(autoCorrectUrlsInCss), null);
        return pair;
    }


}

class JSCSSMergeDirective implements PreChainDirective {

    boolean autoCorrectUrlsInCss = true;

    ServletContext context;

    public static final Logger LOGGER = LoggerFactory.getLogger(JSCSSMergeDirective.class.getName());

    JSCSSMergeDirective(boolean autoCorrectUrlsInCss) {
        this.autoCorrectUrlsInCss = autoCorrectUrlsInCss;
    }

    @Override
    public int execute(ModuleRequest request, ModuleResponse response, ServletContext context) {
        this.context = context;
        String url = getURL(request);

        LOGGER.trace("Started processing request : {}", url);

        List<String> resourcesToMerge = Utils.findResourcesToMerge(request.getContextPath(), url);

        //If not modified, return 304 and stop
        ResourceStatus status = isNotModified(request, response, resourcesToMerge);
        if (status.isNotModified()) {
            LOGGER.debug("Resources Not Modified. Sending 304.");
            sendNotModified(response);
            return STOP_CHAIN;
        }

        String extensionOrPath = Utils.detectExtension(url);//in case of non js/css files it null
        if (extensionOrPath == null) {
            extensionOrPath = resourcesToMerge.get(0);//non grouped i.e. non css/js file, we refer it's path in that case
        }

        //Add appropriate headers
        addAppropriateResponseHeaders(extensionOrPath, resourcesToMerge, status.getActualETag(), response);
        try {
            OutputStream outputStream = response.getOutputStream();
            int resourcesNotFound = processResources(request.getContextPath(), outputStream, resourcesToMerge, autoCorrectUrlsInCss);

            if (resourcesNotFound > 0 && resourcesNotFound == resourcesToMerge.size()) { //all resources not found
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                LOGGER.warn("All resources are not found. Sending 404.");
                return STOP_CHAIN;
            }

            if (outputStream != null) {
                //try {
                response.setStatus(HttpServletResponse.SC_OK);
                outputStream.close();
                //response.commit();
                //} catch (IOException e) {
                //e.printStackTrace();
                //  LOGGER.error(Utils.buildLoggerMessage("Response commit failed.", e.getMessage()));
                //return IRule.Status.CONTINUE;
                //}
            }
        } catch (IOException ex) {
            //ex.printStackTrace();
            LOGGER.error("Error in processing request.", ex);
            return OK;

        }

        LOGGER.debug("Finished processing Request : {}", url);
        return STOP_CHAIN;
    }

    /**
     * @param response httpServletResponse
     */
    private void sendNotModified(HttpServletResponse response) {
        response.setContentLength(0);
        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
    }

    /**
     * @param request HttpServletRequest
     * @return The URL without fingerprint if it has any
     */
    private String getURL(HttpServletRequest request) {
        return Utils.removeFingerPrint(request.getRequestURI());
    }

    /**
     * @param request - HttpServletRequest
     * @param response - HttpServletResponse
     * @param resourcesToMerge - List of resources relative paths
     * @return true if not modified based on if-None-Match and If-Modified-Since
     */
    private ResourceStatus isNotModified(HttpServletRequest request, HttpServletResponse response, List<String> resourcesToMerge) {
        //If-Modified-Since
        String ifModifiedSince = request.getHeader(HTTP_IF_MODIFIED_SINCE);
        if (ifModifiedSince != null) {
            Date date = Utils.readDateFromHeader(ifModifiedSince);
            if (date != null) {
                if (!Utils.isAnyResourceModifiedSince(resourcesToMerge, date.getTime(), context)) {
                    this.sendNotModified(response);
                    return new ResourceStatus(null, true);
                }
            }
        }
        //If-None-match
        String requestETag = request.getHeader(HTTP_IF_NONE_MATCH_HEADER);
        String actualETag = Utils.buildETagForResources(resourcesToMerge, context);
        if ( !Utils.isAnyResourceETagModified(resourcesToMerge, requestETag, actualETag, context)) {
            return new ResourceStatus(actualETag, true);
        }
        return new ResourceStatus(actualETag, false);
    }

    /**
     * @param contextPath          HttpServletRequest context path
     * @param outputStream         - OutputStream
     * @param resourcesToMerge     list of resources to merge
     * @param autoCorrectUrlsInCSS whether to correct image urls in merged css files response.
     * @return number of non existing, unprocessed resources
     */

    private int processResources(String contextPath, OutputStream outputStream, List<String> resourcesToMerge, boolean autoCorrectUrlsInCSS) {

        int resourcesNotFound = 0;

        for (String resourcePath : resourcesToMerge) {

            LOGGER.trace("Processing resource : {}", resourcePath);

            InputStream is = null;

            try {
                is = context.getResourceAsStream(resourcePath);
                if (is == null) {
                    resourcesNotFound++;
                    continue;
                }
                if (resourcePath.endsWith(EXT_CSS) && autoCorrectUrlsInCSS) { //Need to deal with images url in CSS

                    this.processCSS(contextPath, resourcePath, is, outputStream);

                } else {
                    byte[] buffer = new byte[128];
                    int c;
                    while ((c = is.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, c);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error while reading resource : {}", resourcePath);
                LOGGER.error("IOException :",  e);
            }

            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    LOGGER.warn("Failed to close stream: {}", ex);
                }
                try {
                    outputStream.flush();
                } catch (IOException ex) {
                    LOGGER.error("Failed to flush out:{}",  outputStream);
                }
            }

        }
        return resourcesNotFound;
    }

    /**
     * @param cssFilePath - path of the cssFile
     * @param contextPath - web app context path or custom context path
     * @param inputStream - input stream
     * @param outputStream - output stream
     * @throws java.io.IOException - throws exception in case something woes wrong (IO read/write)
     */
    private void processCSS(String contextPath, String cssFilePath, InputStream inputStream, OutputStream outputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        StringBuffer buffer = new StringBuffer();
        while ((line = bufferedReader.readLine()) != null) {
            buffer.setLength(0);
            buffer.append(line);
            line = this.processCSSLine(context, contextPath, cssFilePath, buffer);
            outputStream.write((line + "\n").getBytes());
        }
    }

    /**
     * @param context - ServletContext
     * @param contextPath - context path or custom configured context path
     * @param cssFilePath - css file path
     * @param line - one single line in a css file
     * @return processed string with appropriate replacement of image URLs if any
     */
    private String processCSSLine(ServletContext context, String contextPath, String cssFilePath, StringBuffer line) {
        Matcher matcher = CSS_IMG_URL_PATTERN.matcher(line);
        String cssRealPath = context.getRealPath(cssFilePath);
        while (matcher.find()) {
            String refImgPath = matcher.group(1);
            if (!Utils.isProtocolURL(refImgPath)) { //ignore absolute protocol paths
                String resolvedImgPath = refImgPath;
                if (!refImgPath.startsWith("/")) {
                    resolvedImgPath = Utils.buildProperPath(Utils.getParentPath(cssFilePath), refImgPath);
                }
                String imgRealPath = context.getRealPath(resolvedImgPath);
                String fingerPrint = Utils.buildETagForResource(resolvedImgPath, context);
                int offset = line.indexOf(refImgPath);
                line.replace(
                        offset, //from
                        offset + refImgPath.length(), //to
                        contextPath + Utils.addFingerPrint(fingerPrint, resolvedImgPath)
                );

                Utils.updateReferenceMap(cssRealPath, imgRealPath);
            }
        }
        return line.toString();
    }

    /**
     * @param extensionOrFile  - .css or .js etc. (lower case) or the absolute path of the file in case of image files
     * @param resourcesToMerge - from request
     * @param hashForETag      - from request
     * @param resp             - response object
     */
    private void addAppropriateResponseHeaders(String extensionOrFile, List<String> resourcesToMerge, String hashForETag, HttpServletResponse resp) {
        String mime = Utils.selectMimeForExtension(extensionOrFile);
        if (mime != null) {
            LOGGER.trace("Setting MIME to ",  mime);
            resp.setContentType(mime);
        }
        if (hashForETag != null) {
            resp.addHeader(HTTP_ETAG_HEADER, hashForETag);
            LOGGER.trace("Added ETag headers");
        }
        resp.addHeader(HEADER_X_OPTIMIZED_BY, X_OPTIMIZED_BY_VALUE);

    }
}

/**
 * Class to store resource ETag and modified status
 */
class ResourceStatus {

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