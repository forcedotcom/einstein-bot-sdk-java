/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client.util;

import com.salesforce.einsteinbot.sdk.model.AnyRequestMessage;
import com.salesforce.einsteinbot.sdk.model.AnyVariable;
import com.salesforce.einsteinbot.sdk.client.model.BotRequest;
import com.salesforce.einsteinbot.sdk.model.ChatMessageEnvelope;
import com.salesforce.einsteinbot.sdk.model.EndSessionReason;
import com.salesforce.einsteinbot.sdk.client.model.BotEndSessionRequest;
import com.salesforce.einsteinbot.sdk.model.ForceConfig;
import com.salesforce.einsteinbot.sdk.model.InitMessageEnvelope;
import com.salesforce.einsteinbot.sdk.client.model.BotSendMessageRequest;
import com.salesforce.einsteinbot.sdk.model.TextInitMessage;
import com.salesforce.einsteinbot.sdk.model.TextMessage;
import java.util.List;
import java.util.Optional;

/**
 * RequestFactory - RequestFactory provides factor methods to create model classes needed to make Bot API Request
 * @author relango
 */
public class RequestFactory {
  public static InitMessageEnvelope buildInitMessageEnvelope(String externalSessionKey,
      String forceConfigEndPoint,
      TextInitMessage message,
      List<AnyVariable> variables) {
    return new InitMessageEnvelope()
        .externalSessionKey(externalSessionKey)
        .forceConfig(new ForceConfig().endpoint(forceConfigEndPoint))
        .message(message)
        .variables(variables);
  }

  public static InitMessageEnvelope buildInitMessageEnvelope(String externalSessionKey,
      String forceConfigEndPoint,
      AnyRequestMessage message,
      List<AnyVariable> variables){
    return buildInitMessageEnvelope(externalSessionKey, forceConfigEndPoint, buildInitMessage(message), variables);
  }

  //TODO document in user guide
  public static TextInitMessage buildInitMessage(AnyRequestMessage message){
    if (message instanceof TextMessage){
      return new TextInitMessage()
          .text(((TextMessage) message).getText());
    }else {
      throw new IllegalArgumentException(
          "Message needs to be of type TextMessage to create a new session. But received : "
              + message.getClass());
    }
  }

  public static AnyRequestMessage buildTextMessage(String msg) {
    return new TextMessage()
        .text(msg)
        .type(TextMessage.TypeEnum.TEXT)
        .sequenceId(System.currentTimeMillis());
  }

  public static ChatMessageEnvelope buildChatMessageEnvelope(AnyRequestMessage message){
    return new ChatMessageEnvelope().message(message);
  }

  public static BotSendMessageRequest buildBotSendMessageRequest(AnyRequestMessage message, Optional<String> requestId) {
    return BotRequest
        .withMessage(message)
        .requestId(requestId)
        .build();
  }

  public static BotEndSessionRequest buildSessionBotEndSessionRequest(
      EndSessionReason endSessionReason, Optional<String> requestId) {
    return BotRequest
        .withEndSession(endSessionReason)
        .requestId(requestId)
        .build();
  }
}

