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

import com.googlecode.webutilities.filters.CompressionFilter;
import com.googlecode.webutilities.servlets.JSCSSMergeServlet;
import com.googlecode.webutilities.test.util.TestUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.googlecode.webutilities.common.Constants.*;

public class CompressionFilterTest extends AbstractFilterTest {

    private JSCSSMergeServlet jscssMergeServlet = new JSCSSMergeServlet();

    private CompressionFilter compressionFilter = new CompressionFilter();

    private static final Logger LOGGER = LoggerFactory.getLogger(CompressionFilterTest.class.getName());

    @Override
    protected String getTestPropertiesName() {
        return CompressionFilterTest.class.getSimpleName() + ".properties";
    }

    @Override
    public void prepare() {

        servletTestModule.setServlet(jscssMergeServlet, true);

        servletTestModule.addFilter(compressionFilter, true);

        servletTestModule.setDoChain(true);

    }

    @Override
    public void executeCurrentTestLogic() throws Exception {
        servletTestModule.doFilter();

        String actualResponseEncoding = webMockObjectFactory.getMockResponse().getHeader(HTTP_CONTENT_ENCODING_HEADER);

        String actualVary = webMockObjectFactory.getMockResponse().getHeader(HTTP_VARY_HEADER);

        String expectedEncoding = this.getExpectedProperty("contentEncoding");

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
    }

}
