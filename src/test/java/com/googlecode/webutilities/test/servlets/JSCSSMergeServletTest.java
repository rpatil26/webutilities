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

package com.googlecode.webutilities.test.servlets;

import com.googlecode.webutilities.servlets.JSCSSMergeServlet;
import com.googlecode.webutilities.util.Utils;
import com.mockrunner.mock.web.MockHttpServletResponse;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.googlecode.webutilities.common.Constants.HEADER_EXPIRES;
import static com.googlecode.webutilities.common.Constants.HEADER_LAST_MODIFIED;

public class JSCSSMergeServletTest extends AbstractServletTest {

    private JSCSSMergeServlet jscssMergeServlet = new JSCSSMergeServlet();

    private int expiresMinutes = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(JSCSSMergeServletTest.class.getName());

    private List<Filter> filters = new ArrayList<Filter>();

    private static final int NO_STATUS_CODE = -99999;

    @Override
    protected String getTestPropertiesName() {
        return JSCSSMergeServletTest.class.getSimpleName() + ".properties";
    }

    @Override
    public void setupInitParams() {
        super.setupInitParams();
        String value = webMockObjectFactory.getMockServletConfig().getInitParameter(JSCSSMergeServlet.INIT_PARAM_EXPIRES_MINUTES);
        if (value == null) {
            setupInitParam(JSCSSMergeServlet.INIT_PARAM_EXPIRES_MINUTES, expiresMinutes + ""); //one minute
        }
    }

    public void setupRequest() {
        super.setupRequest();

        boolean removePreviousFilters = Utils.readBoolean(properties.getProperty(this.currentTestNumber + ".test.removePreviousFilters"), true);
        if (removePreviousFilters) {
            filters.clear();
            servletTestModule.setDoChain(false);
        } else {
            for (Filter filter : filters) {
                servletTestModule.addFilter(filter);
                servletTestModule.setDoChain(true);
            }
        }
        String filter = properties.getProperty(this.currentTestNumber + ".test.filter");
        if (filter != null && !filter.trim().equals("")) {
            String[] filtersString = filter.split(",");
            for (String filterClass : filtersString) {
                Class<?> clazz = null;
                try {
                    clazz = Class.forName(filterClass);
                    Filter f = servletTestModule.createFilter(clazz);
                    if (!filters.contains(f)) {
                        filters.add(f);
                        servletTestModule.setDoChain(true);
                    }
                } catch (ClassNotFoundException e) {
                    LOGGER.debug("Error: ", e);
                }
            }
        }

    }

    @Override
    public void prepare() {
        servletTestModule.setServlet(jscssMergeServlet, true);
    }

    public boolean hasCorrectDateHeaders() {

        Date now = new Date();

        Date lastModified = Utils.readDateFromHeader(webMockObjectFactory.getMockResponse().getHeader(HEADER_LAST_MODIFIED));

        Date expires = Utils.readDateFromHeader(webMockObjectFactory.getMockResponse().getHeader(HEADER_EXPIRES));

        if (lastModified == null || expires == null) return false;

        long differenceInMilliseconds = expires.getTime() - now.getTime();

        //!TODO test lastModified value

        return (expiresMinutes - differenceInMilliseconds <= 5 * 1000); //ensure difference between last modified and expires is almost same (tolerate 5 sec)

    }

    @Override
    public void executeCurrentTestLogic() throws Exception {

        servletTestModule.doGet();

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
            Assert.assertTrue(this.hasCorrectDateHeaders());
            String actualOutput = servletTestModule.getOutput();
            //!TODO for now hash is ignored bcoz it will differ based last modification time of the resource
            //!TODO need to consider and test actual generated hash
            actualOutput = actualOutput.replaceAll("_wu_[0-9a-f]{32}\\.", "_wu_<ignore_hash>.");

            Assert.assertNotNull(actualOutput);

            String expectedOutput = this.getExpectedOutput();

            Assert.assertEquals(expectedOutput.trim(), actualOutput.trim());
        }
    }

}
