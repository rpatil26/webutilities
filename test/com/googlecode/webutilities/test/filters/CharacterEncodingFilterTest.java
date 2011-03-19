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

import java.util.Properties;
import java.util.logging.Logger;

import junit.framework.TestCase;

import com.googlecode.webutilities.filters.CharacterEncodingFilter;
import com.googlecode.webutilities.servlets.JSCSSMergeServlet;
import com.googlecode.webutilities.util.Utils;
import com.mockrunner.mock.web.WebMockObjectFactory;
import com.mockrunner.servlet.ServletTestModule;

public class CharacterEncodingFilterTest extends TestCase {

    private JSCSSMergeServlet jscssMergeServlet = new JSCSSMergeServlet();

    private CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();

    private WebMockObjectFactory webMockObjectFactory = new WebMockObjectFactory();

    private ServletTestModule servletTestModule = new ServletTestModule(webMockObjectFactory);

    private Properties properties = new Properties();

    private int currentTestNumber = 1;

    private boolean force = false;

    private static final Logger logger = Logger.getLogger(CharacterEncodingFilterTest.class.getName());

    public CharacterEncodingFilterTest() throws Exception {
        properties.load(this.getClass().getResourceAsStream(CharacterEncodingFilterTest.class.getSimpleName() + ".properties"));
    }

    private void setUpInitParams() {
        String value = properties.getProperty(this.currentTestNumber + ".test.init.params");
        if (value != null && !value.trim().equals("")) {
            String[] params = value.split(",");
            for (String param : params) {
                String[] keyAndValue = param.split(":");
                webMockObjectFactory.getMockFilterConfig().setInitParameter(keyAndValue[0], keyAndValue[1]);
                if ("force".equals(keyAndValue[0])) {
                    force = Utils.readBoolean(keyAndValue[1], force);
                }
            }
        }

    }

    private void setUpResources() {
        String resourcesString = properties.getProperty(this.currentTestNumber + ".test.resources");
        if (resourcesString != null && !resourcesString.trim().equals("")) {
            String[] resources = resourcesString.split(",");
            for (String resource : resources) {
                logger.info("Setting resource : " + resource);
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
    }

    private String getExpectedEncoding() {

        return properties.getProperty(this.currentTestNumber + ".test.expected");

    }

    private void pre() throws Exception {


        webMockObjectFactory = new WebMockObjectFactory();

        servletTestModule = new ServletTestModule(webMockObjectFactory);

        this.setUpInitParams();

        servletTestModule.setServlet(jscssMergeServlet, true);

        servletTestModule.addFilter(characterEncodingFilter, true);

        servletTestModule.setDoChain(true);

        this.setUpResources();

        this.setUpRequest();


    }

    public void testFilterUsingDifferentScenarios() throws Exception {

        while (true) {
            this.pre();

            String testCase = properties.getProperty(this.currentTestNumber + ".test.name");

            if (testCase == null || testCase.trim().equals("")) {
                break;
                //return; // no more test cases in properties file.
            }

            logger.info("Running Test (" + this.currentTestNumber + "): " + testCase + "");

            System.out.println("##################################################################################################################");
            System.out.println("Running Test (" + this.currentTestNumber + "): " + testCase + "");
            System.out.println("##################################################################################################################");

            servletTestModule.doFilter();

            String actualRequestEncoding = webMockObjectFactory.getMockRequest().getCharacterEncoding();

            String actualResponseEncoding = webMockObjectFactory.getMockResponse().getCharacterEncoding();

            String expectedEncoding = this.getExpectedEncoding();

            if (force) {
                assertEquals(expectedEncoding.trim(), actualResponseEncoding.trim());
            }

            assertEquals(expectedEncoding, actualRequestEncoding);

            this.post();

        }

    }


    private void post() {
        this.currentTestNumber++;
    }

}
