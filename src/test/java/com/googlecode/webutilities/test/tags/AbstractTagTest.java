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

package com.googlecode.webutilities.test.tags;

import com.googlecode.webutilities.test.AbstractWebComponentTest;
import com.mockrunner.tag.TagTestModule;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractTagTest extends AbstractWebComponentTest {

    TagTestModule tagTestModule;

    protected final Map<Object, Object> attributeMap = new HashMap<>();

    @Override
    protected void setupInitParam(String name, String value) {
        attributeMap.put(name, value);
    }

    @Override
    public void setupInitParams() {
        String value = properties.getProperty(this.currentTestNumber + ".test.init.params");
        if (value != null && !value.trim().equals("")) {
            String[] params = value.split(",");
            for (String param : params) {
                String[] keyAndValue = param.split(":");
                this.setupInitParam(keyAndValue[0], keyAndValue[1]);
            }
        }
    }

    @Override
    public void setupResources() {
        //Do nothing to set resources
    }

    @Override
    public void prepare() throws Exception {
        this.setupTag();
        this.setupTagBodyContent();
    }

    @Override
    protected void initModule() {
        tagTestModule = new TagTestModule(webMockObjectFactory);
    }

    protected abstract void setupTag();

    public abstract void setupTagBodyContent() throws Exception;

}
