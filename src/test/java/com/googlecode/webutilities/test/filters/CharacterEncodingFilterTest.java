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

package com.googlecode.webutilities.test.filters;

import com.googlecode.webutilities.filters.CharacterEncodingFilter;
import com.googlecode.webutilities.filters.ResponseCacheFilter;
import com.googlecode.webutilities.servlets.JSCSSMergeServlet;
import com.googlecode.webutilities.util.Utils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharacterEncodingFilterTest extends AbstractFilterTest {

    private JSCSSMergeServlet jscssMergeServlet = new JSCSSMergeServlet();

    private CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();

    private ResponseCacheFilter responseCacheFilter = new ResponseCacheFilter();

    private boolean force = false;

    private static final Logger LOGGER = LoggerFactory.getLogger(CharacterEncodingFilterTest.class.getName());

    @Override
    protected String getTestPropertiesName() {
        return CharacterEncodingFilterTest.class.getSimpleName() + ".properties";
    }

    @Override
    protected void setupInitParam(String name, String value) {
        super.setupInitParam(name, value);
        if ("force".equals(name)) {
            force = Utils.readBoolean(value, force);
            LOGGER.debug("Force: {}", force);
        }
    }

    @Override
    public void prepare() {

        servletTestModule.setServlet(jscssMergeServlet, true);

        servletTestModule.addFilter(characterEncodingFilter, true);

        servletTestModule.addFilter(responseCacheFilter, true);

        servletTestModule.setDoChain(true);
    }

    @Override
    public void executeCurrentTestLogic() throws Exception {
        servletTestModule.doFilter();

        String actualRequestEncoding = webMockObjectFactory.getMockRequest().getCharacterEncoding();

        String actualResponseEncoding = webMockObjectFactory.getMockResponse().getCharacterEncoding();

        String expectedEncodings = this.getExpectedEncoding();

        if (expectedEncodings != null) { //request:response -> UTF-8:ISO-8859-1 - requ
            String[] expectedReqEncResEnc = expectedEncodings.split(":");
            if (force) {
                if (expectedReqEncResEnc.length > 1) {
                    Assert.assertEquals(expectedReqEncResEnc[1], actualResponseEncoding);
                } else {
                    Assert.assertEquals(expectedReqEncResEnc[0], actualResponseEncoding);
                }
            }
            Assert.assertEquals(expectedReqEncResEnc[0], actualRequestEncoding);
        }
    }

}
