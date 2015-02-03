/*
 * Copyright 2010-2014 Rajendra Patil
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.webutilities.test.filters;

import com.googlecode.webutilities.filters.ResponseCacheFilter;
import com.googlecode.webutilities.servlets.JSCSSMergeServlet;
import com.mockrunner.mock.web.MockHttpServletResponse;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class RedisResponseCacheFilterTest extends AbstractFilterTest {

    private JSCSSMergeServlet jscssMergeServlet = new JSCSSMergeServlet();

    private ResponseCacheFilter responseCacheFilter = new ExtendedMockResponseCacheFilter();

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisResponseCacheFilterTest.class.getName());

    private static final int NO_STATUS_CODE = -99999;

    @Override
    protected String getTestPropertiesName() {
        return RedisResponseCacheFilterTest.class.getSimpleName() + ".properties";
    }

    @Override
    public void prepare() {

        servletTestModule.setServlet(jscssMergeServlet, true);

        servletTestModule.addFilter(responseCacheFilter, true);

        servletTestModule.setDoChain(true);
    }

    public Map<String, String> getExpectedHeaders() throws Exception {
        Map<String, String> expectedHeaders = super.getExpectedHeaders();
        if (expectedHeaders.isEmpty()) {
            expectedHeaders.put(this.currentTestNumber + ".test.expected.headers", "X-ResponseCacheFilter=ADDED");
        }
        return expectedHeaders;
    }

    public void executeCurrentTestLogic() throws Exception {

        servletTestModule.doFilter();
        MockHttpServletResponse response = webMockObjectFactory.getMockResponse();

        int expectedStatusCode = this.getExpectedStatus(NO_STATUS_CODE);
        int actualStatusCode = response.getStatusCode();
        if (expectedStatusCode != NO_STATUS_CODE) {
            Assert.assertEquals(expectedStatusCode, actualStatusCode);
        }
        Map<String, String> expectedHeaders = this.getExpectedHeaders();
        for (String name : expectedHeaders.keySet()) {
            String value = expectedHeaders.get(name);
            Assert.assertEquals(value, response.getHeader(name));
        }

        if (actualStatusCode != HttpServletResponse.SC_NOT_MODIFIED) {

            /*ResponseCacheFilter.CacheState actualCacheState = ResponseCacheFilter.CacheState.valueOf(
                    webMockObjectFactory.getMockResponse().getHeader(ResponseCacheFilter.CACHE_HEADER));

            String expectedState = getExpectedProperty("cacheState");

            ResponseCacheFilter.CacheState expectedCacheState = expectedState != null
                    ? ResponseCacheFilter.CacheState.valueOf(expectedState) : ResponseCacheFilter.CacheState.ADDED;

            Assert.assertEquals(expectedCacheState, actualCacheState);                                             */

            String actualOutput = servletTestModule.getOutput();

            String expectedOutput = this.getExpectedOutput();

            Assert.assertEquals(expectedOutput, actualOutput);

            Assert.assertNotNull(actualOutput);

            Assert.assertEquals(expectedOutput.trim(), actualOutput.trim());
        }
    }

}
