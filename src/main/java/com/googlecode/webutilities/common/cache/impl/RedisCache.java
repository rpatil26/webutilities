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

import java.io.*;

/**
 * Cache implemented using Redis Cache
 */
public class RedisCache<K, V> implements Cache<K, V> {

  private Jedis jedis;
  CacheConfig<K, V> cacheConfig;

  public RedisCache(CacheConfig<K, V> config) {
    this.cacheConfig = config;
    jedis = new Jedis(config.getHostname(), config.getPortNumber());
    jedis.connect();
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

  public Object toObject(byte [] bytes) {
    if (bytes==null || bytes.length < 1) return null;

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
    if(reloadTime < 0) {
      jedis.setex(toBytes(key), reloadTime, toBytes(value));
    } else {
      jedis.set(toBytes(key), toBytes(value));
    }
  }

  @Override
  public V get(K key) {
    return (V) toObject(jedis.get(toBytes(key)));
  }

  @Override
  public void invalidate(K key) {
      jedis.del(toBytes(key));
  }

  @Override
  public void invalidateAll() {
    jedis.flushDB();
  }
}
