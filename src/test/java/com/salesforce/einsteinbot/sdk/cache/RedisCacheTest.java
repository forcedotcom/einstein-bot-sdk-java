/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Unit tests for RedisCache
 *
 * @author relango
 */
@ExtendWith(MockitoExtension.class)
public class RedisCacheTest {

  private final String redisUrl = "redis://127.0.0.1:6379";

  @Mock
  private JedisPool jedisPool;

  @Mock
  private Jedis jedis;

  @BeforeEach
  public void setup() {
    when(jedisPool.getResource()).thenReturn(jedis);
  }

  @Test
  public void set() {
    long ttl = 5L;
    String key = "key";
    String value = "value";

    RedisCache cut = new RedisCache(ttl, redisUrl);
    cut.setJedisPool(jedisPool);
    cut.set(key, value);

    verify(jedis).setex(key, ttl, value);
  }

  @Test
  public void setWithTtl() {
    long ttl = 5L;
    String key = "key";
    String value = "value";

    long entryTtl = 10L;
    RedisCache cut = new RedisCache(ttl, redisUrl);
    cut.setJedisPool(jedisPool);
    cut.set(key, value, entryTtl);

    verify(jedis).setex(key, entryTtl, value);
  }

  @Test
  public void get_cacheHit() {
    long ttl = 5L;
    String key = "key";
    String value = "value";

    doReturn(value).when(jedis).get(key);

    RedisCache cut = new RedisCache(ttl, redisUrl);
    cut.setJedisPool(jedisPool);
    cut.get(key);

    assertEquals(value, cut.get(key).get());
  }

  @Test
  public void get_cacheMiss() {
    long ttl = 5L;
    String key = "key";

    RedisCache cut = new RedisCache(ttl, redisUrl);
    cut.setJedisPool(jedisPool);
    cut.get(key);

    assertEquals(Optional.empty(), cut.get(key));
  }
}
