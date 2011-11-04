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

import com.googlecode.webutilities.common.Constants;
import com.googlecode.webutilities.filters.common.AbstractFilter;
import com.googlecode.webutilities.filters.common.IgnoreAcceptContext;
import com.googlecode.webutilities.modules.infra.ModuleRequest;
import com.googlecode.webutilities.modules.infra.ModuleResponse;
import com.googlecode.webutilities.util.Utils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewModulesFilter extends AbstractFilter {

    public static final String DEFAULT_MODULES_PACKAGE = "com.googlecode.webutilities.modules.ne";

    private Config config = null;

    public static final Logger LOGGER = LoggerFactory.getLogger(NewModulesFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);

        String configFile = filterConfig.getInitParameter("configFile");
        if (configFile != null && !configFile.trim().equals("")) {
            try {
                config = Config.load(new FileInputStream(configFile));
            } catch (FileNotFoundException ex) {
                LOGGER.error("Specified file could not be loaded. {}", configFile);
                throw new ServletException("Could not load config file: " + configFile);
            }
            LOGGER.debug("Found config file in the classpath. {}", configFile);
        }

        if (config == null) {
            LOGGER.debug("Using default config file.");
            config = Config.load();
        }
    }

//    private IRule.Status process(Iterator iterator, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain chain) throws IOException {
//
//        if (iterator != null && iterator.hasNext()) {
//
//            BaseModule module = (BaseModule) iterator.next();
//            ModuleRequest moduleRequest = module.getRequest(httpServletRequest);
//            ModuleResponse moduleResponse = module.getResponse(moduleRequest, httpServletResponse);
//
//            LOGGER.fine("Processing Module: ", module.getClass().getName()));
//
//            IRule.Status status = IRule.Status.CONTINUE;
//
//            for (PreChainRule rule : module.activeModuleRules.getPreChainRules()) {
//                status = rule.process(moduleRequest, moduleResponse);
//                if (IRule.Status.CONTINUE != status) {
//                    LOGGER.warn("PreChainRule ", rule.getClass().getName(), " returned false | no_chain"));
//                    break;
//                }
//            }
//            if (IRule.Status.NO_CHAIN != status) {
//                status = process(iterator, moduleRequest, moduleResponse, chain);
//                if (status != IRule.Status.CONTINUE) {
//                    return status;
//                }
//            }
//            for (PostChainRule rule : module.activeModuleRules.getPostChainRules()) {
//                status = rule.process(moduleRequest, moduleResponse);
//                if (status != IRule.Status.CONTINUE) {
//                    LOGGER.warn("PostChainRule ", rule.getClass().getName(), " returned false"));
//                    return status;
//                }
//            }
//            moduleResponse.commit();
//        } else {
//            try {
//                LOGGER.finest("Doing chaining, finally.");
//                chain.doFilter(httpServletRequest, httpServletResponse);
//            } catch (Exception ex) {
//                ex.getCause().printStackTrace(httpServletResponse.getWriter());
//                if (httpServletResponse instanceof ModuleResponse) {
//                    ((ModuleResponse) httpServletResponse).commit();
//                }
//                LOGGER.error("Error in chaining.", ex.getMessage()));
//                return IRule.Status.NO_CHAIN;
//            }
//        }
//
//        return IRule.Status.CONTINUE;
//    }

    private List<DirectivePair> getAllEligibleRules(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String requestMime = httpRequest.getContentType();
        String userAgent = httpRequest.getHeader(Constants.HTTP_USER_AGENT_HEADER);
        String url = getURL(httpRequest);
        String responseMime = httpResponse.getContentType();
        if (requestMime == null) {
            responseMime = Utils.selectMimeByFile(url);
        }
        List<DirectivePair> eligibleRules = null;
        for (RulesMapping rulesMapping : config.rulesMappings) {
            if (rulesMapping.isURLAccepted(url) && (rulesMapping.isMIMEAccepted(requestMime) || rulesMapping.isMIMEAccepted(responseMime)) && rulesMapping.isUserAgentAccepted(userAgent)) {
                if (eligibleRules == null)
                    eligibleRules = new ArrayList<DirectivePair>(); //lazy init
                for (DirectivePair rules : rulesMapping.getRules()) {
                    if (!eligibleRules.contains(rules)) {
                        //((BaseModule) module).setContext(this.filterConfig.getServletContext());
                        eligibleRules.add(rules);
                    }
                }

            }
        }
        LOGGER.debug("Found {} rules", eligibleRules != null ? eligibleRules.size() : 0);
        return eligibleRules == null ? Collections.<DirectivePair>emptyList() : eligibleRules;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest moduleRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse moduleResponse = (HttpServletResponse) servletResponse;
        List<DirectivePair> allEligibleRules = getAllEligibleRules(moduleRequest, moduleResponse);
        Stack<PostChainDirective> postChainRules = new Stack<PostChainDirective>();
        int status = IDirective.OK;
        //pre chain rules
        for (DirectivePair rulePair : allEligibleRules) {
            moduleRequest = rulePair.getRequest(moduleRequest);
            moduleResponse = rulePair.getResponse(moduleRequest, moduleResponse);
            //rulePair.setContext(this.filterConfig.getServletContext());
            if (rulePair.getPostChainDirective() != null) {
                postChainRules.push(rulePair.getPostChainDirective());
            }
            status = rulePair.getPreChainDirective().execute((ModuleRequest) moduleRequest, (ModuleResponse) moduleResponse, this.filterConfig.getServletContext());
            if (status != IDirective.OK) {
                break;
            }
        }
        if (status == IDirective.STOP) {
            while (!postChainRules.empty()) {
                PostChainDirective postChainRule = postChainRules.pop();
                postChainRule.execute((ModuleRequest) moduleRequest, (ModuleResponse) moduleResponse, this.filterConfig.getServletContext());
            }
            try {
                ((ModuleResponse) moduleResponse).commit();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return;
        }
        if (status != IDirective.STOP_CHAIN) {
            //chaining
            try {
                LOGGER.trace("Doing chaining, finally.");
                filterChain.doFilter(servletRequest, servletResponse);
                //servletResponse.getWriter().close();
            } catch (Exception ex) {
                ex.getCause().printStackTrace(servletResponse.getWriter());
                if (moduleResponse instanceof ModuleResponse) {
                    ((ModuleResponse) moduleResponse).commit();
                }
                LOGGER.error("Error in chaining.", ex);
                return;
            }
        }
        //post chain rules
        while (!postChainRules.empty()) {
            PostChainDirective postChainRule = postChainRules.pop();
            postChainRule.execute((ModuleRequest) moduleRequest, (ModuleResponse) moduleResponse, this.filterConfig.getServletContext());
        }
        if (moduleResponse instanceof ModuleResponse) {
            try {
                ((ModuleResponse) moduleResponse).commit();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    private String getURL(HttpServletRequest request) {
        return Utils.removeFingerPrint(request.getRequestURI());
    }

    public static void main(String[] args) {
        Config config = Config.load();
        LOGGER.debug("Rule mappings count:{}",config.getRuleMappings().size());
    }

    /**
     * Class that holds the configuration details. It parses and loads the modules their rules from
     * modules.conf file. Default load API without any param uses the default modules.conf file
     * bundled with the jar.
     * <p/>
     * Default modules.conf has default standard optimization rules for JS, CSS, Images. Refer default
     * modules.conf file for more details.
     */

    public static class Config {


        private final List<RulesMapping> rulesMappings = new ArrayList<RulesMapping>();

        private static String extractRegExFor(String line, String param) {
            int index = line.indexOf(param);
            if (index < 0) return null;
            String regEx = line.substring(index + param.length() + 1).trim();
            if (regEx.startsWith("\"")) {
                regEx = regEx.substring(1, regEx.substring(1).indexOf("\""));
            } else {
                int spaceAt = regEx.indexOf(" ");
                if (spaceAt > 0) {
                    regEx = regEx.substring(0, spaceAt);
                } else {
                    regEx = regEx.substring(0);
                }
            }
            return regEx;
        }


        public static Config load() {

            InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("modules.conf");
            if (inputStream == null) {
                inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("/modules.conf");
                if (inputStream == null) {
                    inputStream = NewModulesFilter.class.getResourceAsStream("/modules.conf");
                }
            }
            return load(inputStream);

        }

        public static Config load(InputStream inputStream) {
            Config config = new Config();

            if (inputStream != null) {


                String ignoreURLPattern;

                String acceptURLPattern;

                String ignoreMIMEPattern;

                String acceptMIMEPattern;

                String ignoreUAPattern;

                String acceptUAPattern;

                String line;

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                RulesMapping currentMapping = null;

                try {
                    while ((line = bufferedReader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("#") || line.equals("")) {
                            continue;
                        }

                        if (line.startsWith("for")) {

                            if (currentMapping != null) {
                                config.rulesMappings.add(currentMapping);
                            }

                            acceptURLPattern = extractRegExFor(line, "acceptURL");
                            ignoreURLPattern = extractRegExFor(line, "ignoreURL");

                            acceptMIMEPattern = extractRegExFor(line, "acceptMIME");
                            ignoreMIMEPattern = extractRegExFor(line, "ignoreMIME");

                            acceptUAPattern = extractRegExFor(line, "acceptUA");
                            ignoreUAPattern = extractRegExFor(line, "ignoreUA");

                            currentMapping = new RulesMapping(ignoreURLPattern, acceptURLPattern, ignoreMIMEPattern, acceptMIMEPattern, ignoreUAPattern, acceptUAPattern);

                            continue;
                        }

                        if (currentMapping != null) {
                            String handlerName = line.split(" ")[0];
                            handlerName = DEFAULT_MODULES_PACKAGE + "." + handlerName;
                            IModule module = null; //currentMapping.getModule(handlerName);
                            //if (module == null) {
                            try {

                                Class moduleClass = Class.forName(handlerName);

                                Constructor constructor = moduleClass.getConstructor();

                                module = (IModule) constructor.newInstance();

                                //currentMapping.addModule(handlerName, module);

                            } catch (ClassNotFoundException ex) {
                                ex.printStackTrace();
                            } catch (NoSuchMethodException ex) {
                                ex.printStackTrace();
                            } catch (IllegalAccessException ex) {
                                ex.printStackTrace();
                            } catch (InvocationTargetException ex) {
                                ex.printStackTrace();

                            } catch (InstantiationException ex) {
                                ex.printStackTrace();

                            }
                            //}

                            if (module == null) continue;
                            DirectivePair pair = module.parseDirectives(line);
                            if (pair != null)
                                currentMapping.addRulesPair(pair);

                        }

                    }

                    if (currentMapping != null) {
                        config.rulesMappings.add(currentMapping);
                    }

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return config;
        }

        public List<RulesMapping> getRuleMappings() {
            return this.rulesMappings;
        }

    }

    /**
     * Class that maps the rules based on URL pattern, mime type pattern or user agent string pattern.
     */
    public static class RulesMapping implements IgnoreAcceptContext {

        private String ignoreURLPattern;

        private String acceptURLPattern;

        private String ignoreMIMEPattern;

        private String acceptMIMEPattern;

        private String ignoreUAPattern;

        private String acceptUAPattern;

        RulesMapping(String ignoreURLPattern, String acceptURLPattern, String ignoreMIMEPattern, String acceptMIMEPattern, String ignoreUAPattern, String acceptUAPattern) {
            this.ignoreURLPattern = ignoreURLPattern;
            this.acceptURLPattern = acceptURLPattern;
            this.ignoreMIMEPattern = ignoreMIMEPattern;
            this.acceptMIMEPattern = acceptMIMEPattern;
            this.ignoreUAPattern = ignoreUAPattern;
            this.acceptUAPattern = acceptUAPattern;
        }

        private final List<DirectivePair> rules = new ArrayList<DirectivePair>();

        private boolean isURLIgnored(String url) {
            return this.ignoreURLPattern != null && url != null && url.matches(ignoreURLPattern);
        }

        public boolean isURLAccepted(String url) {
            return !this.isURLIgnored(url) && (this.acceptURLPattern == null || (url != null && url.matches(acceptURLPattern)));
        }

        private boolean isMIMEIgnored(String mimeType) {
            return this.ignoreMIMEPattern != null && mimeType != null && mimeType.matches(ignoreMIMEPattern);
        }

        public boolean isMIMEAccepted(String mimeType) {
            return !this.isMIMEIgnored(mimeType) && (this.acceptMIMEPattern == null || (mimeType != null && mimeType.matches(acceptMIMEPattern)));
        }

        private boolean isUserAgentIgnored(String userAgent) {
            return this.ignoreUAPattern != null && userAgent != null && userAgent.matches(ignoreUAPattern);
        }

        public boolean isUserAgentAccepted(String userAgent) {
            return !this.isUserAgentIgnored(userAgent) && (this.acceptUAPattern == null || (userAgent != null && userAgent.matches(acceptUAPattern)));
        }

        public List<DirectivePair> getRules() {
            return rules;
        }

        public void addRulesPair(DirectivePair pair) {
            rules.add(pair);
        }

        public void addRulesPairs(List<DirectivePair> pairs) {
            rules.addAll(pairs);
        }
    }

}

