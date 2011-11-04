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

import com.googlecode.webutilities.test.filters.CharacterEncodingFilterTest;
import com.googlecode.webutilities.test.filters.CompressionFilterTest;
import com.googlecode.webutilities.test.filters.ResponseCacheFilterTest;
import com.googlecode.webutilities.test.filters.YUIMinFilterTest;
import com.googlecode.webutilities.test.tags.URLTagTest;
import com.googlecode.webutilities.test.tags.YUIMinTagTest;
import org.junit.runners.Suite;


/**
 * Test suit for webutilities tests
 */
@Suite.SuiteClasses({CompressionFilterTest.class,
    YUIMinFilterTest.class,
    YUIMinTagTest.class,
    CharacterEncodingFilterTest.class,
    CompressionFilterTest.class,
    ResponseCacheFilterTest.class,
    URLTagTest.class})
public class WebutilitiesTestSuite {

}
