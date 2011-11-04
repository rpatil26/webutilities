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

package com.googlecode.webutilities.test.filters;

import com.googlecode.webutilities.filters.ResponseCacheFilter;
import com.googlecode.webutilities.servlets.JSCSSMergeServlet;
import com.googlecode.webutilities.test.util.TestUtils;
import com.googlecode.webutilities.util.Utils;
import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.mock.web.WebMockObjectFactory;
import com.mockrunner.servlet.ServletTestModule;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ResponseCacheFilterTest {

    private JSCSSMergeServlet jscssMergeServlet = new JSCSSMergeServlet();

    private ResponseCacheFilter responseCacheFilter = new ResponseCacheFilter();

    private WebMockObjectFactory webMockObjectFactory = new WebMockObjectFactory();

    private ServletTestModule servletTestModule = new ServletTestModule(webMockObjectFactory);

    private Properties properties = new Properties();

    private int currentTestNumber = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseCacheFilterTest.class.getName());

    private static final int NO_STATUS_CODE = -99999;

    public ResponseCacheFilterTest() throws Exception {
        properties.load(this.getClass().getResourceAsStream(ResponseCacheFilterTest.class.getSimpleName() + ".properties"));
    }

    private void setUpInitParams() {
        String value = properties.getProperty(this.currentTestNumber + ".test.init.params");
        if (value != null && !value.trim().equals("")) {
            String[] params = value.split(",");
            for (String param : params) {
                String[] keyAndValue = param.split(":");
                webMockObjectFactory.getMockFilterConfig().setInitParameter(keyAndValue[0], keyAndValue[1]);
            }
        }

    }

    private void setUpResources() {
        String resourcesString = properties.getProperty(this.currentTestNumber + ".test.resources");
        if (resourcesString != null && !resourcesString.trim().equals("")) {
            String[] resources = resourcesString.split(",");
            for (String resource : resources) {
                LOGGER.info("Setting resource : {}", resource);
                webMockObjectFactory.getMockServletContext().setResourceAsStream(resource, this.getClass().getResourceAsStream(resource));
                webMockObjectFactory.getMockServletContext().setRealPath(resource, this.getClass().getResource(resource).getPath());
            }
        }
    }

    private int getExpectedStatus() throws Exception {
        return Utils.readInt(properties.getProperty(this.currentTestNumber + ".test.expected.status"), NO_STATUS_CODE);
    }

    private Map<String, String> getExpectedHeaders() throws Exception {
        Map<String, String> headersMap = new HashMap<String, String>();
        String expectedHeaders = properties.getProperty(this.currentTestNumber + ".test.expected.headers");
        if (expectedHeaders == null || expectedHeaders.trim().equals("")) return headersMap;

        String[] headersString = expectedHeaders.split(",");
        for (String header : headersString) {
            String[] nameValue = header.split("=");
            if (nameValue.length == 2 && nameValue[1].contains("hashOf")) {
                String res = nameValue[1].replaceAll(".*hashOf\\s*\\((.*)\\)", "$1");
                nameValue[1] = Utils.buildETagForResource(res, webMockObjectFactory.getMockServletContext());
            } else if (nameValue.length == 2 && nameValue[1].contains("lastModifiedOf")) {
                String res = nameValue[1].replaceAll(".*lastModifiedOf\\s*\\((.*)\\)", "$1");
                nameValue[1] = Utils.forHeaderDate(new File(webMockObjectFactory.getMockServletContext().getRealPath(res)).lastModified());
            }
            headersMap.put(nameValue[0], nameValue.length == 2 ? nameValue[1] : null);
        }
        return headersMap;
    }

    private void setUpRequest() {
        String requestURI = properties.getProperty(this.currentTestNumber + ".test.request.uri");
        String contextPath = properties.getProperty(this.currentTestNumber + ".test.request.contextPath");
        webMockObjectFactory.getMockRequest().setContextPath(contextPath);
        if (requestURI != null && !requestURI.trim().equals("")) {
            String[] uriAndQuery = requestURI.split("\\?");
            webMockObjectFactory.getMockRequest().setRequestURI(uriAndQuery[0]);
            if (uriAndQuery.length > 1) {
                String[] params = uriAndQuery[1].split("&");
                for (String param : params) {
                    String[] nameValue = param.split("=");
                    webMockObjectFactory.getMockRequest().setupAddParameter(nameValue[0], nameValue[1]);
                }

            }
        }
        String headers = properties.getProperty(this.currentTestNumber + ".test.request.headers");
        if (headers != null && !headers.trim().equals("")) {
            String[] headersString = headers.split("&");
            for (String header : headersString) {
                String[] nameValue = header.split("=");
                if (nameValue.length == 2 && nameValue[1].contains("hashOf")) {
                    String res = nameValue[1].replaceAll(".*hashOf\\s*\\((.*)\\).*", "$1");
                    nameValue[1] = Utils.buildETagForResource(res, webMockObjectFactory.getMockServletContext());
                } else if (nameValue.length == 2 && nameValue[1].contains("lastModifiedOf")) {
                    String res = nameValue[1].replaceAll(".*lastModifiedOf\\s*\\((.*)\\)", "$1");
                    nameValue[1] = Utils.forHeaderDate(new File(webMockObjectFactory.getMockServletContext().getRealPath(res)).lastModified());
                }
                webMockObjectFactory.getMockRequest().addHeader(nameValue[0], nameValue[1]);
            }
        }
    }

    private String getExpectedOutput() throws Exception {

        String expectedResource = properties.getProperty(this.currentTestNumber + ".test.expected");
        if (expectedResource == null || expectedResource.trim().equals("")) return null;
        return TestUtils.readContents(this.getClass().getResourceAsStream(expectedResource), webMockObjectFactory.getMockResponse().getCharacterEncoding());

    }

    private void pre() throws Exception {


        webMockObjectFactory = new WebMockObjectFactory();

        servletTestModule = new ServletTestModule(webMockObjectFactory);

        this.setUpInitParams();

        servletTestModule.setServlet(jscssMergeServlet, true);

        servletTestModule.addFilter(responseCacheFilter, true);

        servletTestModule.setDoChain(true);

        this.setUpResources();

        this.setUpRequest();


    }

    @Test
    public void testFilterUsingDifferentScenarios() throws Exception {

        while (true) {
            this.pre();

            String testCase = properties.getProperty(this.currentTestNumber + ".test.name");

            if (testCase == null || testCase.trim().equals("")) {
                return; // no more test cases in properties file.
            }

            LOGGER.info("Running Test {}: {}", new Object[]{this.currentTestNumber, testCase});

            LOGGER.debug("##################################################################################################################");
            LOGGER.debug("Running Test {}:{}", this.currentTestNumber, testCase);
            LOGGER.debug("##################################################################################################################");


            servletTestModule.doFilter();
            MockHttpServletResponse response = webMockObjectFactory.getMockResponse();

            int expectedStatusCode = this.getExpectedStatus();
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

                String actualOutput = servletTestModule.getOutput();

                Assert.assertNotNull(actualOutput);

                String expectedOutput = this.getExpectedOutput();

                Assert.assertEquals(expectedOutput.trim(), actualOutput.trim());
            }

            this.post();

        }

    }


    private void post() {
        this.currentTestNumber++;
    }

}
