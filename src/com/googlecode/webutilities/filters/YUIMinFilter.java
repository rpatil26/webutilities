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
import com.googlecode.webutilities.common.ServletResponseWrapper;
import com.googlecode.webutilities.filters.common.AbstractFilter;
import com.googlecode.webutilities.util.Utils;
import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import static com.googlecode.webutilities.common.Constants.*;

/**
 * The <code>YUIMinFilter</code> is implemented as Servlet Filter to enable on the fly minification of JS and CSS resources
 * using YUICompressor.
 * <p>
 * Using the <code>YUIMinFilter</code> the JS and CSS resources can be minified in realtime by adding this filter on them.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * Put the <b>webutilities-x.y.z.jar</b> and <b>yuicompressor-x.y.z.jar</b> (See dependency mentioned below) in your classpath (WEB-INF/lib folder of your webapp).
 * </p>
 * <p>
 * Declare this filter in your <code>web.xml</code> ( web descriptor file)
 * </p>
 * <pre>
 * ...
 * &lt;filter&gt;
 * 	&lt;filter-name&gt;yuiMinFilter&lt;/filter-name&gt;</b>
 * 	&lt;filter-class&gt;<b>com.googlecode.webutilities.filters.YUIMinFilter</b>&lt;/filter-class&gt;
 * 	&lt;!-- All the init params are optional and are equivalent to YUICompressor command line options --&gt;
 * 	&lt;init-param&gt;
 * 		&lt;param-name&gt;lineBreak&lt;/param-name&gt;
 * 		&lt;param-value&gt;8000&lt;/param-value&gt;
 * 	&lt;/init-param&gt;
 * &lt;/filter&gt;
 * ...
 * </pre>
 * Map this filter on your JS and CSS resources
 * <pre>
 * ...
 * &lt;filter-mapping&gt;
 *   &lt;filter-name&gt;yuiMinFilter&lt;/filter-name&gt;
 *   &lt;url-pattern&gt;<b>*.js</b>&lt;/url-pattern&gt;
 *   &lt;url-pattern&gt;<b>*.json</b>&lt;/url-pattern&gt;
 *   &lt;url-pattern&gt;<b>*.css</b>&lt;/url-pattern&gt;
 * &lt;/filter-mapping>
 * ...
 * </pre>
 * <p>
 * And you are all done! All your JS and CSS files should get minified on the fly.
 * </p>
 * <h3>Init Parameters</h3>
 * <p>
 * All the init parameters are optional and explained below.
 * </p>
 * <pre>
 *  <b>lineBreak</b> - equivalent to YUICompressor --line-break. Insert a line break after the specified column number
 *  <b>noMunge</b> - equivalent to YUICompressor --nomunge. Minify only, do not obfuscate. Default false.
 *  <b>preserveSemi</b> - equivalent to YUICompressor --preserve-semi. Preserve all semicolons. Default false.
 *  <b>disableOptimizations</b> - equivalent to YUICompressor --disable-optimizations. Disable all micro optimizations. Default false.
 *  <b>useCache</b> - to cache the earlier minified contents and serve from cache. Default true.
 *  <b>charset</b> - to use specified charset
 * </pre>
 * <h3>Dependency</h3>
 * <p>The <code>YUIMinFilter</code> depends on servlet-api and YUICompressor jar to be in the classpath.</p>
 * <p><b>servlet-api.jar</b> - Must be already present in your webapp classpath</p>
 * <p><b>yuicompressor-x.y.z.jar</b> - Download and put appropriate version of this jar in your classpath (in WEB-INF/lib)</p>
 * <h3>Limitations</h3>
 * <p> As a best practice you should also add appropriate expires header on static resources so that browser caches them and doesn't request them again and again.
 * You can use the <code>JSCSSMergeServlet</code> from <code>webutilities.jar</code> to add expires header on JS and CSS. It also helps combines multiple JS or CSS requests in one HTTP request. See <code>JSCSSMergeServlet</code> for details.
 * </p>
 *
 * Visit http://code.google.com/p/webutilities/wiki/YUIMinFilter for more details.
 *
 * @author rpatil
 * @version 1.0
 */
public class YUIMinFilter extends AbstractFilter {

    private String charset = DEFAULT_CHARSET;

    private static final String INIT_PARAM_LINE_BREAK = "lineBreak";

    private static final String INIT_PARAM_NO_MUNGE = "noMunge";

