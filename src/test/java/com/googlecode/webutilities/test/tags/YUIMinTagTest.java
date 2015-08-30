/*
 * Copyright 2010-2015 Rajendra Patil
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.webutilities.test.tags;

import com.googlecode.webutilities.tags.YUIMinTag;
import com.googlecode.webutilities.test.util.TestUtils;
import com.mockrunner.tag.NestedTag;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YUIMinTagTest extends AbstractTagTest {

    private NestedTag yuiMinTag;

    private static final Logger LOGGER = LoggerFactory.getLogger(YUIMinTagTest.class.getName());

    @Override
    protected String getTestPropertiesName() {
        return YUIMinTagTest.class.getSimpleName() + ".properties";
    }

    @Override
    public void setupTagBodyContent() throws Exception {
        String resourcesString = properties.getProperty(this.currentTestNumber + ".test.resources");
        if (resourcesString != null && !resourcesString.trim().equals("")) {
            String[] resources = resourcesString.split(",");
            for (String resource : resources) {
                LOGGER.trace("Setting resource : {}", resource);
                yuiMinTag.addTextChild(TestUtils.readContents(this.getClass().getResourceAsStream(resource),
                        webMockObjectFactory.getMockResponse().getCharacterEncoding()) + "\n");
            }
        }
    }

    @Override
    public void executeCurrentTestLogic() throws Exception {
        yuiMinTag.doLifecycle();

        String actualOutput = tagTestModule.getOutput();

        Assert.assertNotNull(actualOutput);

        String expectedOutput = this.getExpectedOutput();

        Assert.assertEquals(expectedOutput.trim(), actualOutput.trim());
    }

    @Override
    public void setupTag() {
        yuiMinTag = tagTestModule.createNestedTag(YUIMinTag.class, this.attributeMap);
    }
}
