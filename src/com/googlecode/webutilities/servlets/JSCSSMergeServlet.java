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
package com.googlecode.webutilities.servlets;

import com.googlecode.webutilities.util.Utils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.googlecode.webutilities.common.Constants.*;

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
 * &lt;link rel="StyleSheet" href="<b>/myapp/css/common,calendar,aquaskin.css</b>"/&gt;
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
 * &lt;link rel="StyleSheet" href="/myapp/css/common,calendar,aquaskin.css<b>?_dbg=1</b>"/&gt;
 * or
 * &lt;script language="JavaScript" src="/myapp/js/prototype,controls,dragdrop,myapp.js<b>?_expirecache_=1</b>"&gt;&lt;/script&gt;
 * </pre>
 * <h3>Limitations</h3>
 * <p>
 * The multiple JS or CSS files <b>can be combined together in one request if they are in same parent path</b>. eg. <code><b>/myapp/js/a.js</b></code>, <code><b>/myapp/js/b.js</b></code> and <code><b>/myapp/js/c.js</b></code>
 * can be combined together as <code><b>/myapp/js/a,b,c.js</b></code>. If they are not in common path then they can not be combined in one request. Same applies for CSS too.
 * </p>
 *
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

    private long expiresMinutes = DEFAULT_EXPIRES_MINUTES; //default value 7 days

    private String cacheControl = DEFAULT_CACHE_CONTROL; //default

    private boolean autoCorrectUrlsInCSS = true; //default  

    private static final Logger logger = Logger.getLogger(JSCSSMergeServlet.class.getName());

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.expiresMinutes = Utils.readLong(config.getInitParameter(INIT_PARAM_EXPIRES_MINUTES), this.expiresMinutes);
        this.cacheControl = config.getInitParameter(INIT_PARAM_CACHE_CONTROL) != null ? config.getInitParameter(INIT_PARAM_CACHE_CONTROL) : this.cacheControl ;
        this.autoCorrectUrlsInCSS = Utils.readBoolean(config.getInitParameter(INIT_PARAM_AUTO_CORRECT_URLS_IN_CSS),this.autoCorrectUrlsInCSS);
        logger.info("Servlet initialized: " +
                "{" +
                "   " + INIT_PARAM_EXPIRES_MINUTES + ":" + this.expiresMinutes + "" +
                "   " + INIT_PARAM_CACHE_CONTROL + ":" + this.cacheControl + "" +
                "   " + INIT_PARAM_AUTO_CORRECT_URLS_IN_CSS + ":" + this.autoCorrectUrlsInCSS + "" +
                "}");
    }

    /**
     *
     * @param extension - .css or .js etc. (lower case)
     * @param resp - response object
     */
    private void setResponseMimeAndHeaders(String extension, HttpServletResponse resp) {
        String mime = Utils.selectMimeForExtension(extension);
        if(mime != null){
            logger.info("Setting MIME to " + mime);
            resp.setContentType(mime);
        }
        resp.addDateHeader(HEADER_EXPIRES, new Date().getTime() + expiresMinutes * 60 * 1000);
        resp.addDateHeader(HEADER_LAST_MODIFIED, new Date().getTime());
        resp.addHeader(HTTP_CACHE_CONTROL_HEADER, this.cacheControl);
        logger.info("Added expires and last-modified headers");
    }

    public static boolean isAnyResourceModifiedSince(List<String> resources, long sinceTime, ServletContext servletContext){
        for (String resourcePath : resources) {
            logger.info("Checking for modification : " + resourcePath);
            resourcePath = servletContext.getRealPath(resourcePath);
            if(resourcePath == null) continue;
            File resource =  new File(resourcePath);
            long lastModified = resource.lastModified();
            if(lastModified > sinceTime){
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
      * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
      */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getRequestURI();

        logger.info("doGetCalled : " + url);

        String extension = Utils.detectExtension(url);

        this.setResponseMimeAndHeaders(extension, resp);

        Writer out = resp.getWriter();

        List<String> resourcesToMerge = JSCSSMergeServlet.findResourcesToMerge(req);
        int resourcesNotFound = 0;
        for (String fullPath : resourcesToMerge) {
            logger.info("Processing resource : " + fullPath);
            InputStream is = null;
            try {
                is = super.getServletContext().getResourceAsStream(fullPath);
                if (is != null) {
                    if(fullPath.endsWith(EXT_CSS) && autoCorrectUrlsInCSS){ //Need to deal with images url in CSS
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
                        String line;
                        Pattern  pattern = Pattern.compile("[uU][rR][lL]\\s*\\(\\s*['\"]?([^('|\")]*)['\"]?\\s*\\)");
                        while((line = bufferedReader.readLine()) != null){
                            Matcher matcher = pattern.matcher(line);
                            while(matcher.find()){
                                String relativePath = matcher.group(1);
                                if(relativePath.matches("[^(http|ftp|////)].*")){ //ignore paths starting with these
                                    //!TODO can be improved?
                                    line = line.replaceAll(relativePath, req.getContextPath() + Utils.buildProperPath(new File(fullPath).getParent(), relativePath));
                                }
                            }
                            out.write(line+"\n");
                        }
                    }else{
                        int c;
                        while ((c = is.read()) != -1) {
                            out.write(c);
                        }
                    }
                }else{
                    resourcesNotFound++;
                }
            } catch (Exception e) {
                logger.warning("Error while reading resource : " + fullPath);
                logger.severe("Exception :" + e);
            } finally {
                if (is != null) {
                    is.close();
                    out.flush();
                }
            }
        }
        if(resourcesNotFound > 0 && resourcesNotFound == resourcesToMerge.size()){ //all resources not found
            resp.sendError(404);
            return;
        }
        if (out != null) {
        	try{
        		out.close();
        	}catch (Exception e) {
				// ignore
			}
        }
    }

    /**
     * Split multiple resources with comma eg. if URL is http://server/context/js/a,b,c.js
     * then a.js, b.js and c.js have to be processed and merged together.
     * <p/>
     * b and c can be absolute paths or relative (relative to previous resource) too.
     * <p/>
     * eg.
     * <p/>
     * http://server/context/js/a,/js/libs/b,/js/yui/c.js - absolutes paths for all OR
     * http://server/context/js/a,/js/libs/b,../yui/c.js - relative path used for c.js (relative to b) OR
     * http://server/context/js/a,/js/libs/b,./c.js OR - b & c are in same directory /js/libs
     *
     * @param request HttpServletRequest
     * @return Set of resources to be processed
     */

    public static List<String> findResourcesToMerge(HttpServletRequest request) {

        String contextPath = request.getContextPath();

        String requestURI = request.getRequestURI(); //w/o hostname, starts with context. eg. /context/path/subpath/a,b,/anotherpath/c.js

        String extension = Utils.detectExtension(requestURI);

        logger.info("Detected extension : " + extension);

        requestURI = requestURI.replace(contextPath, "").replace(extension, "");//remove the context path & ext. will become /path/subpath/a,b,/anotherpath/c

        String[] resourcesPath = requestURI.split(",");

        List<String> resources = new ArrayList<String>();

        String currentPath = "/"; //default

        for (String filePath : resourcesPath) {

            String path = Utils.buildProperPath(currentPath, filePath) + extension;
            if (filePath == null) continue;

            currentPath = new File(path).getParent();

            logger.info("Adding path: " + path + "(Path for next relative resource will be : " + currentPath + ")");

            if(!resources.contains(path)){
                resources.add(path);
            }
        }
        logger.info("Found " + resources.size() + " resources to process and merge.");
        return resources;
    }



    public static void main(String[] args) {
        String s = "";
               s += "background-image : url (\"http://a.png\");";
               s += "background-image : url (\"ftp://b.png\");";
                s += "background-image : url (\"//c.png\");";
                s += "background-image : url (\"/d.png\");";
                s += "background-image : url (\"./././.././././e.png\");";
                s += "background-image : url (./../.././../f.png)";
                s += "background-image : url ('g.png')";

        Pattern  pattern = Pattern.compile(":url\\(['\"]?([^('|\")]*)['\"]?\\)");
        s = s.replaceAll(" ","");
        Matcher matcher = pattern.matcher(s);

        while(matcher.find()){
            String path = matcher.group(1);
            if(path.matches("[^(http|ftp|////)].*")){
                s = s.replaceAll(path,Utils.buildProperPath("/root/css/osx", path));
            }

        }
        System.out.println(s);

    }

}
