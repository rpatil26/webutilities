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

package com.googlecode.webutilities.modules.infra;

public interface IRule {

    static enum Status {
        CONTINUE,
        NO_CHAIN,
        STOP
    }

//    static final int CONTINUE = 0;
//    static final int NO_CHAIN = 1;
//    static final int STOP = 2;


    Status process(ModuleRequest request, ModuleResponse response);

}
