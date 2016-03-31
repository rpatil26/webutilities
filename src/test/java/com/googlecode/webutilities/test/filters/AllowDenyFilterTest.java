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

import com.googlecode.webutilities.filters.AllowDenyFilter;
import com.googlecode.webutilities.servlets.JSCSSMergeServlet;
import com.mockrunner.mock.web.MockHttpServletResponse;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AllowDenyFilterTest extends AbstractFilterTest {

    private static final int NO_STATUS_CODE = -99999;

    private JSCSSMergeServlet jscssMergeServlet = new JSCSSMergeServlet();

    private static final Logger LOGGER = LoggerFactory.getLogger(AllowDenyFilterTest.class.getName());

    @Override
    protected String getTestPropertiesName() {
        return AllowDenyFilterTest.class.getSimpleName() + ".properties";
    }

    @Override
    public void prepare() {

        servletTestModule.setServlet(jscssMergeServlet, true);
        AllowDenyFilter allowDenyFilter = new AllowDenyFilter();
        servletTestModule.addFilter(allowDenyFilter, true);
        servletTestModule.setDoChain(true);

    }

    @Override
    public void executeCurrentTestLogic() throws Exception {
        servletTestModule.doFilter();

        MockHttpServletResponse response = webMockObjectFactory.getMockResponse();

        int expectedStatusCode = this.getExpectedStatus(NO_STATUS_CODE);
        int actualStatusCode = response.getErrorCode();
        if (expectedStatusCode != NO_STATUS_CODE) {
            Assert.assertEquals(expectedStatusCode, actualStatusCode);
        }

        Map<String, String> expectedHeaders = this.getExpectedHeaders();
        for (String name : expectedHeaders.keySet()) {
            String value = expectedHeaders.get(name);
            Assert.assertEquals(value, response.getHeader(name));
        }

    }

}
