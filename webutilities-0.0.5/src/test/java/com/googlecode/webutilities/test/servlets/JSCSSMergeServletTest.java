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

package com.googlecode.webutilities.test.servlets;

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

import javax.servlet.Filter;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.googlecode.webutilities.common.Constants.HEADER_EXPIRES;
import static com.googlecode.webutilities.common.Constants.HEADER_LAST_MODIFIED;

public class JSCSSMergeServletTest {

    private JSCSSMergeServlet jscssMergeServlet = new JSCSSMergeServlet();

    private WebMockObjectFactory webMockObjectFactory = new WebMockObjectFactory();

    private ServletTestModule servletTestModule = new ServletTestModule(webMockObjectFactory);

    private Properties properties = new Properties();

    private int expiresMinutes = 2;

    private int currentTestNumber = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(JSCSSMergeServletTest.class.getName());

    private List<Filter> filters = new ArrayList<Filter>();

    private static final int NO_STATUS_CODE = -99999;

    public JSCSSMergeServletTest() throws Exception {
        properties.load(this.getClass().getResourceAsStream(JSCSSMergeServletTest.class.getSimpleName() + ".properties"));
    }

    private void setUpInitParams() {
        String value = properties.getProperty(this.currentTestNumber + ".test.init.params");
        if (value != null && !value.trim().equals("")) {
            String[] params = value.split(",");
            for (String param : params) {
                String[] keyAndValue = param.split(":");
                webMockObjectFactory.getMockServletConfig().setInitParameter(keyAndValue[0], keyAndValue[1]);
                if (JSCSSMergeServlet.INIT_PARAM_EXPIRES_MINUTES.equals(keyAndValue[0])) {
                    expiresMinutes = Utils.readInt(keyAndValue[1], expiresMinutes);
                }
            }
        } else { //default
            webMockObjectFactory.getMockServletConfig().setInitParameter(JSCSSMergeServlet.INIT_PARAM_EXPIRES_MINUTES, expiresMinutes + ""); //one minute
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

    private void setUpRequest() throws Exception {
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
            for(String header: headersString){
                String[] nameValue = header.split("=");
                if(nameValue.length == 2 && nameValue[1].contains("hashOf")){
                    String res = nameValue[1].replaceAll(".*hashOf\\s*\\((.*)\\).*","$1");
                    nameValue[1] = Utils.buildETagForResource(res, webMockObjectFactory.getMockServletContext());
                }else if(nameValue.length == 2 && nameValue[1].contains("lastModifiedOf")){
                    String res = nameValue[1].replaceAll(".*lastModifiedOf\\s*\\((.*)\\)","$1");
                    nameValue[1] = Utils.forHeaderDate(new File(webMockObjectFactory.getMockServletContext().getRealPath(res)).lastModified());
                }
                webMockObjectFactory.getMockRequest().addHeader(nameValue[0], nameValue[1]);
            }
        }
        boolean removePreviousFilters = Utils.readBoolean(properties.getProperty(this.currentTestNumber + ".test.removePreviousFilters"), true);
        if(removePreviousFilters){
            filters.clear();
            servletTestModule.setDoChain(false);
        }else{
            for(Filter filter: filters){
                servletTestModule.addFilter(filter);
                servletTestModule.setDoChain(true);
            }
        }

        String filter = properties.getProperty(this.currentTestNumber + ".test.filter");
        if (filter != null && !filter.trim().equals("")) {
            String[] filtersString = filter.split(",");
            for(String filterClass: filtersString){
                Class<?> clazz = Class.forName(filterClass);
                Filter f = servletTestModule.createFilter(clazz);
                if(!filters.contains(f)){
                    filters.add(f);
                    servletTestModule.setDoChain(true);
                }
            }
        }

    }

    private String getExpectedOutput() throws Exception {

        String expectedResource = properties.getProperty(this.currentTestNumber + ".test.expected");
        if (expectedResource == null || expectedResource.trim().equals("")) return null;
        return TestUtils.readContents(this.getClass().getResourceAsStream(expectedResource),webMockObjectFactory.getMockResponse().getCharacterEncoding());

    }

    private int getExpectedStatus() throws Exception {
        return Utils.readInt(properties.getProperty(this.currentTestNumber + ".test.expected.status"), NO_STATUS_CODE);
    }
    private Map<String,String> getExpectedHeaders() throws Exception {
        Map<String,String> headersMap = new HashMap<String,String>();
        String expectedHeaders = properties.getProperty(this.currentTestNumber + ".test.expected.headers");
        if (expectedHeaders == null || expectedHeaders.trim().equals("")) return headersMap;

        String[] headersString = expectedHeaders.split(",");
        for(String header: headersString){
            String[] nameValue = header.split("=");
            if(nameValue.length == 2 && nameValue[1].contains("hashOf")){
                String res = nameValue[1].replaceAll(".*hashOf\\s*\\((.*)\\)","$1");
                nameValue[1] = Utils.buildETagForResource(res, webMockObjectFactory.getMockServletContext());
            }else if(nameValue.length == 2 && nameValue[1].contains("lastModifiedOf")){
                String res = nameValue[1].replaceAll(".*lastModifiedOf\\s*\\((.*)\\)","$1");
                nameValue[1] = Utils.forHeaderDate(new File(webMockObjectFactory.getMockServletContext().getRealPath(res)).lastModified());
            }
            headersMap.put(nameValue[0], nameValue.length == 2 ? nameValue[1] : null);
        }
        return headersMap;
    }
    private void pre() throws java.lang.Exception {


        webMockObjectFactory = new WebMockObjectFactory();

        servletTestModule = new ServletTestModule(webMockObjectFactory);

        this.setUpInitParams();

        servletTestModule.setServlet(jscssMergeServlet, true);

        this.setUpResources();

        this.setUpRequest();


    }

    public boolean hasCorrectDateHeaders() {

        Date now = new Date();

        Date lastModified = Utils.readDateFromHeader(webMockObjectFactory.getMockResponse().getHeader(HEADER_LAST_MODIFIED));

        Date expires = Utils.readDateFromHeader(webMockObjectFactory.getMockResponse().getHeader(HEADER_EXPIRES));

        if (lastModified == null || expires == null) return false;

        long differenceInMilliseconds = expires.getTime() - now.getTime();

        //!TODO test lastModified value

        return (expiresMinutes - differenceInMilliseconds <= 5*1000); //ensure difference between last modified and expires is almost same (tolerate 5 sec)

    }

    @Test
    public void testServletUsingDifferentScenarios() throws Exception {

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

            servletTestModule.doGet();

            MockHttpServletResponse response = webMockObjectFactory.getMockResponse();


            int expectedStatusCode = this.getExpectedStatus();
            int actualStatusCode = response.getStatusCode();
            if(expectedStatusCode != NO_STATUS_CODE){
                Assert.assertEquals(expectedStatusCode, actualStatusCode);
            }
            Map<String,String> expectedHeaders = this.getExpectedHeaders();
            for(String name :  expectedHeaders.keySet()){
                String value = expectedHeaders.get(name);
                Assert.assertEquals(value, response.getHeader(name));
            }

            if(actualStatusCode != HttpServletResponse.SC_NOT_MODIFIED){
                Assert.assertTrue(this.hasCorrectDateHeaders());
                String actualOutput = servletTestModule.getOutput();
                //!TODO for now hash is ignored bcoz it will differ based last modification time of the resource
                //!TODO need to consider and test actual generated hash
                actualOutput = actualOutput.replaceAll("_wu_[0-9a-f]{32}\\.","_wu_<ignore_hash>.");

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
