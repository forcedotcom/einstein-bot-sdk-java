/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client.model;

import static com.salesforce.einsteinbot.sdk.client.model.BotHttpHeaders.fromSpringHttpHeaders;
import static com.salesforce.einsteinbot.sdk.client.util.ResponseFactory.buildResponseEnvelope;

import com.salesforce.einsteinbot.sdk.model.ChatMessageResponseEnvelope;
import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;
import org.springframework.http.ResponseEntity;

/**
 * BotResponseBuilder - BotResponseBuilder provides methods to build BotResponse from Spring's
 * Response Entity.
 *
 * @author relango
 */
public class BotResponseBuilder {

  public static BotResponse fromResponseEnvelopeResponseEntity(
      ResponseEntity<ResponseEnvelope> responseEntity) {
    responseEntity.getHeaders();
    return BotResponse.with(responseEntity.getBody(),
        responseEntity.getStatusCodeValue(),
        fromSpringHttpHeaders(responseEntity.getHeaders()));
  }

  public static BotResponse fromChatMessageResponseEnvelopeResponseEntity(
      ResponseEntity<ChatMessageResponseEnvelope> responseEntity, String sessionId) {
    ResponseEnvelope responseEnvelope = buildResponseEnvelope(sessionId, responseEntity.getBody());
    return BotResponse.with(responseEnvelope,
        responseEntity.getStatusCodeValue(),
        fromSpringHttpHeaders(responseEntity.getHeaders()));
  }
}
