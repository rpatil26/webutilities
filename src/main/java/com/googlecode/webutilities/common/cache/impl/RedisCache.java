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

import com.googlecode.webutilities.common.cache.Cache;
import com.googlecode.webutilities.common.cache.CacheConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.*;

/**
 * Cache implemented using Redis Cache
 */
public class RedisCache<K, V> implements Cache<K, V> {

    private JedisPool jedisPool;

    CacheConfig<K, V> cacheConfig;

    public RedisCache(CacheConfig<K, V> config) {
        this.cacheConfig = config;
        this.getPool(config.getHostname(), config.getPortNumber());
    }

    private JedisPool getPool(String server, int port) {
        if (this.jedisPool == null) {
            this.jedisPool = new JedisPool(server, port);
        }
        return this.jedisPool;
    }

    private byte[] toBytes(Object value) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] result = new byte[0];
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(value);
            result = bos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            //log error
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return result;
    }

    public Object toObject(byte[] bytes) {
        if (bytes == null || bytes.length < 1) return null;

        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

        ObjectInput in = null;
        Object result = null;

        try {
            in = new ObjectInputStream(bis);
            result = in.readObject();
        } catch (IOException e) {
            //Log error
        } catch (ClassNotFoundException e) {
            //Log error
        } finally {
            try {
                bis.close();
            } catch (IOException ex) {
                // ignore close exception
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return result;
    }

    @Override
    public void put(K key, V value) {
        int reloadTime = cacheConfig.getReloadTime();
        Jedis jedis = this.jedisPool.getResource();
        try {
            if (reloadTime < 0) {
                jedis.setex(toBytes(key), reloadTime, toBytes(value));
            } else {
                jedis.set(toBytes(key), toBytes(value));
            }
        } finally {
            if (jedis != null) {
                this.jedisPool.returnResource(jedis);
            }
        }

    }

    @Override
    public V get(K key) {
        Jedis jedis = this.jedisPool.getResource();
        try {
            return (V) toObject(jedis.get(toBytes(key)));
        } finally {
            if (jedis != null) {
                this.jedisPool.returnResource(jedis);
            }
        }
    }

    @Override
    public void invalidate(K key) {
        Jedis jedis = this.jedisPool.getResource();
        try {
            jedis.del(toBytes(key));
        } finally {
            if (jedis != null) {
                this.jedisPool.returnResource(jedis);
            }
        }
    }

    @Override
    public void invalidateAll() {
        Jedis jedis = this.jedisPool.getResource();
        try {
            jedis.flushDB();
        } finally {
            if (jedis != null) {
                this.jedisPool.returnResource(jedis);
            }
        }
    }

    @Override
    public void cleanup() {
        this.jedisPool.close();
    }
}
