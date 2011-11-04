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

package com.googlecode.webutilities.filters;

import com.googlecode.webutilities.common.Constants;
import com.googlecode.webutilities.filters.common.AbstractFilter;
import com.googlecode.webutilities.filters.common.IgnoreAcceptContext;
import com.googlecode.webutilities.modules.infra.*;
import com.googlecode.webutilities.util.Utils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModulesFilter extends AbstractFilter {

    public static final String DEFAULT_MODULES_PACKAGE = "com.googlecode.webutilities.modules";

    private Config config = null;

    public static final Logger LOGGER = LoggerFactory.getLogger(ModulesFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);

        String configFile = filterConfig.getInitParameter("configFile");
        if (configFile != null && !configFile.trim().equals("")) {
            try {
                config = Config.load(new FileInputStream(configFile));
            } catch (FileNotFoundException ex) {
                LOGGER.error("Specified file could not be loaded.{}", configFile);
                throw new ServletException("Could not load config file: " + configFile);
            }
            LOGGER.debug("Found config file in the classpath. {}", configFile);
        }

        if (config == null) {
            LOGGER.debug("Using default config file.");
            config = Config.load();
        }
    }

    private IRule.Status process(Iterator iterator, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain chain) throws IOException {

        if (iterator != null && iterator.hasNext()) {

            BaseModule module = (BaseModule) iterator.next();
            ModuleRequest moduleRequest = module.getRequest(httpServletRequest);
            ModuleResponse moduleResponse = module.getResponse(moduleRequest, httpServletResponse);

            LOGGER.debug("Processing Module: {}", module.getClass().getName());

            IRule.Status status = IRule.Status.CONTINUE;

            for (PreChainRule rule : module.activeModuleRules.getPreChainRules()) {
                status = rule.process(moduleRequest, moduleResponse);
                if (IRule.Status.CONTINUE != status) {
                    LOGGER.warn("PreChainRule {} returned false | no_chain", rule.getClass().getName());
                    break;
                }
            }
            if (IRule.Status.NO_CHAIN != status ){
                status = process(iterator, moduleRequest, moduleResponse, chain);
                if(status != IRule.Status.CONTINUE){
                    return status;
                }
            }
            for (PostChainRule rule : module.activeModuleRules.getPostChainRules()) {
                status = rule.process(moduleRequest, moduleResponse);
                if (status != IRule.Status.CONTINUE) {
                    LOGGER.warn("PostChainRule {} returned false.", rule.getClass().getName());
                    return status;
                }
            }
            moduleResponse.commit();
        } else {
            try {
                LOGGER.trace("Doing chaining, finally.");
                chain.doFilter(httpServletRequest, httpServletResponse);
            } catch (Exception ex) {
                ex.getCause().printStackTrace(httpServletResponse.getWriter());
                if (httpServletResponse instanceof ModuleResponse) {
                    ((ModuleResponse) httpServletResponse).commit();
                }
                LOGGER.error("Error in chaining.", ex);
                return IRule.Status.NO_CHAIN;
            }
        }

        return IRule.Status.CONTINUE;
    }

    private List<IModule> getAllEligibleModules(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String requestMime = httpRequest.getContentType();
        String userAgent = httpRequest.getHeader(Constants.HTTP_USER_AGENT_HEADER);
        String url = getURL(httpRequest);
        String responseMime = httpResponse.getContentType();
        if (requestMime == null) {
            responseMime = Utils.selectMimeByFile(url);
        }
        List<IModule> eligibleModule = null;
        for (RuleMapping ruleMapping : config.ruleMappings) {
            if (ruleMapping.isURLAccepted(url) && (ruleMapping.isMIMEAccepted(requestMime) || ruleMapping.isMIMEAccepted(responseMime)) && ruleMapping.isUserAgentAccepted(userAgent)) {
                if (eligibleModule == null) eligibleModule = new ArrayList<IModule>(); //lazy init
                for (IModule module : ruleMapping.getAllModules()) {
                    if (!eligibleModule.contains(module)) {
                        ((BaseModule) module).setContext(this.filterConfig.getServletContext());
                        eligibleModule.add(module);
                    }
                }

            }
        }
        LOGGER.debug("Found {} modules.", eligibleModule != null ? eligibleModule.size() : 0);
        return eligibleModule;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest moduleRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse moduleResponse = (HttpServletResponse) servletResponse;
        List<IModule> allEligibleModules = getAllEligibleModules(moduleRequest, moduleResponse);
        Iterator iterator = null;
        if (allEligibleModules != null && allEligibleModules.size() > 0) {
            iterator = allEligibleModules.iterator();
        }
        process(iterator, moduleRequest, moduleResponse, filterChain);

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
     *
     * Default modules.conf has default standard optimization rules for JS, CSS, Images. Refer default
     * modules.conf file for more details.
     */

    public static class Config {


        private final List<RuleMapping> ruleMappings = new ArrayList<RuleMapping>();

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
                    inputStream = ModulesFilter.class.getResourceAsStream("/modules.conf");
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

                RuleMapping currentMapping = null;

                try {
                    while ((line = bufferedReader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("#") || line.equals("")) {
                            continue;
                        }

                        if (line.startsWith("for")) {

                            if (currentMapping != null) {
                                config.ruleMappings.add(currentMapping);
                            }

                            acceptURLPattern = extractRegExFor(line, "acceptURL");
                            ignoreURLPattern = extractRegExFor(line, "ignoreURL");

                            acceptMIMEPattern = extractRegExFor(line, "acceptMIME");
                            ignoreMIMEPattern = extractRegExFor(line, "ignoreMIME");

                            acceptUAPattern = extractRegExFor(line, "acceptUA");
                            ignoreUAPattern = extractRegExFor(line, "ignoreUA");

                            currentMapping = new RuleMapping(ignoreURLPattern, acceptURLPattern, ignoreMIMEPattern, acceptMIMEPattern, ignoreUAPattern, acceptUAPattern);

                            continue;
                        }

                        if (currentMapping != null) {
                            String moduleName = line.split(" ")[0];
                            moduleName = DEFAULT_MODULES_PACKAGE + "." + moduleName;
                            IModule module = currentMapping.getModule(moduleName);
                            if (module == null) {
                                try {

                                    Class moduleClass = Class.forName(moduleName);

                                    Constructor constructor = moduleClass.getConstructor();

                                    module = (IModule) constructor.newInstance();

                                    currentMapping.addModule(moduleName, module);

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
                            }

                            if (module == null) continue;
                            module.parseConfigRules(line);
                        }

                    }

                    if (currentMapping != null) {
                        config.ruleMappings.add(currentMapping);
                    }

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return config;
        }

        public List<RuleMapping> getRuleMappings() {
            return this.ruleMappings;
        }

    }

    /**
     * Class that maps the rules based on URL pattern, mime type pattern or user agent string pattern.
     */
    public static class RuleMapping implements IgnoreAcceptContext {

        private String ignoreURLPattern;

        private String acceptURLPattern;

        private String ignoreMIMEPattern;

        private String acceptMIMEPattern;

        private String ignoreUAPattern;

        private String acceptUAPattern;

        RuleMapping(String ignoreURLPattern, String acceptURLPattern, String ignoreMIMEPattern, String acceptMIMEPattern, String ignoreUAPattern, String acceptUAPattern) {
            this.ignoreURLPattern = ignoreURLPattern;
            this.acceptURLPattern = acceptURLPattern;
            this.ignoreMIMEPattern = ignoreMIMEPattern;
            this.acceptMIMEPattern = acceptMIMEPattern;
            this.ignoreUAPattern = ignoreUAPattern;
            this.acceptUAPattern = acceptUAPattern;
        }

        private final Map<String, IModule> modules = new LinkedHashMap<String, IModule>();

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

        public IModule getModule(String name) {
            return modules.get(name);
        }

        public void addModule(String name, IModule module) {
            modules.put(name, module);
        }

        public Collection<IModule> getAllModules() {
            return modules.values();
        }

    }

}

