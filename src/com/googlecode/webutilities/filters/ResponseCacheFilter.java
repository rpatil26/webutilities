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

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.webutilities.common.Constants;
import com.googlecode.webutilities.common.ServletResponseWrapper;
import com.googlecode.webutilities.servlets.JSCSSMergeServlet;

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
 * <h3>Init Parameters</h3>
 *
 * @author rpatil
 * @version 1.0
 */

public class ResponseCacheFilter implements Filter {

    private FilterConfig config;

    private class CacheObject{

        private long time;

        ServletResponseWrapper servletResponseWrapper;

        CacheObject(long time, ServletResponseWrapper servletResponseWrapper){
            this.time = time;
            this.servletResponseWrapper = servletResponseWrapper;
        }

        public long getTime() {
            return time;
        }

        public ServletResponseWrapper getServletResponseWrapper() {
            return servletResponseWrapper;
        }

    }

    private Map<String, CacheObject> cache = Collections.synchronizedMap(new LinkedHashMap<String, CacheObject>());

    private static final Logger logger = Logger.getLogger(ResponseCacheFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.config = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;

        String url = httpServletRequest.getRequestURI();

        boolean expireCache = httpServletRequest.getParameter(Constants.PARAM_EXPIRE_CACHE) != null;

        if(expireCache){
            cache.clear();
        }

        boolean skipCache = httpServletRequest.getParameter(Constants.PARAM_DEBUG) != null || httpServletRequest.getParameter(Constants.PARAM_SKIP_CACHE) != null;

        if(skipCache || expireCache){
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        boolean cacheFound = false;
        CacheObject cacheObject = cache.get(url);
        if(cacheObject != null && cacheObject.getServletResponseWrapper() != null){
            List<String> requestedResources = JSCSSMergeServlet.findResourcesToMerge(httpServletRequest);
            if(requestedResources != null && JSCSSMergeServlet.isAnyResourceModifiedSince(requestedResources, cacheObject.getTime(), config.getServletContext())){
                logger.info("Some resources have been modified since last cache: " + url);
                cache.remove(url);
                cacheFound = false;
            }else{
                logger.info("Found valid cached response.");
                cacheFound = true;
            }
        }

        if(cacheFound){
            logger.info("Returning Cached response.");
            fillResponseFromCache(httpServletResponse, cacheObject.getServletResponseWrapper());
        }else{
            logger.info("Cache not found or invalidated");
            ServletResponseWrapper wrapper = new ServletResponseWrapper(httpServletResponse);
            filterChain.doFilter(servletRequest, wrapper);
            cache.put(url, new CacheObject(new Date().getTime(), wrapper));
            httpServletResponse.setCharacterEncoding(wrapper.getCharacterEncoding());
            httpServletResponse.setContentType(wrapper.getContentType());
            httpServletResponse.getOutputStream().write(wrapper.getBytes());
            httpServletResponse.setStatus(wrapper.getStatus());
        }

    }
    
    private void fillResponseFromCache(HttpServletResponse actual, ServletResponseWrapper cache) throws IOException{
    	for(Cookie cookie : cache.getCookies()){
    		actual.addCookie(cookie);
    	}
    	for(String headerName : cache.getHeaders().keySet()){
    		Object value = cache.getHeaders().get(headerName);
    		if(value instanceof Long){
    			actual.addDateHeader(headerName, ((Long) value));
    		}else if(value instanceof Integer){
    			actual.addIntHeader(headerName, ((Integer) value));
    		}else {
    			actual.addHeader(headerName, value.toString());
    		}
    	}
    	actual.setCharacterEncoding(cache.getCharacterEncoding());
    	actual.setContentType(cache.getContentType());
    	actual.getOutputStream().write(cache.getBytes());
    	actual.setStatus(cache.getStatus());
    }
    
    @Override
    public void destroy() {
       this.config = null;
    }
}





