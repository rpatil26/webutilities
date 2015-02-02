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

package com.googlecode.webutilities.common.cache;

import com.googlecode.webutilities.common.cache.impl.GoogleCache;
import com.googlecode.webutilities.common.cache.impl.MemcachedCache;
import com.googlecode.webutilities.common.cache.impl.RedisCache;

import java.io.IOException;

/**
 * Cache factory to get the correct Cache based on input Configuration
 */
public class CacheFactory {

  public static <K, V> Cache getDefaultCache() {
    return new GoogleCache<K, V>(new CacheConfig<K, V>());
  }

  public static <K, V> Cache<K, V> getCache(CacheConfig<K, V> config) throws IOException {
    if (CacheConfig.CacheProvider.MEMCACHED.equals(config.getProvider())) {
      return new MemcachedCache<K, V>(config);
    } else if (CacheConfig.CacheProvider.REDIS.equals(config.getProvider())) {
      return new RedisCache<K, V>(config);
    } else {
      return new GoogleCache<K, V>(new CacheConfig<K, V>());
    }
  }
  public static boolean isCacheProvider (Cache cache, CacheConfig.CacheProvider provider) {
    return cache != null && provider != null && (CacheConfig.CacheProvider.MEMCACHED.equals(provider) && cache instanceof MemcachedCache
        || CacheConfig.CacheProvider.REDIS.equals(provider) && cache instanceof RedisCache
        || CacheConfig.CacheProvider.DEFAULT.equals(provider) && cache instanceof GoogleCache);
  }
}

