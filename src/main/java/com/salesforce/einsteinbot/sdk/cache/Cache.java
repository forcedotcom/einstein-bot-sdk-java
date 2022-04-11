/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.cache;

import java.util.Optional;

/**
 * Cache interface provides an abstraction for plugging in a cache mechanism for maintaining the
 * session cache. This interface is used by SessionManagedChatbotClientImpl to manage the mapping
 * between client session id Einstein Bots session id.
 */
public interface Cache {

  Optional<String> get(String key);

  void set(String key, String val);

  void set(String key, String val, long ttlSeconds);
}
