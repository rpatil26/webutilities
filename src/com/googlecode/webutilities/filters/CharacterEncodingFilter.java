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

import com.googlecode.webutilities.common.Constants;
import com.googlecode.webutilities.filters.common.AbstractFilter;
import com.googlecode.webutilities.util.Utils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;


/**
 * The <code>CharacterEncodingFilter</code> can be used to specify a character encoding for requests in Servlet 2.3/2.4.
 * <p>
 * Using the <code>CharacterEncodingFilter</code> you can specify the encoding to be applied on request (if request
 * doesn't have any encoding set already by client). If client has already set the encoding on request but you still want
 * to enforce to use this encoding then you can even force it (using force = true). If you are using Servlet 2.4 and force
 * is true then not just request but the response will also be forced to the specified encoding.
 * <p/>
 * This is very similar to CharacterEncodingFilter in Spring framework.
 * Look at http://static.springsource.org/spring/docs/2.0.x/api/org/springframework/web/filter/CharacterEncodingFilter.html
 * </p>
 * <h3>Usage</h3>
 * <p>
 * Put the <b>webutilities-x.y.z.jar</b> (See dependency mentioned below) in your classpath (WEB-INF/lib folder of your webapp).
 * </p>
 * <p>
 * Declare this filter in your <code>web.xml</code> (web descriptor file).
 * </p>
 * <pre>
 * ...
 * &lt;filter&gt;
 * 	&lt;filter-name&gt;characterEncodingFilter&lt;/filter-name&gt;</b>
 * 	&lt;filter-class&gt;<b>com.googlecode.webutilities.CharacterEncodingFilter</b>&lt;/filter-class&gt;
 * 	&lt;!-- init params  --&gt;
 * 	&lt;init-param&gt;
 * 		&lt;param-name&gt;encoding&lt;/param-name&gt;
 * 		&lt;param-value&gt;UTF-8&lt;/param-value&gt;
 * 	&lt;/init-param&gt;
 * 	&lt;init-param&gt;
 * 		&lt;param-name&gt;force&lt;/param-name&gt;
 * 		&lt;param-value&gt;true&lt;/param-value&gt; &lt;!-- true if you wanted to force encoding  --&gt;
 * 	&lt;/init-param&gt;
 * 	&lt;init-param&gt;
 * 		&lt;param-name&gt;ignoreURLPattern&lt;/param-name&gt;
 * 		&lt;param-value&gt;.*\.(png|jpg)&lt;/param-value&gt; &lt;!-- regular expression to be matched against URL to skip setting encoding on --&gt;
 * 	&lt;/init-param&gt;
 * &lt;/filter&gt;
 * ...
 * </pre>
 * Map this filter on your web requests
 * <pre>
 * ...
 * &lt;filter-mapping&gt;
 *   &lt;filter-name&gt;characterEncodingFilter&lt;/filter-name&gt;
 *   &lt;url-pattern&gt;<b>*</b>&lt;/url-pattern&gt;
 * &lt;/filter-mapping>
 * ...
 * </pre>
 * <p>
 * And you are all done!
 * </p>
 * <h3>Init Parameters</h3>
 * <p>
 * All the init parameters are optional and explained below.
 * </p>
 * <pre>
 *  <b>encoding</b> - name of the encoding that you wanted to set. e.g. UTF-8
 *  <b>force</b> - true or false. If true this encoding will be forced and overwrite already set encoding for both request and response.
 *  And all common the six filtering criteria parameters for better customization.
 *  <b>ignoreURLPattern</b> - regular expression. e.g. <code>.*\.(jpg|png|gif)</code>.
 *  <b>acceptURLPattern</b> - regular expression. e.g. <code>.*\.(css|js|html)</code>.
 *  <b>ignoreMIMEPattern</b> - regular expression. e.g. <code>images/.*|video/.*</code>.
 *  <b>acceptMIMEPattern</b> - regular expression. e.g. <code>text/css|text/html</code>.
 *  <b>ignoreUAPattern</b> - regular expression for user agent. e.g. <code>MSIE|KHTML</code>.
 *  <b>acceptUAPattern</b> - regular expression. e.g. <code>WebKit|Opera</code>.
 * </pre>
 * <p/>
 * Visit http://code.google.com/p/webutilities/wiki/CharacterEncodingFilter for more details.
 *
 * @author rpatil
 * @version 1.0
 */

public class CharacterEncodingFilter extends AbstractFilter {

    private String encoding;

    private Boolean force = false;

    private static final String INIT_PARAM_ENCODING = "encoding";

    private static final String INIT_PARAM_FORCE = "force";

    private static final Logger logger = Logger.getLogger(CharacterEncodingFilter.class.getName());

    public void init(FilterConfig config) throws ServletException {
        super.init(config);

        this.encoding = filterConfig.getInitParameter(INIT_PARAM_ENCODING);
        this.force = Utils.readBoolean(filterConfig.getInitParameter(INIT_PARAM_FORCE), this.force);

        logger.info("Filter initialized with: " +
                "{" +
                INIT_PARAM_ENCODING + ":" + encoding + "," +
                INIT_PARAM_FORCE + ":" + force + "," +
                "}");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String url = req.getRequestURI();
        if (isURLAccepted(url) && isUserAgentAccepted(req.getHeader(Constants.HTTP_USER_AGENT_HEADER)) && (force || request.getCharacterEncoding() == null)) {
            if (encoding != null) {
                request.setCharacterEncoding(encoding);
                logger.info("Applied request encoding : " + encoding);
            }
        }

        chain.doFilter(req, resp);

        if (encoding != null && force && isMIMEAccepted(response.getContentType())) {
            try {
                resp.setCharacterEncoding(encoding);
                logger.info("Applied response encoding : " + encoding);
            } catch (Exception e) {
                logger.severe("Failed to set response encoding : " + encoding);
                //failed to set encoding may be you have Servlet <= 2.3 (which doesn't have response.setCharacterEncoding)
            }
        }

    }

}
