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

import com.googlecode.webutilities.common.Constants;
import com.googlecode.webutilities.filters.compression.CompressedHttpServletRequestWrapper;
import com.googlecode.webutilities.filters.compression.CompressedHttpServletResponseWrapper;
import com.googlecode.webutilities.filters.compression.EncodedStreamsFactory;
import com.googlecode.webutilities.modules.infra.ModuleRequest;
import com.googlecode.webutilities.modules.infra.ModuleResponse;
import com.googlecode.webutilities.util.Utils;

import javax.print.attribute.standard.Compression;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.googlecode.webutilities.common.Constants.*;

public class CompressionModule implements IModule {


    @Override
    public DirectivePair parseDirectives(String ruleString) {
        DirectivePair pair = null;
        int index = 0;
        String[] tokens = ruleString.split("\\s+");

        assert tokens.length >= 1;

        if (!tokens[index++].equals(Compression.class.getSimpleName())) return pair;

        int threshold = Constants.DEFAULT_COMPRESSION_SIZE_THRESHOLD;

        if (tokens.length > 1) {
            if ("threshold".equals(tokens[index++])) {
                threshold = Utils.readInt(tokens[index], threshold);
            }
        }
        pair = new CompressionRulePair(new StartCompressionRule(threshold), new FinishCompressionRule());

        return pair;
    }


}

class StartCompressionRule implements PreChainDirective {

    int threshold;

    StartCompressionRule(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public int execute(ModuleRequest request, ModuleResponse response, ServletContext context) {
        //!TODO
//        CompressedHttpServletResponseWrapper responseWrapper = (CompressedHttpServletResponseWrapper) response;
//        responseWrapper.setThreshold(threshold);
        return IDirective.OK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StartCompressionRule that = (StartCompressionRule) o;

        return that.threshold == threshold;

    }

    @Override
    public int hashCode() {
        return threshold;
    }
}

class FinishCompressionRule implements PostChainDirective {

    private static final String COMPRESSED_ATTR = Compression.class.getName() + ".COMPRESSED";

    @Override
    public int execute(ModuleRequest request, ModuleResponse response, ServletContext context) {
        if (Boolean.TRUE.equals(request.getAttribute(COMPRESSED_ATTR))) {
            return IDirective.OK;

        }
        //!TODO
//        if (response instanceof CompressedHttpServletResponseWrapper) {
//
//            CompressedHttpServletResponseWrapper compressedResponseWrapper = (CompressedHttpServletResponseWrapper) response;
//
//            try {
//
//                compressedResponseWrapper.close();  //so that stream is finished and closed.
//
//            } catch (IOException ex) {
//
//                //LOGGER.error("Response was already closed: " + ex);
//
//            }
//
//            if (compressedResponseWrapper.isCompressed()) {
//
//                request.setAttribute(COMPRESSED_ATTR, Boolean.TRUE);
//
//            }
//
//        }
        return IDirective.OK;
    }

}

class CompressionRulePair extends DirectivePair {

    private static final String PROCESSED_ATTR = Compression.class.getName() + ".PROCESSED";

    public CompressionRulePair(PreChainDirective preChainDirective, PostChainDirective postChainDirective) {
        super(preChainDirective, postChainDirective);
    }

    private String getAppropriateContentEncoding(String acceptEncoding) {
        if (acceptEncoding == null) return null;

        String contentEncoding = null;
        if (CONTENT_ENCODING_IDENTITY.equals(acceptEncoding.trim())) {
            return contentEncoding; //no encoding to be applied
        }

        String[] clientAccepts = acceptEncoding.split(",");

        //!TODO select best encoding (based on q) when multiple encoding are accepted by client
        //@see http://stackoverflow.com/questions/3225136/http-what-is-the-preferred-accept-encoding-for-gzip-deflate
        for (String accepts : clientAccepts) {
            if (CONTENT_ENCODING_IDENTITY.equals(accepts.trim())) {
                return contentEncoding;
            } else if (EncodedStreamsFactory.SUPPORTED_ENCODINGS.containsKey(accepts.trim())) {
                contentEncoding = accepts; //get first matching encoding
                break;
            }
        }
        return contentEncoding;
    }

    @Override
    public ModuleResponse getResponse(HttpServletRequest request, HttpServletResponse response) {

        if (response.isCommitted() /*|| request.getAttribute(PROCESSED_ATTR) != null*/) {
            //LOGGER.finest("No Compression: Response committed or filter has already been applied");
            return new ModuleResponse(response);
        }

        String acceptEncoding = request.getHeader(HTTP_ACCEPT_ENCODING_HEADER);

        String contentEncoding = getAppropriateContentEncoding(acceptEncoding);

        if (contentEncoding == null) {
            //LOGGER.finest("No Compression: Accept encoding is : " + acceptEncoding);
            return new ModuleResponse(response);
        }

        EncodedStreamsFactory encodedStreamsFactory = EncodedStreamsFactory.getFactoryForContentEncoding(contentEncoding);

        //LOGGER.fine("Compressing response: content encoding : " + contentEncoding);

        ////!TODO return new CompressedHttpServletResponseWrapper(response, encodedStreamsFactory, contentEncoding, 0, null);
        return new ModuleResponse(response);
    }

    @Override
    public ModuleRequest getRequest(HttpServletRequest request) {

        request.setAttribute(PROCESSED_ATTR, Boolean.TRUE);

        String contentEncoding = request.getHeader(HTTP_CONTENT_ENCODING_HEADER);

        if (contentEncoding == null) {
            //LOGGER.finest("No Compression: Request content encoding is: " + contentEncoding);
            return new ModuleRequest(request);
        }

        if (!EncodedStreamsFactory.isRequestContentEncodingSupported(contentEncoding)) {
            //LOGGER.finest("No Compression: unsupported request content encoding: " + contentEncoding);
            return new ModuleRequest(request);
        }

        //LOGGER.fine("Decompressing request: content encoding : " + contentEncoding);

        //!TODO return new CompressedHttpServletRequestWrapper(request, EncodedStreamsFactory.getFactoryForContentEncoding(contentEncoding));
        return new ModuleRequest(request);
    }
}
