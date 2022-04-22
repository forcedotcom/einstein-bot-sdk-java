/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client.model;

import java.util.Objects;

/**
 * ExternalSessionId - ExternalSessionId is a class to track client channel's external session id.
 *
 * <p>
 * An external session ID is the unique ID used to identify a session on the channel that is sending
 * messages to Einstein Bots Runtime. Using Slack as an example, the external session ID could be a
 * combination of Slack app ID, Slack user ID and Slack channel ID (if present).
 * </p>
 *
 * @author relango
 */
public class ExternalSessionId implements BotSessionId {

  private String value;

  public ExternalSessionId(String value) {
    Objects.requireNonNull(value);
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExternalSessionId that = (ExternalSessionId) o;
    return value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
