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
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Charset Module
 * <p/>
 * Example rules
 * Charset UTF-8 force
 */
public class CharsetModule implements IModule {

    @Override
    public DirectivePair parseDirectives(String ruleString) {

        DirectivePair pair = null;

        int index = 0;

        String[] splits = ruleString.split("\\s+");

        assert splits.length >= 2;

        if (!splits[index++].equals(Charset.class.getSimpleName())) return pair;

        boolean force = false;

        String encoding = splits[index++];

        assert encoding != null;

        if (splits.length > 2 && "force".equals(splits[index])) {
            force = true;
        }
        pair = new DirectivePair(new CharsetDirective(encoding, force), null);

        return pair;
    }

}

class CharsetDirective implements PreChainDirective {

    String encoding;

    boolean force;

    public CharsetDirective(String encoding, boolean force) {
        this.encoding = encoding;
        this.force = force;
    }

    @Override
    public int execute(ModuleRequest request, ModuleResponse response, ServletContext context) {
        if (encoding != null) {
            try {
                request.setCharacterEncoding(encoding);
                if (force) {
                    try {
                        response.setCharacterEncoding(encoding);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return IDirective.OK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CharsetDirective that = (CharsetDirective) o;

        return force == that.force && !(encoding != null ? !encoding.equals(that.encoding) : that.encoding != null);

    }

    @Override
    public int hashCode() {
        int result = encoding != null ? encoding.hashCode() : 0;
        result = 31 * result + (force ? 1 : 0);
        return result;
    }
}