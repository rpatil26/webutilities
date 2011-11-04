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

import com.googlecode.webutilities.tags.YUIMinTag;
import com.googlecode.webutilities.test.util.TestUtils;
import com.mockrunner.mock.web.WebMockObjectFactory;
import com.mockrunner.tag.NestedTag;
import com.mockrunner.tag.TagTestModule;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class YUIMinTagTest {

    private WebMockObjectFactory webMockObjectFactory = new WebMockObjectFactory();

    private TagTestModule tagTestModule = new TagTestModule(webMockObjectFactory);

    private NestedTag yuiMinTag;

    private Properties properties = new Properties();

    private int currentTestNumber = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(YUIMinTagTest.class.getName());

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
                LOGGER.info("Setting resource : {}", resource);
                yuiMinTag.addTextChild(TestUtils.readContents(this.getClass().getResourceAsStream(resource), webMockObjectFactory.getMockResponse().getCharacterEncoding()));
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

        tagTestModule = new TagTestModule(webMockObjectFactory);

        this.setUpTag();

        this.setUpTagBodyContent();

    }

    @Test
    public void testTagUsingDifferentScenarios() throws Exception {

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


            yuiMinTag.doLifecycle();

            String actualOutput = tagTestModule.getOutput();

            Assert.assertNotNull(actualOutput);

            String expectedOutput = this.getExpectedOutput();

            Assert.assertEquals(expectedOutput.trim(), actualOutput.trim());

            this.post();

        }

    }


    private void post() {
        this.currentTestNumber++;
    }

}
