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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public abstract class BaseModule implements IModule {

    protected ServletContext context;

    public void setContext(ServletContext context) {
        this.context = context;
    }

    public ModuleRules activeModuleRules = new ModuleRules();

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

    @Override
    public void parseConfigRules(String ruleString) {
        List<IRule> moduleRules = parseRules(ruleString);
        if (moduleRules != null && moduleRules.size() > 0) {
            for (IRule rule : moduleRules) {
                this.activeModuleRules.addRule(rule);
            }
        }

    }

    public ModuleRules getActiveRules() {
        return activeModuleRules;
    }

    //Config time
    protected abstract List<IRule> parseRules(String ruleString);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseModule that = (BaseModule) o;

        return !(activeModuleRules != null ? !activeModuleRules.equals(that.activeModuleRules) : that.activeModuleRules != null);

    }

    @Override
    public int hashCode() {
        return activeModuleRules != null ? activeModuleRules.hashCode() : 0;
    }
}

