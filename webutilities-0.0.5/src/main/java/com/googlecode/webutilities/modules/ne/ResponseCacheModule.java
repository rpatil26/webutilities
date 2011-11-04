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
import com.googlecode.webutilities.modules.infra.ModuleRequest;
import com.googlecode.webutilities.modules.infra.ModuleResponse;
import com.googlecode.webutilities.util.Utils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.googlecode.webutilities.util.Utils.findResourcesToMerge;

public class ResponseCacheModule implements IModule {

    static long lastResetTime;

    static final Map<String, CacheObject> cache = new ConcurrentHashMap<String, CacheObject>();

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseCacheModule.class.getName());

    @Override
    public DirectivePair parseDirectives(String ruleString) {

        DirectivePair pair = null;

        int index = 0, resetTime = 0, reloadTime = 0;

        String[] tokens = ruleString.split("\\s+");

        assert tokens.length >= 1;

        if (!tokens[index++].equals(ResponseCacheModule.class.getSimpleName())) return pair;

        if ("resetTime".equals(tokens[index++])) {
            resetTime = Utils.readInt(tokens[index++], resetTime);
        }
        if ("reloadTime".equals(tokens[index++])) {
            reloadTime = Utils.readInt(tokens[index], reloadTime);
        }
        pair = new DirectivePair(new CheckCacheDirective(reloadTime, resetTime), new StoreCacheDirective(reloadTime, resetTime));
        return pair;
    }


    public static String getURL(HttpServletRequest request) {
        return Utils.removeFingerPrint(request.getRequestURI());
    }


}

