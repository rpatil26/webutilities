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

package com.googlecode.webutilities.filters;

import com.googlecode.webutilities.common.Constants;
import com.googlecode.webutilities.filters.common.AbstractFilter;
import com.googlecode.webutilities.filters.common.IpSubnet;
import com.googlecode.webutilities.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

public class AllowDenyFilter extends AbstractFilter {

    private String allowFrom;

    private String denyFrom;

    Set<IpSubnet> allowedSubnets = new HashSet<IpSubnet>();

    Set<IpSubnet> deniedSubnets = new HashSet<IpSubnet>();

    private int errorCodeToReturn = HttpServletResponse.SC_FORBIDDEN;

    //http://www.enterprisenetworkingplanet.com/netsp/article.php/3566521/Networking-101-Understanding-Subnets-and-CIDR.htm
    private static final String INIT_PARAM_ALLOW_FROM = "allowFrom"; //default "all" or CIDR Subnet Mask. x.x.x.x/y where y is (0-32)

    private static final String INIT_PARAM_DENY_FROM = "denyFrom"; // default "" (none) or CIDR Subnet Mask

    private static final String INIT_PARAM_ERROR_CODE = "errorCodeToReturn"; // default 403

    private static final Logger LOGGER = LoggerFactory.getLogger(AllowDenyFilter.class.getName());

    public void init(FilterConfig config) throws ServletException {
        super.init(config);

        this.allowFrom = Utils.readString(filterConfig.getInitParameter(INIT_PARAM_ALLOW_FROM), null);
        this.denyFrom = Utils.readString(filterConfig.getInitParameter(INIT_PARAM_DENY_FROM), null);
        this.errorCodeToReturn = Utils.readInt(filterConfig.getInitParameter(INIT_PARAM_ERROR_CODE), this.errorCodeToReturn);

        try {
            this.buildSubjects();
        } catch (UnknownHostException uhe) {
            LOGGER.error("Init failed:", uhe);
            throw new ServletException("Unknown host", uhe);
        } catch (IllegalArgumentException uhe) {
            LOGGER.error("Init failed:", uhe);
            throw new ServletException("Invalid allow/deny formats", uhe);
        }

        LOGGER.debug("Filter initialized with: {}:{}, {}:{}", INIT_PARAM_ALLOW_FROM, allowFrom, INIT_PARAM_DENY_FROM, denyFrom);

    }

    private void buildSubjects() throws IllegalArgumentException, UnknownHostException {
        String[] tokens;
        if (allowFrom != null) {
            tokens = allowFrom.split("\\s+");
            for (String host : tokens) {
                if ("all".equals(host)) {
                    host = "0.0.0.0/0";
                }
                allowedSubnets.add(new IpSubnet(host));
            }
        }
        if (denyFrom != null) {
            tokens = denyFrom.split("\\s+");
            for (String host : tokens) {
                if ("all".equals(host)) {
                    host = "0.0.0.0/0";
                }
                deniedSubnets.add(new IpSubnet(host));
            }
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String url = req.getRequestURI();
        if (isURLAccepted(url)
                && isQueryStringAccepted(req.getQueryString())
                && isUserAgentAccepted(req.getHeader(Constants.HTTP_USER_AGENT_HEADER))) {
            String remoteIp = req.getRemoteAddr();

            if (!this.isAccessAllowedFrom(remoteIp)) {
                try {
                    resp.sendError(this.errorCodeToReturn);  //return specified error code
                    return; // Don't allow further access
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        }
        // Grant access if we reach here
        chain.doFilter(req, resp);
    }

    private boolean isAccessAllowedFrom(String remoteIp) {
        //Process Allowed rules and grant access if match
        for (IpSubnet allowSubnet : allowedSubnets) {
            try {
                if (allowSubnet.isInRange(Inet4Address.getByName(remoteIp))) {
                    return true;
                }
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }

        //Then process Denied rules and deny access if match
        for (IpSubnet denySubnet : deniedSubnets) {
            try {
                if (denySubnet.isInRange(Inet4Address.getByName(remoteIp))) {
                    return false;
                }
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }

        //If neither allowed nor denied, grant access by default
        return true;
    }
}
