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
import javax.servlet.http.HttpServletResponse;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Module similar to apache's mod_access
 * <p/>
 * Example Rules
 * Access Allow from all|host|subnet
 * Access Deny from all|host|subnet
 */
public class AccessModule implements IModule {

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

    Set<IpSubnet> subnets = new HashSet<IpSubnet>();

    AllowRule(String hosts) {
        String[] multiple = hosts.split("\\s+");
        for (String host : multiple) {
            if ("all".equals(host)) {
                host = "0.0.0.0/0";
            }
            try {
                subnets.add(new IpSubnet(host));
            } catch (UnknownHostException ex) {
                ex.printStackTrace();
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
                ex.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public int execute(ModuleRequest request, ModuleResponse response, ServletContext context) {
        String hostName = request.getRemoteHost();
        if (!this.allow(hostName)) {
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return IDirective.STOP;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return IDirective.OK;
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

class IpSubnet {

    private long network;

    private long netmask;

    private static final Pattern PATTERN = Pattern.compile("((?:\\d|\\.)+)(?:/(\\d{1,2}))?");

    public IpSubnet(String ipRange) throws UnknownHostException {
        Matcher matcher = PATTERN.matcher(ipRange);
        if (matcher.matches()) {
            String networkPart = matcher.group(1);
            String cidrPart = matcher.group(2);
            init(networkPart, cidrPart);
        } else {
            init(ipRange, "32");
        }
    }

    private void init(String networkPart, String cidrPart) throws UnknownHostException {

        long netmask = 0;
        int cidr = cidrPart == null ? 32 : Integer.parseInt(cidrPart);
        for (int pos = 0; pos < 32; ++pos) {
            if (pos >= 32 - cidr) {
                netmask |= (1L << pos);
            }
        }

        this.network = netmask & toMask(InetAddress.getByName(networkPart));
        this.netmask = netmask;
    }

    public boolean isInRange(InetAddress address) {
        return network == (toMask(address) & netmask);
    }

    static long toMask(InetAddress address) {
        byte[] data = address.getAddress();
        long accum = 0;
        int idx = 3;
        for (int shiftBy = 0; shiftBy < 32; shiftBy += 8) {
            accum |= ((long) (data[idx] & 0xff)) << shiftBy;
            idx--;
        }
        return accum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IpSubnet ipSubnet = (IpSubnet) o;

        return netmask == ipSubnet.netmask && network == ipSubnet.network;

    }

    @Override
    public int hashCode() {
        int result = (int) (network ^ (network >>> 32));
        result = 31 * result + (int) (netmask ^ (netmask >>> 32));
        return result;
    }
}