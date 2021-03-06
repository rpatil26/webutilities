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

package com.googlecode.webutilities.modules.ne;


import com.googlecode.webutilities.filters.common.IpSubnet;
import com.googlecode.webutilities.modules.infra.ModuleRequest;
import com.googlecode.webutilities.modules.infra.ModuleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

/**
 * Module similar to apache's mod_access
 * <p>
 * Example Rules
 * Access Allow from all|host|subnet
 * Access Deny from all|host|subnet
 */
public class AccessModule implements IModule {

    public static final Logger LOGGER = LoggerFactory.getLogger(AccessModule.class.getName());

    @Override
    public DirectivePair parseDirectives(String ruleString) {
        DirectivePair pair = null;
        int index = 0;
        String[] tokens = ruleString.split("\\s+");

        assert tokens.length >= 4;

        if (!tokens[index++].equals(AccessModule.class.getSimpleName())) return pair;

        String directive = tokens[index++];

        if (!"from".equals(tokens[index++])) {
            return pair;
        }

        String hosts = ruleString.substring(ruleString.indexOf(tokens[index]));

        assert directive != null;

        pair = new DirectivePair(directive.equals("Allow") ?
                new AllowRule(hosts)
                : new DenyRule(hosts), null);

        return pair;
    }

}

class AllowRule implements PreChainDirective {

    public static final Logger LOGGER = LoggerFactory.getLogger(AllowRule.class.getName());

    Set<IpSubnet> subnets = new HashSet<>();

    AllowRule(String hosts) {
        String[] multiple = hosts.split("\\s+");
        for (String host : multiple) {
            if ("all".equals(host)) {
                host = "0.0.0.0/0";
            }
            try {
                subnets.add(new IpSubnet(host));
            } catch (UnknownHostException ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }

    }

    boolean allow(String hostname) {
        for (IpSubnet subnet : subnets) {
            try {
                if (subnet.isInRange(Inet4Address.getByName(hostname))) {
                    return true;
                }
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }
        return false;
    }

    @Override
    public int execute(ModuleRequest request, ModuleResponse response, ServletContext context) {
        String hostName = request.getRemoteHost();
        if (this.allow(hostName)) {
            return IDirective.OK;
        }
        try {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        return IDirective.STOP;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AllowRule allowRule = (AllowRule) o;

        return !(subnets != null ? !subnets.equals(allowRule.subnets) : allowRule.subnets != null);

    }

    @Override
    public int hashCode() {
        return subnets != null ? subnets.hashCode() : 0;
    }
}

class DenyRule extends AllowRule {

    DenyRule(String hosts) {
        super(hosts);
    }

    @Override
    boolean allow(String hostname) {
        return !super.allow(hostname);
    }

}

