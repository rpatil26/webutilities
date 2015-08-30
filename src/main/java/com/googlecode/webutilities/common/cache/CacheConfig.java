/*
 * Copyright 2010-2015 Rajendra Patil
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.webutilities.common.cache;

/**
 * Cache Config
 */
public class CacheConfig<K, V> {

    public static enum CacheProvider {DEFAULT, MEMCACHED, REDIS, COUCHBASE}

    private CacheProvider provider;

    private String hostname;

    private int portNumber;

    private int reloadTime;

    private int resetTime;

    public CacheConfig() {
        this.provider = CacheProvider.DEFAULT;
    }

    public CacheConfig(CacheProvider provider) {
        this.provider = provider;
    }

    public CacheConfig(CacheProvider provider, int reloadTime, int resetTime) {
        this.provider = provider;
        this.reloadTime = reloadTime;
        this.resetTime = resetTime;
    }

    public CacheConfig(CacheProvider provider, String hostname, int portNumber, int reloadTime, int resetTime) {
        this.provider = provider;
        this.hostname = hostname;
        this.portNumber = portNumber;
        this.reloadTime = reloadTime;
        this.resetTime = resetTime;
    }

    public CacheProvider getProvider() {
        return provider;
    }

    public void setProvider(CacheProvider provider) {
        this.provider = provider;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public int getReloadTime() {
        return reloadTime;
    }

    public void setReloadTime(int reloadTime) {
        this.reloadTime = reloadTime;
    }

    public int getResetTime() {
        return resetTime;
    }

    public void setResetTime(int resetTime) {
        this.resetTime = resetTime;
    }

    @Override
    public String toString() {
        return "CacheConfig{" +
                "provider=" + provider +
                ", hostname='" + hostname + '\'' +
                ", portNumber=" + portNumber +
                ", reloadTime=" + reloadTime +
                ", resetTime=" + resetTime +
                '}';
    }
}
