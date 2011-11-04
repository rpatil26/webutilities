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

import com.googlecode.webutilities.filters.CompressionFilter;
import com.googlecode.webutilities.servlets.JSCSSMergeServlet;
import com.googlecode.webutilities.test.util.TestUtils;
import com.mockrunner.mock.web.WebMockObjectFactory;
import com.mockrunner.servlet.ServletTestModule;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static com.googlecode.webutilities.common.Constants.HTTP_ACCEPT_ENCODING_HEADER;
import static com.googlecode.webutilities.common.Constants.HTTP_CONTENT_ENCODING_HEADER;
import static com.googlecode.webutilities.common.Constants.HTTP_USER_AGENT_HEADER;
import static com.googlecode.webutilities.common.Constants.HTTP_VARY_HEADER;

public class CompressionFilterTest {

    private JSCSSMergeServlet jscssMergeServlet = new JSCSSMergeServlet();

    private CompressionFilter compressionFilter = new CompressionFilter();

    private WebMockObjectFactory webMockObjectFactory = new WebMockObjectFactory();

    private ServletTestModule servletTestModule = new ServletTestModule(webMockObjectFactory);

    private Properties properties = new Properties();

    private int currentTestNumber = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(CompressionFilterTest.class.getName());

    public CompressionFilterTest() throws Exception {
        properties.load(this.getClass().getResourceAsStream(CompressionFilterTest.class.getSimpleName() + ".properties"));
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
            }
        }
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
        String userAgent = properties.getProperty(this.currentTestNumber + ".test.request.userAgent");
        if (userAgent != null && !userAgent.trim().equals("")) {
            webMockObjectFactory.getMockRequest().addHeader(HTTP_USER_AGENT_HEADER, userAgent);
        }
        String accept = properties.getProperty(this.currentTestNumber + ".test.request.accept");
        if (accept != null && !accept.trim().equals("")) {
            webMockObjectFactory.getMockRequest().addHeader(HTTP_ACCEPT_ENCODING_HEADER, accept);
        }
    }

    private String getExpectedContentEncoding() {

        return properties.getProperty(this.currentTestNumber + ".test.expected");

    }

    private String getExpectedOutput() throws Exception {

        String expectedResource = properties.getProperty(this.currentTestNumber + ".test.expected.output");
        if (expectedResource == null || expectedResource.trim().equals("")) return null;
        return TestUtils.readContents(this.getClass().getResourceAsStream(expectedResource), webMockObjectFactory.getMockResponse().getCharacterEncoding());

    }

    private void pre() throws Exception {


        webMockObjectFactory = new WebMockObjectFactory();

        servletTestModule = new ServletTestModule(webMockObjectFactory);

        this.setUpInitParams();

        servletTestModule.setServlet(jscssMergeServlet, true);

        servletTestModule.addFilter(compressionFilter, true);

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
                break;
                //return; // no more test cases in properties file.
            }

            LOGGER.info("Running Test {}:{}", new Object[]{this.currentTestNumber, testCase});

            LOGGER.debug("##################################################################################################################");
            LOGGER.debug("Running Test {}:{}", this.currentTestNumber, testCase);
            LOGGER.debug("##################################################################################################################");


            servletTestModule.doFilter();

            String actualResponseEncoding = webMockObjectFactory.getMockResponse().getHeader(HTTP_CONTENT_ENCODING_HEADER);

            String actualVary = webMockObjectFactory.getMockResponse().getHeader(HTTP_VARY_HEADER);

            String expectedEncoding = this.getExpectedContentEncoding();

            if (expectedEncoding == null || expectedEncoding.trim().equalsIgnoreCase("null")) {
                Assert.assertNull("Actual Encoding from response should be null", actualResponseEncoding);
            } else {
                Assert.assertNotNull("Actual Encoding expected was " + expectedEncoding + " but found null.", actualResponseEncoding);

                Assert.assertEquals(expectedEncoding.trim(), actualResponseEncoding.trim());
                Assert.assertEquals(actualVary.trim(), HTTP_ACCEPT_ENCODING_HEADER);

                String expected = getExpectedOutput();
                String actual = servletTestModule.getOutput();
                if (expected != null) { //!NEED TO REMOVE AND ADD .output in test cases - to test deflate also
                    if (expectedEncoding.equalsIgnoreCase("gzip")) {
                        Assert.assertTrue("Contents not matching for test: " + currentTestNumber, TestUtils.compressedContentEquals(expected, actual));
                    } else if (!expectedEncoding.equalsIgnoreCase("compress")) {
                        //WE ARE NOT ABLE TO TEST COMPRESS ENCODING AT THIS TIME :(
                        //IT DIFFERS IN HEADER, FILENAME AND FOOTER when compared with one created from zip command
                        Assert.assertEquals(expected, actual);
                    }
                }
            }

            this.post();

        }

    }

    private void post() {
        this.currentTestNumber++;
    }

}
