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

package com.googlecode.webutilities.test.filters;

import com.googlecode.webutilities.filters.ResponseCacheFilter;
import org.junit.Ignore;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

@Ignore
public class ExtendedMockResponseCacheFilter extends ResponseCacheFilter {

    private boolean resetCache = true; //reset cache once in the start

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        if (this.resetCache) {
            this.invalidateCache(); //every time start with fresh cache
            this.resetCache = false;
        }
    }
}
