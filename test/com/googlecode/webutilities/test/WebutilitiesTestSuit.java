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

package com.googlecode.webutilities.test;

import com.googlecode.webutilities.test.filters.*;
import com.googlecode.webutilities.test.servlets.JSCSSMergeServletTest;
import com.googlecode.webutilities.test.tags.YUIMinTagTest;
import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Test suit for webutilities tests
 */
public class WebutilitiesTestSuit extends TestSuite {

    public static Test suite() {
        TestSuite suite
                = new TestSuite("Test suite for com.googlecode.webutilities");

        suite.addTestSuite(JSCSSMergeServletTest.class);
        suite.addTestSuite(YUIMinFilterTest.class);
        suite.addTestSuite(YUIMinTagTest.class);
        suite.addTestSuite(CharacterEncodingFilterTest.class);
        suite.addTestSuite(CompressionFilterTest.class);
        suite.addTestSuite(ResponseCacheFilterTest.class);


        return suite;
    }

}
