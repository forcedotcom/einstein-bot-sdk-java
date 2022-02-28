/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.cache;

import com.google.common.cache.CacheBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * InMemoryCache is an implementation of {@link Cache} which caches entries in memory. It is
 * intended for testing and is not meant to be used in a distributed or production environment.
 */
public class InMemoryCache implements Cache {

  private final com.google.common.cache.Cache<String, String> cache;

  public InMemoryCache(long ttlSeconds) {
    cache = CacheBuilder.newBuilder().expireAfterAccess(ttlSeconds, TimeUnit.SECONDS).build();
  }

  @Override
  public Optional<String> get(String key) {
    String val = cache.getIfPresent(key);
    return Optional.ofNullable(val);
  }

  @Override
  public void set(String key, String val) {
    cache.put(key, val);
  }

  /**
   * This method does not respect the ttlSeconds parameter.
   */
  @Override
  public void set(String key, String val, long ttlSeconds) {
    cache.put(key, val);
  }
}
