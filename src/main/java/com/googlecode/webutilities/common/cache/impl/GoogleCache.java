/*
 * Copyright 2010-2014 Rajendra Patil
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.webutilities.common.cache.impl;

import com.google.common.cache.CacheBuilder;
import com.googlecode.webutilities.common.cache.Cache;
import com.googlecode.webutilities.common.cache.CacheConfig;

import java.util.concurrent.TimeUnit;

/**
 * Cache implementation using Google Guava Cache
 */
public class GoogleCache<K, V> implements Cache<K, V> {

    private com.google.common.cache.Cache<K, V> googleCache;

    public GoogleCache(CacheConfig<K, V> cacheConfig) {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder().softValues();
        // if(reloadAfterAccess > 0)
        //     builder.expireAfterAccess(reloadAfterAccess, TimeUnit.SECONDS);
        if (cacheConfig.getReloadTime() > 0)
            builder.expireAfterWrite(cacheConfig.getReloadTime(), TimeUnit.SECONDS);
        googleCache = builder.build();
    }

    @Override
    public void put(K key, V value) {
        googleCache.put(key, value);
    }

    @Override
    public V get(K key) {
        return googleCache.getIfPresent(key);
    }

    @Override
    public void invalidate(K key) {
        googleCache.invalidate(key);
    }

    @Override
    public void invalidateAll() {
        googleCache.invalidateAll();
    }

    @Override
    public void shutdown() {

    }
}
