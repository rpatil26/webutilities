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
import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.StringReader;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.googlecode.webutilities.common.Constants.*;

public class YUICompressModule implements IModule {

    private static final String INIT_PARAM_LINE_BREAK = "lineBreak";

    private static final String INIT_PARAM_NO_MUNGE = "noMunge";

    private static final String INIT_PARAM_PRESERVE_SEMI = "preserveSemi";

    private static final String INIT_PARAM_DISABLE_OPTIMIZATIONS = "disableOptimizations";

    private static final String INIT_PARAM_CHARSET = "charset";

    //private static final String PROCESSED_ATTR = YUICompress.class.getName() + ".MINIFIED";

    private static final Logger LOGGER = LoggerFactory.getLogger(YUICompressModule.class.getName());

    @Override
    public DirectivePair parseDirectives(String ruleString) {

        DirectivePair pair = null;
        int index = 0;

        int lineBreak = -1;

        boolean noMunge = false;

        boolean preserveSemi = false;

        boolean disableOptimizations = false;

        String charset = DEFAULT_CHARSET;

        String[] splits = ruleString.split("\\s+");

        if (!splits[index++].equals(YUICompressModule.class.getSimpleName())) return pair;

        while (index < splits.length) {
            if ((splits[index++]).equalsIgnoreCase(INIT_PARAM_LINE_BREAK)) {
                lineBreak = Utils.readInt(splits[index++], lineBreak);
            } else if ((splits[index++]).equalsIgnoreCase(INIT_PARAM_NO_MUNGE)) {
                noMunge = Utils.readBoolean(splits[index++], noMunge);
            } else if ((splits[index++]).equalsIgnoreCase(INIT_PARAM_PRESERVE_SEMI)) {
                preserveSemi = Utils.readBoolean(splits[index++], preserveSemi);
            } else if ((splits[index++]).equalsIgnoreCase(INIT_PARAM_DISABLE_OPTIMIZATIONS)) {
                disableOptimizations = Utils.readBoolean(splits[index++], disableOptimizations);
            } else if ((splits[index++]).equalsIgnoreCase(INIT_PARAM_CHARSET)) {
                charset = Utils.readString(splits[index++], charset);
            }
        }

        pair = new YUICompressRulesPair(new PreMinifyDirective(lineBreak, noMunge, preserveSemi, disableOptimizations, charset), null);
        return pair;
    }


}

class YUICompressRulesPair extends DirectivePair {

    private static final Logger LOGGER = LoggerFactory.getLogger(YUICompressRulesPair.class.getName());

    YUICompressRulesPair(PreChainDirective preChainDirective, PostChainDirective postChainDirective) {
        super(preChainDirective, postChainDirective);
    }

    @Override
    public ModuleResponse getResponse(final HttpServletRequest request, final HttpServletResponse response) {
        return new ModuleResponse(response) {
            @Override
            public byte[] getBytes() {
                PreMinifyDirective rule = (PreMinifyDirective) request.getAttribute("rule");

                byte[] originalBytes = super.getBytes();
                try {
                    StringWriter out = new StringWriter();
                    String data = new String(originalBytes);
                    StringReader sr = new StringReader(data);

                    String lowerUrl = request.getRequestURI().toLowerCase();
                    //work on generated response
                    if (lowerUrl.endsWith(EXT_JS) || lowerUrl.endsWith(EXT_JSON) || (response.getContentType() != null && (response.getContentType().equals(MIME_JS) || response.getContentType().equals(MIME_JSON)))) {
                        JavaScriptCompressor compressor = new JavaScriptCompressor(sr, null);
                        LOGGER.trace("Compressing JS/JSON type");
                        compressor.compress(out, rule.lineBreak, !rule.noMunge, false, rule.preserveSemi, rule.disableOptimizations);
                    } else if (lowerUrl.endsWith(EXT_CSS) || (response.getContentType() != null && (response.getContentType().equals(MIME_CSS)))) {
                        CssCompressor compressor = new CssCompressor(sr);
                        LOGGER.trace("Compressing CSS type");
                        compressor.compress(out, rule.lineBreak);
                    }
                    return out.getBuffer().toString().getBytes();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return originalBytes;
            }

        };
    }
}

class PreMinifyDirective implements PreChainDirective {

    int lineBreak = -1;

    boolean noMunge = false;

    boolean preserveSemi = false;

    boolean disableOptimizations = false;

    String charset = DEFAULT_CHARSET;

    PreMinifyDirective(int lineBreak, boolean noMunge, boolean preserveSemi, boolean disableOptimizations, String charset) {
        this.lineBreak = lineBreak;
        this.noMunge = noMunge;
        this.preserveSemi = preserveSemi;
        this.disableOptimizations = disableOptimizations;
        this.charset = charset;
    }

    @Override
    public int execute(ModuleRequest request, ModuleResponse response, ServletContext context) {
        request.setAttribute("rule", this);
        return OK;
    }
}
