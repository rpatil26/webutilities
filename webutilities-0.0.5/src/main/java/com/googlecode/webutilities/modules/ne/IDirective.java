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

package com.googlecode.webutilities.modules.ne;

import com.googlecode.webutilities.modules.infra.ModuleRequest;
import com.googlecode.webutilities.modules.infra.ModuleResponse;

import javax.servlet.ServletContext;

public interface IDirective {

    public static final int OK = 0;

    public static final int STOP = 1;

    public static final int STOP_CHAIN = 2;

    int execute(ModuleRequest request, ModuleResponse response, ServletContext context);

}
