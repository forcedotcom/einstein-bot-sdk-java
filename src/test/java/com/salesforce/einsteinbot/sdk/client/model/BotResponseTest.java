/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.salesforce.einsteinbot.sdk.client.util.ResponseFactory;
import com.salesforce.einsteinbot.sdk.model.ChatMessageResponseEnvelope;
import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;
import com.salesforce.einsteinbot.sdk.util.TestUtils;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for BotResponse
 *
 * @author relango
 */
@ExtendWith(MockitoExtension.class)
public class BotResponseTest {

  @Mock
  private ResponseEnvelope responseEnvelope;

  @Mock
  private ChatMessageResponseEnvelope chatMessageResponseEnvelope;

  private final HttpStatus httpStatus = HttpStatus.OK;
  private final int httpStatusCode = httpStatus.value();
  private final String requestId = "TestRequestId";

  private BotHttpHeaders botHttpHeaders = BotHttpHeaders
      .with()
      .requestId(requestId)
      .build();

  @Test
  public void testBotResponseCreation() {
    BotResponse botResponse = BotResponse.with(responseEnvelope, httpStatusCode, botHttpHeaders);
    assertEquals(responseEnvelope, botResponse.getResponseEnvelope());
    assertEquals(httpStatusCode, botResponse.getHttpStatusCode());
    assertEquals(botHttpHeaders, botResponse.getHttpHeaders());

    Optional<String> actualRequestId = botResponse.getHttpHeaders().getRequestIdHeader();
    assertTrue(actualRequestId.isPresent());
    assertEquals(requestId, actualRequestId.get());
  }

  @Test
  public void testBotResponseCreationInvalidValue() {
    assertThrows(NullPointerException.class, () -> BotResponse
        .with(responseEnvelope, httpStatusCode, null));
  }

  @Test
  public void testBotResponseBuildFromResponseEnvelopeResponseEntity() {
    ResponseEntity<ResponseEnvelope> responseEntity = TestUtils
        .createResponseEntity(responseEnvelope, botHttpHeaders, httpStatus);

    BotResponse botResponse = BotResponseBuilder.fromResponseEnvelopeResponseEntity(responseEntity);
    assertEquals(responseEnvelope, botResponse.getResponseEnvelope());
    assertEquals(httpStatusCode, botResponse.getHttpStatusCode());
    assertEquals(botHttpHeaders, botResponse.getHttpHeaders());
  }

  @Test
  public void testBotResponseBuildFromChatMessageResponseEnvelopeResponseEntity() {
    ResponseEntity<ChatMessageResponseEnvelope> responseEntity = TestUtils
        .createResponseEntity(chatMessageResponseEnvelope, botHttpHeaders, httpStatus);

    String sessionId = "TestSessionId";
    BotResponse botResponse = BotResponseBuilder
        .fromChatMessageResponseEnvelopeResponseEntity(responseEntity, sessionId);
    ResponseEnvelope expectedResponseEnvelop = ResponseFactory
        .buildResponseEnvelope(sessionId, chatMessageResponseEnvelope);

    assertEquals(expectedResponseEnvelop, botResponse.getResponseEnvelope());
    assertEquals(httpStatusCode, botResponse.getHttpStatusCode());
    assertEquals(botHttpHeaders, botResponse.getHttpHeaders());
  }
}
