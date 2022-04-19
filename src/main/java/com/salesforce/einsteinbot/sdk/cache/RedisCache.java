/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.cache;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * RedisCache is an implementation of {@link Cache} that uses Redis as its backing cache. Its
 * default ttl is 259,140 seconds (which is 1 minute short of 3 days). This number was chosen to be
 * just under how long Einstein Bots cache sessions for.
 */
public class RedisCache implements Cache {

  private static final Long DEFAULT_TTL_SECONDS = 259140L; // 2 days, 23 hours, 59 minutes

  private JedisPool jedisPool;
  private long ttlSeconds;

  /**
   * This constructor will use the default ttl of 259,140 seconds and will assume standard Redis
   * configuration(i.e. running on local machine on default port).
   */
  public RedisCache() {
    this(DEFAULT_TTL_SECONDS, "redis://127.0.0.1:6379");
  }

  /**
   * @param ttlSeconds - Cache expiry time. The recommended value here is 259,140 seconds.
   * @param redisUrl   - Url of Redis (i.e. redis://127.0.0.1:6379)
   */
  public RedisCache(Long ttlSeconds, String redisUrl) {
    this.ttlSeconds = ttlSeconds;

    URI uri;
    try {
      uri = new URI(redisUrl);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    this.jedisPool = new JedisPool(new JedisPoolConfig(), uri);
  }

  @VisibleForTesting
  void setJedisPool(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }

  @Override
  public Optional<String> get(String key) {
    try (Jedis jedis = this.jedisPool.getResource()) {
      return Optional.ofNullable(jedis.get(key));
    }
  }

  @Override
  public void set(String key, String val) {
    try (Jedis jedis = this.jedisPool.getResource()) {
      jedis.setex(key, ttlSeconds, val);
    }
  }

  @Override
  public void set(String key, String val, long ttlInSeconds) {
    try (Jedis jedis = this.jedisPool.getResource()) {
      jedis.setex(key, ttlInSeconds, val);
    }
  }

  @Override
  public void remove(String key){
    try (Jedis jedis = this.jedisPool.getResource()) {
      jedis.del(key);
    }
  }
}
