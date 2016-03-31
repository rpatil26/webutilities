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

import com.googlecode.webutilities.common.Constants;
import com.googlecode.webutilities.filters.YUIMinFilter;
import com.googlecode.webutilities.servlets.JSCSSMergeServlet;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class YUIMinFilterTest extends AbstractFilterTest {

    private JSCSSMergeServlet jscssMergeServlet = new MockJSCSSMergeServlet();

    private YUIMinFilter yuiMinFilter = new YUIMinFilter();

    private static final Logger LOGGER = LoggerFactory.getLogger(YUIMinFilterTest.class.getName());

    @Override
    protected String getTestPropertiesName() {
        return YUIMinFilterTest.class.getSimpleName() + ".properties";
    }

    @Override
    public void prepare() {

        servletTestModule.setServlet(jscssMergeServlet, true);

        servletTestModule.addFilter(yuiMinFilter, true);
        servletTestModule.setDoChain(true);

    }

    @Override
    public void executeCurrentTestLogic() throws Exception {
        servletTestModule.doFilter();

        String actualOutput = servletTestModule.getOutput();

        Assert.assertNotNull(actualOutput);

        String expectedOutput = this.getExpectedOutput();

        Assert.assertEquals(expectedOutput.trim(), actualOutput.trim());

        Assert.assertEquals("" + actualOutput.length(), webMockObjectFactory.getMockResponse().getHeader("Content-Length"));
    }

}

class MockJSCSSMergeServlet extends com.googlecode.webutilities.servlets.JSCSSMergeServlet {

    private static final String TEST_JS_EXT = ".testjs";

    private static final String TEST_CSS_EXT = ".testcss";

    private static final String TEST_JSON_EXT = ".testjson";


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String url = req.getRequestURI();
        super.doGet(req, resp);
        if (url.endsWith(TEST_JS_EXT)) {
            resp.setContentType(Constants.MIME_JS);
        }
        if (url.endsWith(TEST_CSS_EXT)) {
            resp.setContentType(Constants.MIME_CSS);
        }
        if (url.endsWith(TEST_JSON_EXT)) {
            resp.setContentType(Constants.MIME_JSON);
        }
    }

    @Override
    protected boolean isCSS(String resourcePath) {
        return resourcePath.endsWith(TEST_CSS_EXT) || super.isCSS(resourcePath);
    }
}