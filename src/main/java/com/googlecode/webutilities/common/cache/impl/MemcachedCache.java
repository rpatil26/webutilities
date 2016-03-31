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

package com.googlecode.webutilities.common.cache.impl;

import com.googlecode.webutilities.common.cache.Cache;
import com.googlecode.webutilities.common.cache.CacheConfig;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;

import java.io.IOException;

/**
 * Cache implemented using Memcached Cache
 */
public class MemcachedCache<K, V> implements Cache<K, V> {

    private MemcachedClient client;

    private CacheConfig<K, V> cacheConfig;

    public MemcachedCache(CacheConfig<K, V> config) throws IOException {
        this.cacheConfig = config;
        String addr = config.getHostname() + ":" + config.getPortNumber();
        this.client = new MemcachedClient(new ConnectionFactoryBuilder().setDaemon(true).setFailureMode(FailureMode.Retry).build(), AddrUtil.getAddresses(addr));
    }

    @Override
    public void put(K key, V value) {
        int reloadTime = cacheConfig.getReloadTime();
        if (reloadTime > 0) {
            client.set(key.toString(), reloadTime, value);
        } else {
            client.set(key.toString(), 3600, value);
        }
    }

    @Override
    public V get(K key) {
        return (V) client.get(key.toString());
    }

    @Override
    public void invalidate(K key) {
        client.delete(key.toString());
    }

    @Override
    public void invalidateAll() {
        client.flush();
    }

    @Override
    public void cleanup() {
        client.flush();
        client.shutdown();
        client = null;
    }
}
