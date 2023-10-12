/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client.model;

import com.salesforce.einsteinbot.sdk.client.util.RequestEnvelopeInterceptor;
import com.salesforce.einsteinbot.sdk.model.AnyRequestMessage;
import com.salesforce.einsteinbot.sdk.model.AnyVariable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * BotSendMessageRequest - BotSendMessageRequest implementation of {@link BotRequest} that is used
 * to send a message to Bot.
 *
 * @author relango
 */

public class BotSendMessageRequest extends BotRequest {

  private List<AnyVariable> variables;
  private AnyRequestMessage message;
  private Optional<String> tz;

  protected BotSendMessageRequest(Optional<String> requestId, Optional<String> runtimeCRC,
      RequestEnvelopeInterceptor requestEnvelopeInterceptor,
      List<AnyVariable> variables,
      AnyRequestMessage message,
      Optional<String> tz
  ) {
    super(requestId, runtimeCRC, requestEnvelopeInterceptor);
    Objects.requireNonNull(message);
    Objects.requireNonNull(variables);
    Objects.requireNonNull(tz);
    this.variables = variables;
    this.message = message;
    this.tz = tz;
  }

  public List<AnyVariable> getVariables() {
    return variables;
  }

  public AnyRequestMessage getMessage() {
    return message;
  }

  public Optional<String> getTz() {
    return tz;
  }

  public SendMessageRequestCloneBuilder<BotSendMessageRequest> clone(){
    return new FluentBuilder(this);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", BotSendMessageRequest.class.getSimpleName() + "[", "]")
        .add("variables=" + variables)
        .add("message=" + message)
        .add("tz=" + tz)
        .add(super.toString())
        .toString();
  }
}
