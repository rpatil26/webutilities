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

import java.util.ArrayList;
import java.util.List;

public class ModuleRules {

    private List<PreChainRule> preChainRules = new ArrayList<PreChainRule>();

    private List<PostChainRule> postChainRules = new ArrayList<PostChainRule>();

    public boolean addRule(IRule rule){
        if(rule instanceof PostChainRule){
            return postChainRules.add((PostChainRule)rule);
        }else{
            return preChainRules.add((PreChainRule)rule);
        }
    }

    public List<PreChainRule> getPreChainRules() {
        return preChainRules;
    }

    public List<PostChainRule> getPostChainRules() {
        return postChainRules;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModuleRules)) return false;

        ModuleRules that = (ModuleRules) o;

        return !(postChainRules != null ?
                !postChainRules.equals(that.postChainRules) :
                that.postChainRules != null) && (preChainRules != null ?
                !preChainRules.equals(that.preChainRules) :
                that.preChainRules == null);

    }

    @Override
    public int hashCode() {
        int result = preChainRules != null ? preChainRules.hashCode() : 0;
        result = 31 * result + (postChainRules != null ? postChainRules.hashCode() : 0);
        return result;
    }
}
