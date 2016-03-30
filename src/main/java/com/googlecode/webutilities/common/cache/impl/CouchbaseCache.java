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

import com.couchbase.client.deps.io.netty.util.ReferenceCountUtil;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.SerializableDocument;
import com.googlecode.webutilities.common.cache.Cache;
import com.googlecode.webutilities.common.cache.CacheConfig;

import java.io.Serializable;

/**
 * Cache implemented using CouchbaseCache Cache
 */
public class CouchbaseCache<K, V> implements Cache<K, V> {

    private static final String BUCKET_NAME = "webutilities-response-cache";

    private Cluster cluster;

    private Bucket bucket;

    CacheConfig<K, V> cacheConfig;

    public CouchbaseCache(CacheConfig<K, V> config) {
        this.cacheConfig = config;
        this.cluster = CouchbaseCluster.create(config.getHostname().split("\\s+"));
        this.bucket = cluster.openBucket(BUCKET_NAME);
    }

    @Override
    public void put(K key, V value) {
        int reloadTime = cacheConfig.getReloadTime();
        bucket.upsert(SerializableDocument.create(key.toString(), reloadTime, (Serializable) value));
    }

    @Override
    public V get(K key) {
        SerializableDocument document = bucket.get(key.toString(), SerializableDocument.class);
        if (document != null) {
            V data = (V) document.content();
            ReferenceCountUtil.release(data);
            return data;
        }
        return null;
    }

    @Override
    public void invalidate(K key) {
        bucket.remove(key.toString());
    }

    @Override
    public void invalidateAll() {
        bucket.bucketManager().flush();
    }

    @Override
    public void cleanup() {
        this.bucket.close();
        this.cluster.disconnect();
    }
}
