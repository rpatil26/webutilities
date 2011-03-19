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

package com.googlecode.webutilities.test.tags;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import junit.framework.TestCase;

import com.googlecode.webutilities.tags.YUIMinTag;
import com.googlecode.webutilities.test.util.TestUtils;
import com.mockrunner.mock.web.WebMockObjectFactory;
import com.mockrunner.tag.NestedTag;
import com.mockrunner.tag.TagTestModule;

public class YUIMinTagTest extends TestCase {

    private WebMockObjectFactory webMockObjectFactory = new WebMockObjectFactory();

    private TagTestModule tagTestModule = new TagTestModule(webMockObjectFactory);

    private NestedTag yuiMinTag;

    private Properties properties = new Properties();

    private int currentTestNumber = 1;

    private static final Logger logger = Logger.getLogger(YUIMinTagTest.class.getName());

    public YUIMinTagTest() throws Exception {
        properties.load(this.getClass().getResourceAsStream(YUIMinTagTest.class.getSimpleName() + ".properties"));
    }

    private void setUpTag() {
        Map<Object, Object> attributeMap = new HashMap<Object, Object>();
        String value = properties.getProperty(this.currentTestNumber + ".test.init.params");
        if (value != null && !value.trim().equals("")) {
            String[] params = value.split(",");
            for (String param : params) {
                String[] keyAndValue = param.split(":");
                attributeMap.put(keyAndValue[0], keyAndValue[1]);
            }
        }

        yuiMinTag = tagTestModule.createNestedTag(YUIMinTag.class, attributeMap);

    }

    private void setUpTagBodyContent() throws Exception {
        String resourcesString = properties.getProperty(this.currentTestNumber + ".test.resources");
        if (resourcesString != null && !resourcesString.trim().equals("")) {
            String[] resources = resourcesString.split(",");
            for (String resource : resources) {
                logger.info("Setting resource : " + resource);
                yuiMinTag.addTextChild(TestUtils.readContents(this.getClass().getResourceAsStream(resource)));
            }
        }
    }

    private String getExpectedOutput() throws Exception {

        String expectedResource = properties.getProperty(this.currentTestNumber + ".test.expected");
        if (expectedResource == null || expectedResource.trim().equals("")) return null;
        return TestUtils.readContents(this.getClass().getResourceAsStream(expectedResource));

    }

    private void pre() throws Exception {


        webMockObjectFactory = new WebMockObjectFactory();

        tagTestModule = new TagTestModule(webMockObjectFactory);

        this.setUpTag();

        this.setUpTagBodyContent();

    }

    public void testTagUsingDifferentScenarios() throws Exception {

        while (true) {
            this.pre();

            String testCase = properties.getProperty(this.currentTestNumber + ".test.name");

            if (testCase == null || testCase.trim().equals("")) {
                return; // no more test cases in properties file.
            }

            logger.info("Running Test (" + this.currentTestNumber + "): " + testCase + "");

            System.out.println("##################################################################################################################");
            System.out.println("Running Test (" + this.currentTestNumber + "): " + testCase + "");
            System.out.println("##################################################################################################################");

            yuiMinTag.doLifecycle();

            String actualOutput = tagTestModule.getOutput();

            assertNotNull(actualOutput);

            String expectedOutput = this.getExpectedOutput();

            assertEquals(expectedOutput.trim(), actualOutput.trim());

            this.post();

        }

    }


    private void post() {
        this.currentTestNumber++;
    }

}
