/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client.model;

import com.salesforce.einsteinbot.sdk.model.EndSessionReason;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * BotEndSessionRequest - BotEndSessionRequest implementation of {@link BotRequest}
 * that is used to end a chat session.
 */
public class BotEndSessionRequest extends BotRequest {

  private EndSessionReason endSessionReason;

  BotEndSessionRequest( Optional<String> requestId, Optional<String> runtimeCRC,
      EndSessionReason endSessionReason) {
    super(requestId, runtimeCRC);
    Objects.requireNonNull(endSessionReason);
    this.endSessionReason = endSessionReason;
  }

  public EndSessionReason getEndSessionReason() {
    return endSessionReason;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", BotEndSessionRequest.class.getSimpleName() + "{",
        "}")
        .add("sessionEndReason=" + endSessionReason)
        .add(super.toString())
        .toString();
  }
}
