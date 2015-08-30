/*
 * Copyright 2010-2015 Rajendra Patil
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.webutilities.filters;

import com.googlecode.webutilities.common.Constants;
import com.googlecode.webutilities.filters.common.AbstractFilter;
import com.googlecode.webutilities.filters.compression.CompressedHttpServletRequestWrapper;
import com.googlecode.webutilities.filters.compression.CompressedHttpServletResponseWrapper;
import com.googlecode.webutilities.filters.compression.EncodedStreamsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.googlecode.webutilities.common.Constants.*;
import static com.googlecode.webutilities.util.Utils.readInt;
import static com.googlecode.webutilities.util.Utils.readLong;


/**
 * Servlet Filter implementation class CompressionFilter to handle compressed requests
 * and also respond with compressed contents supporting gzip, compress or
 * deflate compression encoding.
 * <p/>
 * Visit http://code.google.com/p/webutilities/wiki/CompressionFilter for more details.
 *
 * @author rpatil
 * @since 0.0.4
 */
public class CompressionFilter extends AbstractFilter {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CompressionFilter.class.getName());

    /**
     * The threshold (number of bytes) to compress
     */
    private int compressionThreshold = DEFAULT_COMPRESSION_SIZE_THRESHOLD;

    /**
     * The default decompression rate
     */
    private long decompressionRate = DEFAULT_DECOMPRESS_BYTES_PER_SECOND;

    /**
     * Default max allowed uncompressed size of compressed request
     */
    private long maxDecompressedRequestSizeInBytes = MAX_DECOMPRESSED_REQUEST_SIZE_IN_BYTES;

    /**
     * To mark the request that it is processed
     */
    private static final String PROCESSED_ATTR = CompressionFilter.class.getName() + ".PROCESSED";

    /**
     * To mark the request that response compressed
     */
    private static final String COMPRESSED_ATTR = CompressionFilter.class.getName() + ".COMPRESSED";

    /**
     * Threshold
     */
    private static final String INIT_PARAM_COMPRESSION_THRESHOLD = "compressionThreshold";

    /**
     * Compressed HTTP request can be decompressed at this rate (max bytes per second)
     */
    private static final String INIT_PARAM_DECOMPRESS_MAX_BYTES_PER_SECOND = "decompressMaxBytesPerSecond";

    private static final String INIT_PARAM_MAX_DECOMPRESSED_REQUEST_SIZE_IN_BYTES = "maxDecompressedRequestSizeInBytes";

    /* (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);

        int compressionMinSize = readInt(filterConfig.getInitParameter(INIT_PARAM_COMPRESSION_THRESHOLD), this.compressionThreshold);
        long decompressMaxBytesPerSecond = readLong(filterConfig.getInitParameter(INIT_PARAM_DECOMPRESS_MAX_BYTES_PER_SECOND),
                this.decompressionRate);

        long maxDecompressedRequestSize = readLong(filterConfig.getInitParameter(INIT_PARAM_MAX_DECOMPRESSED_REQUEST_SIZE_IN_BYTES),
                this.maxDecompressedRequestSizeInBytes);

        if (compressionMinSize > 0) { // priority given to configured value
            this.compressionThreshold = compressionMinSize;
        }
        if (decompressMaxBytesPerSecond > 0) { // priority given to configured value
            this.decompressionRate = decompressMaxBytesPerSecond;
        }
        if (maxDecompressedRequestSize > 0) { // priority given to configured value
            this.maxDecompressedRequestSizeInBytes = maxDecompressedRequestSize;
        }
        LOGGER.trace("Filter initialized with: {}:{},\n{}:{}\n{}:{}", INIT_PARAM_COMPRESSION_THRESHOLD, String.valueOf(this.compressionThreshold),
                INIT_PARAM_DECOMPRESS_MAX_BYTES_PER_SECOND, String.valueOf(this.decompressionRate),
                INIT_PARAM_MAX_DECOMPRESSED_REQUEST_SIZE_IN_BYTES, String.valueOf(this.maxDecompressedRequestSizeInBytes));
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        ServletRequest req = getRequest(request);

        ServletResponse resp = getResponse(request, response);

        request.setAttribute(PROCESSED_ATTR, Boolean.TRUE);

        chain.doFilter(req, resp);

        if (resp instanceof CompressedHttpServletResponseWrapper) {

            CompressedHttpServletResponseWrapper compressedResponseWrapper = (CompressedHttpServletResponseWrapper) resp;

            try {

                compressedResponseWrapper.close();  //so that stream is finished and closed.

            } catch (IOException ex) {

                LOGGER.error("Response was already closed: ", ex.toString());

            }

            if (compressedResponseWrapper.isCompressed()) {

                req.setAttribute(COMPRESSED_ATTR, Boolean.TRUE);

            }

        }

    }

    private ServletRequest getRequest(ServletRequest request) {

        if (!(request instanceof HttpServletRequest)) {
            LOGGER.trace("No Compression: non http request");
            return request;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String contentEncoding = httpRequest.getHeader(HTTP_CONTENT_ENCODING_HEADER);

        if (contentEncoding == null) {
            LOGGER.trace("No Compression: Request content encoding is: {}", contentEncoding);
            return request;
        }

        if (!EncodedStreamsFactory.isRequestContentEncodingSupported(contentEncoding)) {
            LOGGER.trace("No Compression: unsupported request content encoding: {}", contentEncoding);
            return request;
        }

        LOGGER.debug("Decompressing request: content encoding : {}, throttled read rate: {}",
                contentEncoding, this.decompressionRate);

        return new CompressedHttpServletRequestWrapper(
                httpRequest, EncodedStreamsFactory.getFactoryForContentEncoding(contentEncoding),
                this.decompressionRate, this.maxDecompressedRequestSizeInBytes);

    }

    private String getAppropriateContentEncoding(String acceptEncoding) {
        if (acceptEncoding == null) return null;

        String contentEncoding = null;
        if (CONTENT_ENCODING_IDENTITY.equals(acceptEncoding.trim())) {
            return contentEncoding; //no encoding to be applied
        }

        String[] clientAccepts = acceptEncoding.trim().split("\\s*,\\s*");

        //!TODO select best encoding (based on q) when multiple encoding are accepted by client
        //@see http://stackoverflow.com/questions/3225136/http-what-is-the-preferred-accept-encoding-for-gzip-deflate
        for (String accepts : clientAccepts) {
            if (CONTENT_ENCODING_IDENTITY.equals(accepts)) {
                return contentEncoding;
            } else if (EncodedStreamsFactory.SUPPORTED_ENCODINGS.containsKey(accepts)) {
                contentEncoding = accepts; //get first matching encoding
                break;
            }
        }
        return contentEncoding;
    }

    private ServletResponse getResponse(ServletRequest request, ServletResponse response) {
        if (response.isCommitted() || request.getAttribute(PROCESSED_ATTR) != null) {
            LOGGER.trace("No Compression: Response committed or filter has already been applied");
            return response;
        }

        if (!(response instanceof HttpServletResponse) || !(request instanceof HttpServletRequest)) {
            LOGGER.trace("No Compression: non http request/response");
            return response;
        }

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String acceptEncoding = httpRequest.getHeader(HTTP_ACCEPT_ENCODING_HEADER);

        String contentEncoding = getAppropriateContentEncoding(acceptEncoding);

        if (contentEncoding == null) {
            LOGGER.trace("No Compression: Accept encoding is : {}", acceptEncoding);
            return response;
        }

        String requestURI = httpRequest.getRequestURI();
        if (!isURLAccepted(requestURI)) {
            LOGGER.trace("No Compression: For path: ", requestURI);
            return response;
        }
        if (!isQueryStringAccepted(httpRequest.getQueryString())) {
            LOGGER.trace("No Compression: For Query String: ", httpRequest.getQueryString());
            return response;
        }
        String userAgent = httpRequest.getHeader(Constants.HTTP_USER_AGENT_HEADER);
        if (!isUserAgentAccepted(userAgent)) {
            LOGGER.trace("No Compression: For User-Agent: {}", userAgent);
            return response;
        }

        EncodedStreamsFactory encodedStreamsFactory = EncodedStreamsFactory.getFactoryForContentEncoding(contentEncoding);

        LOGGER.debug("Compressing response: content encoding : {}", contentEncoding);

        return new CompressedHttpServletResponseWrapper(httpResponse, encodedStreamsFactory, contentEncoding, compressionThreshold, this);
    }

}
