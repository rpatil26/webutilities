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

package com.googlecode.webutilities.filters;

import static com.googlecode.webutilities.common.Constants.HTTP_IF_MODIFIED_SINCE;
import static com.googlecode.webutilities.common.Constants.HTTP_IF_NONE_MATCH_HEADER;
import static com.googlecode.webutilities.util.Utils.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.webutilities.common.Constants;
import com.googlecode.webutilities.common.WebUtilitiesResponseWrapper;
import com.googlecode.webutilities.filters.common.AbstractFilter;


/**
 * The <code>ResponseCacheFilter</code> is implemented as Servlet Filter to enable caching of STATIC resources (JS, CSS, static HTML files)
 * <p>
 * This enables the server side caching of the static resources, where client caching is done using JSCSSMergeServlet by setting
 * appropriate expires/Cache-Control headers.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * Put the <b>webutilities-x.y.z.jar</b> in your classpath (WEB-INF/lib folder of your webapp).
 * </p>
 * <p>
 * Declare this filter in your <code>web.xml</code> ( web descriptor file)
 * </p>
 * <pre>
 * ...
 * &lt;filter&gt;
 * 	&lt;filter-name&gt;responseCacheFilter&lt;/filter-name&gt;</b>
 * 	&lt;filter-class&gt;<b>com.googlecode.webutilities.filters.ResponseCacheFilter</b>&lt;/filter-class&gt;
 * &lt;/filter&gt;
 * ...
 * </pre>
 * Map this filter on your JS and CSS resources
 * <pre>
 * ...
 * &lt;filter-mapping&gt;
 *   &lt;filter-name&gt;responseCacheFilter&lt;/filter-name&gt;
 *   &lt;url-pattern&gt;<b>*.js</b>&lt;/url-pattern&gt;
 *   &lt;url-pattern&gt;<b>*.json</b>&lt;/url-pattern&gt;
 *   &lt;url-pattern&gt;<b>*.css</b>&lt;/url-pattern&gt;
 * &lt;/filter-mapping>
 * ...
 * </pre>
 * <p>
 * And you are all done!
 * </p>
 *
 * Visit http://code.google.com/p/webutilities/wiki/ResponseCacheFilter for more details.
 *
 * @author rpatil
 * @version 1.0
 */

public class ResponseCacheFilter extends AbstractFilter {

    private class CacheObject{

        private long time;

        //private long accessCount = 0;

        private WebUtilitiesResponseWrapper webUtilitiesResponseWrapper;

        CacheObject(long time, WebUtilitiesResponseWrapper webUtilitiesResponseWrapper){
            this.time = time;
            this.webUtilitiesResponseWrapper = webUtilitiesResponseWrapper;
        }

        public long getTime() {
            return time;
        }

        public WebUtilitiesResponseWrapper getWebUtilitiesResponseWrapper() {
            return webUtilitiesResponseWrapper;
        }

        /*public void increaseAccessCount(){
            accessCount++;
        }

        public long getAccessCount(){
            return this.accessCount;
        }*/

    }

    private Map<String, CacheObject> cache = Collections.synchronizedMap(new LinkedHashMap<String, CacheObject>());

    private int reloadTime = 0;

    private int resetTime = 0;

