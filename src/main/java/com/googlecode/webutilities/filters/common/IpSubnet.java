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

package com.googlecode.webutilities.filters.common;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IpSubnet {

    private long network;

    private long netmask;
    //Pattern Credit: http://blog.markhatton.co.uk/2011/03/15/regular-expressions-for-ip-addresses-cidr-ranges-and-hostnames/
    private static final Pattern PATTERN = Pattern.compile("^((([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))(?:\\/([0-9]|[1-2][0-9]|3[0-2]))?$");

    public IpSubnet(String ipRange) throws UnknownHostException, IllegalArgumentException {
        ipRange = ipRange.trim();
        Matcher matcher = PATTERN.matcher(ipRange);
        if (matcher.matches()) {
            String networkPart = matcher.group(1);
            String cidrPart = matcher.group(5);
            init(networkPart, cidrPart);
        } else {
            throw new IllegalArgumentException("Invalid IPV4 CIDR Subnet Mask Format:" + ipRange);
        }
    }

    private void init(String networkPart, String cidrPart) throws UnknownHostException {

        long netmask = 0;
        int cidr = cidrPart == null || cidrPart.trim().length() < 1 ? 32 : Integer.parseInt(cidrPart);
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