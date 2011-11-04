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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DirectivePair {

    private PreChainDirective preChainDirective;

    private PostChainDirective postChainDirective;

    //protected ServletContext context;

    public DirectivePair(PreChainDirective preChainDirective, PostChainDirective postChainDirective) {
        this.preChainDirective = preChainDirective;
        this.postChainDirective = postChainDirective;
    }

//    public void setContext(ServletContext context) {
//        this.context = context;
//    }

    public ModuleResponse getResponse(HttpServletRequest request, HttpServletResponse response) {
        if (response instanceof ModuleResponse) {
            return (ModuleResponse) response;
        } else {
            return new ModuleResponse(response);
        }
    }

    public ModuleRequest getRequest(HttpServletRequest request) {
        if (request instanceof ModuleRequest) {
            return (ModuleRequest) request;
        } else {
            return new ModuleRequest(request);
        }
    }

    public PreChainDirective getPreChainDirective() {
        return preChainDirective;
    }

//    public void setPreChainDirective(PreChainDirective preChainDirective) {
//        this.preChainDirective = preChainDirective;
//    }

    public PostChainDirective getPostChainDirective() {
        return postChainDirective;
    }

//    public void setPostChainDirective(PostChainDirective postChainDirective) {
//        this.postChainDirective = postChainDirective;
//    }

}