    private static final String INIT_PARAM_PRESERVE_SEMI = "preserveSemi";

    private static final String INIT_PARAM_DISABLE_OPTIMIZATIONS = "disableOptimizations";

    private static final String INIT_PARAM_CHARSET = "charset";

    private int lineBreak = -1;

    private boolean noMunge = false;

    private boolean preserveSemi = false;

    private boolean disableOptimizations = false;

    private static final String PROCESSED_ATTR = YUIMinFilter.class.getName() + ".MINIFIED";


    private static final Logger logger = Logger.getLogger(YUIMinFilter.class.getName());

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest rq = (HttpServletRequest) req;

        HttpServletResponse rs = (HttpServletResponse) resp;

        String url = rq.getRequestURI(), lowerUrl = url.toLowerCase();

        logger.info("Filtering URI: " + url);

        boolean alreadyProcessed = req.getAttribute(PROCESSED_ATTR) != null;

        if (!alreadyProcessed && isURLAccepted (url) && isUserAgentAccepted(rq.getHeader(Constants.HTTP_USER_AGENT_HEADER)) && (lowerUrl.endsWith(EXT_JS) || lowerUrl.endsWith(EXT_JSON) || lowerUrl.endsWith(EXT_CSS))) {

            req.setAttribute(PROCESSED_ATTR, Boolean.TRUE);

            ServletResponseWrapper wrapper = new ServletResponseWrapper(rs);
            //Let the response be generated

            chain.doFilter(req, wrapper);

            Writer out = resp.getWriter();

            if(!isMIMEAccepted(wrapper.getContentType())){
                out.write(wrapper.getContents());
                out.flush();
                return;
            }

            StringReader sr = new StringReader(new String(wrapper.getBytes(), this.charset));

            //work on generated response
            if (lowerUrl.endsWith(EXT_JS) || lowerUrl.endsWith(EXT_JSON) || (wrapper.getContentType() != null && (wrapper.getContentType().equals(MIME_JS) || wrapper.getContentType().equals(MIME_JSON)))) {
                JavaScriptCompressor compressor = new JavaScriptCompressor(sr, null);
                logger.info("Compressing JS/JSON type");
                compressor.compress(out, this.lineBreak, !this.noMunge, false, this.preserveSemi, this.disableOptimizations);
            } else if (lowerUrl.endsWith(EXT_CSS) || (wrapper.getContentType() != null && (wrapper.getContentType().equals(MIME_CSS)))) {
                CssCompressor compressor = new CssCompressor(sr);
                logger.info("Compressing CSS type");
                compressor.compress(out, this.lineBreak);
            } else {
                logger.info("Not Compressing anything.");
                out.write(wrapper.getContents());
            }

            out.flush();
        } else {
            chain.doFilter(req, resp);
        }
    }

    @Override
    public void init(FilterConfig config) throws ServletException {

        super.init(config);

        this.charset = this.filterConfig.getInitParameter(INIT_PARAM_CHARSET) == null ? this.charset : this.filterConfig.getInitParameter(INIT_PARAM_CHARSET);

        if(!Charset.isSupported(this.charset)){
            logger.info("Charset " + charset + " not supported. Using default: " + DEFAULT_CHARSET);
            this.charset = DEFAULT_CHARSET;
        }

        this.lineBreak = Utils.readInt(filterConfig.getInitParameter(INIT_PARAM_LINE_BREAK), this.lineBreak);

        this.noMunge = Utils.readBoolean(filterConfig.getInitParameter(INIT_PARAM_NO_MUNGE), this.noMunge);

        this.preserveSemi = Utils.readBoolean(filterConfig.getInitParameter(INIT_PARAM_PRESERVE_SEMI), this.preserveSemi);

        this.disableOptimizations = Utils.readBoolean(filterConfig.getInitParameter(INIT_PARAM_DISABLE_OPTIMIZATIONS), this.disableOptimizations);

        logger.info("Filter initialized with: " +
                "{" +
                "   " + INIT_PARAM_LINE_BREAK + ":" + lineBreak + "," +
                "   " + INIT_PARAM_NO_MUNGE + ":" + noMunge + "," +
                "   " + INIT_PARAM_PRESERVE_SEMI + ":" + preserveSemi + "," +
                "   " + INIT_PARAM_DISABLE_OPTIMIZATIONS + ":" + disableOptimizations + "," +
                "   " + INIT_PARAM_CHARSET + ":" + charset + "," +
                "}");

    }

}

