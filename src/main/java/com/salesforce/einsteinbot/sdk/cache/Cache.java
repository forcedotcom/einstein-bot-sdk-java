/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.cache;

import java.util.Optional;

/**
 * Cache interface provides an abstraction for plugging in a cache mechanism.
 * Cache is used by SessionManagedChatbotClient to manage the mapping
 * between client session id Einstein Bots session id.
 *
 * Cache is also used by JwtBearerOAuth to cache jwt tokens to avoid network requests to fetch tokens
 *
 * @author relango
 */
public interface Cache {

  /**
   * Get value associated with given key.
   * @param key
   * @return
   */
  Optional<String> get(String key);

  /**
   * Add or updates given key and value in Cache
   * @param key
   * @param val
   */
  void set(String key, String val);

  /**
   * Add or updates given key and value in Cache and expires the entry after given ttlSeconds
   * @param key
   * @param val
   * @param ttlSeconds
   */
  void set(String key, String val, long ttlSeconds);

  /**
   * Removes entry associated with given key from Cache.
   * @param key
   */
  void remove(String key);
}