class CheckCacheDirective implements PreChainDirective {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckCacheDirective.class.getName());

    private int reloadTime = 0;

    private int resetTime = 0;

    CheckCacheDirective(int reloadTime, int resetTime) {
        this.reloadTime = reloadTime;
        this.resetTime = resetTime;
    }

    @Override
    public int execute(ModuleRequest request, ModuleResponse response, ServletContext context) {

        long now = new Date().getTime();

        String url = ResponseCacheModule.getURL(request);

        CacheObject cacheObject = ResponseCacheModule.cache.get(url);

        boolean expireCache = request.getParameter(Constants.PARAM_EXPIRE_CACHE) != null ||
                (cacheObject != null && reloadTime > 0 && (now - cacheObject.getTime()) / 1000 > reloadTime);

        if (expireCache) {
            LOGGER.trace("Removing Cache for {} due to URL parameter.", url);
            ResponseCacheModule.cache.remove(url);
        }

        boolean resetCache = request.getParameter(Constants.PARAM_RESET_CACHE) != null ||
                resetTime > 0 && (now - ResponseCacheModule.lastResetTime) / 1000 > resetTime;

        if (resetCache) {
            LOGGER.trace("Resetting whole Cache for due to URL parameter.");
            ResponseCacheModule.cache.clear();
            ResponseCacheModule.lastResetTime = now;
        }

        boolean skipCache = request.getParameter(Constants.PARAM_DEBUG) != null || request.getParameter(Constants.PARAM_SKIP_CACHE) != null;

        if (skipCache) {
            LOGGER.trace("Skipping Cache for {} due to URL parameter.", url);
            return OK;
        }

        List<String> requestedResources = findResourcesToMerge(request.getContextPath(), url);
        //If-Modified-Since
        String ifModifiedSince = request.getHeader(Constants.HTTP_IF_MODIFIED_SINCE);
        if (ifModifiedSince != null) {
            Date date = Utils.readDateFromHeader(ifModifiedSince);
            if (date != null) {
                if (!Utils.isAnyResourceModifiedSince(requestedResources, date.getTime(), context)) {
                    //cache.remove(url);
                    this.sendNotModified(response);
                    return STOP_CHAIN;
                }
            }
        }
        //If-None-match
        String requestETag = request.getHeader(Constants.HTTP_IF_NONE_MATCH_HEADER);
        if (!Utils.isAnyResourceETagModified(requestedResources, requestETag, null, context)) {
            //cache.remove(url);
            this.sendNotModified(response);
            return STOP_CHAIN;
        }

        boolean cacheFound = false;

        if (cacheObject != null && cacheObject.getModuleResponse() != null) {
            if (requestedResources != null && Utils.isAnyResourceModifiedSince(requestedResources, cacheObject.getTime(), context)) {
                LOGGER.trace("Some resources have been modified since last cache: {}", url);
                ResponseCacheModule.cache.remove(url);
                cacheFound = false;
            } else {
                LOGGER.trace("Found valid cached response.");
                //cacheObject.increaseAccessCount();
                cacheFound = true;
            }
        }

        if (cacheFound) {
            LOGGER.debug("Returning Cached response.");
            try {
                cacheObject.getModuleResponse().fill(response);
                return STOP_CHAIN;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return OK;
    }


    private void sendNotModified(HttpServletResponse httpServletResponse) {
        httpServletResponse.setContentLength(0);
        httpServletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        LOGGER.trace("returning Not Modified (304)");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CheckCacheDirective that = (CheckCacheDirective) o;

        return reloadTime == that.reloadTime && resetTime == that.resetTime;

    }

    @Override
    public int hashCode() {
        int result = reloadTime;
        result = 31 * result + resetTime;
        return result;
    }
}

class StoreCacheDirective implements PostChainDirective {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoreCacheDirective.class.getName());

    private int reloadTime = 0;

    private int resetTime = 0;

    StoreCacheDirective(int reloadTime, int resetTime) {
        this.reloadTime = reloadTime;
        this.resetTime = resetTime;
    }

    @Override
    public int execute(ModuleRequest request, ModuleResponse response, ServletContext context) {

        long now = new Date().getTime();

        String url = ResponseCacheModule.getURL(request);

        CacheObject cacheObject = ResponseCacheModule.cache.get(url);

        boolean expireCache = request.getParameter(Constants.PARAM_EXPIRE_CACHE) != null ||
                (cacheObject != null && reloadTime > 0 && (now - cacheObject.getTime()) / 1000 > reloadTime);

        if (expireCache) {
            LOGGER.trace("Removing Cache for {} due to URL parameter.", url);
            ResponseCacheModule.cache.remove(url);
        }

        boolean resetCache = request.getParameter(Constants.PARAM_RESET_CACHE) != null ||
                resetTime > 0 && (now - ResponseCacheModule.lastResetTime) / 1000 > resetTime;

        if (resetCache) {
            LOGGER.trace("Resetting whole Cache for due to URL parameter.");
            ResponseCacheModule.cache.clear();
            ResponseCacheModule.lastResetTime = now;
        }

        boolean skipCache = request.getParameter(Constants.PARAM_DEBUG) != null || request.getParameter(Constants.PARAM_SKIP_CACHE) != null;

        if (!skipCache && !expireCache && !resetCache && response.getStatus() != HttpServletResponse.SC_NOT_MODIFIED) {
            List<String> requestedResources = findResourcesToMerge(request.getContextPath(), url);
            try {
                response.commit();
            } catch (IOException e) {
                LOGGER.warn("Response commit failed: ", e);
            }
            ResponseCacheModule.cache.put(url, new CacheObject(Utils.getLastModifiedFor(requestedResources, context), response));
            LOGGER.debug("Cache added for: {}", url);
        }

//            try {
//                response.commit();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

        return OK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StoreCacheDirective that = (StoreCacheDirective) o;

        return reloadTime == that.reloadTime && resetTime == that.resetTime;

    }

    @Override
    public int hashCode() {
        int result = reloadTime;
        result = 31 * result + resetTime;
        return result;
    }
}


class CacheObject {

    private long time;

    //private long accessCount = 0;

    private ModuleResponse moduleResponse;

    CacheObject(long time, ModuleResponse moduleResponse) {
        this.time = time;
        this.moduleResponse = moduleResponse;
    }

    public long getTime() {
        return time;
    }

    public ModuleResponse getModuleResponse() {
        return moduleResponse;
    }

    /*public void increaseAccessCount(){
        accessCount++;
    }

    public long getAccessCount(){
        return this.accessCount;
    }*/

}