    private long lastResetTime;

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseCacheFilter.class.getName());

    private static final String INIT_PARAM_RELOAD_TIME = "reloadTime";

    private static final String INIT_PARAM_RESET_TIME = "resetTime";


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);

        this.reloadTime = readInt(filterConfig.getInitParameter(INIT_PARAM_RELOAD_TIME),reloadTime);

        this.resetTime = readInt(filterConfig.getInitParameter(INIT_PARAM_RESET_TIME),resetTime);

        lastResetTime = new Date().getTime();

        LOGGER.debug("Cache Filter initialized with: {}:{},\n{}:{}",
                new Object[]{INIT_PARAM_RELOAD_TIME, String.valueOf(reloadTime),
                INIT_PARAM_RESET_TIME ,String.valueOf(resetTime)});

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;

        String url = httpServletRequest.getRequestURI();

        if(!isURLAccepted(url) || !isUserAgentAccepted(httpServletRequest.getHeader(Constants.HTTP_USER_AGENT_HEADER))){
            LOGGER.debug("Skipping Cache filter for: {}" , url);
            LOGGER.debug("URL or UserAgent not accepted");
            filterChain.doFilter(servletRequest,servletResponse);
            return;
        }


        long now = new Date().getTime();

        CacheObject cacheObject = cache.get(url);

        boolean expireCache = httpServletRequest.getParameter(Constants.PARAM_EXPIRE_CACHE) != null ||
                (cacheObject != null &&  reloadTime > 0 && (now - cacheObject.getTime())/1000 > reloadTime);

        if(expireCache){
            LOGGER.trace("Removing Cache for {}  due to URL parameter.", url);
            cache.remove(url);
        }

        boolean resetCache = httpServletRequest.getParameter(Constants.PARAM_RESET_CACHE) != null ||
                resetTime > 0 && (now - lastResetTime)/1000 > resetTime;

        if(resetCache){
            LOGGER.trace("Resetting whole Cache for {} due to URL parameter.", url);
            cache.clear();
            lastResetTime = now;
        }

        boolean skipCache = httpServletRequest.getParameter(Constants.PARAM_DEBUG) != null || httpServletRequest.getParameter(Constants.PARAM_SKIP_CACHE) != null;

        if(skipCache){
            filterChain.doFilter(servletRequest, servletResponse);
            LOGGER.trace("Skipping Cache for {} due to URL parameter.", url);
            return;
        }
        
        List<String> requestedResources = findResourcesToMerge(httpServletRequest.getContextPath(), url);
        ServletContext context = filterConfig.getServletContext();
        //If-Modified-Since
        String ifModifiedSince = httpServletRequest.getHeader(HTTP_IF_MODIFIED_SINCE);
        if(ifModifiedSince != null){
            Date date = readDateFromHeader(ifModifiedSince);
            if(date != null){
                if(!isAnyResourceModifiedSince(requestedResources, date.getTime(), context)){
                    //cache.remove(url);
                    this.sendNotModified(httpServletResponse);
                    return;
                }
            }
        }
        //If-None-match
        String requestETag = httpServletRequest.getHeader(HTTP_IF_NONE_MATCH_HEADER);
        if(!isAnyResourceETagModified(requestedResources, requestETag, null, context)){
            cache.remove(url);
        	this.sendNotModified(httpServletResponse);
    		return;
        }

        boolean cacheFound = false;

        if(cacheObject != null && cacheObject.getWebUtilitiesResponseWrapper() != null){
            if(requestedResources != null && isAnyResourceModifiedSince(requestedResources, cacheObject.getTime(), context)){
                LOGGER.trace("Some resources have been modified since last cache: {}" , url);
                cache.remove(url);
                cacheFound = false;
            }else{
                LOGGER.trace("Found valid cached response.");
                //cacheObject.increaseAccessCount();
                cacheFound = true;
            }
        }

        if(cacheFound){
            LOGGER.debug("Returning Cached response.");
            cacheObject.getWebUtilitiesResponseWrapper().fill(httpServletResponse);
            //fillResponseFromCache(httpServletResponse, cacheObject.getModuleResponse());
        }else{
            LOGGER.trace("Cache not found or invalidated");
            WebUtilitiesResponseWrapper wrapper = new WebUtilitiesResponseWrapper(httpServletResponse);
            filterChain.doFilter(servletRequest, wrapper);

            if(isMIMEAccepted(wrapper.getContentType()) && !expireCache && !resetCache && wrapper.getStatus() != HttpServletResponse.SC_NOT_MODIFIED){
            	cache.put(url, new CacheObject(getLastModifiedFor(requestedResources, context), wrapper));
	            LOGGER.debug("Cache added for: {}", url);
            }else{
                LOGGER.trace("Cache NOT added for: {}", url);
                LOGGER.trace("is MIME not accepted: {}", isMIMEAccepted(wrapper.getContentType()));
                LOGGER.trace("is expireCache: {}", expireCache);
                LOGGER.trace("is resetCache: {}", resetCache);
            }
            wrapper.fill(httpServletResponse);
        }

    }
    
    private void sendNotModified(HttpServletResponse httpServletResponse){
        httpServletResponse.setContentLength(0);
        httpServletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        LOGGER.trace("returning Not Modified (304)");
    }
}





