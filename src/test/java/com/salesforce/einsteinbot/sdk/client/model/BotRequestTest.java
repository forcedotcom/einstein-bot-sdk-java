/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.salesforce.einsteinbot.sdk.model.AnyRequestMessage;
import com.salesforce.einsteinbot.sdk.model.EndSessionReason;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for BotRequestTest
 *
 * @author relango
 */
@ExtendWith(MockitoExtension.class)
public class BotRequestTest {

  @Mock
  private AnyRequestMessage requestMessage;

  private EndSessionReason endSessionReason = EndSessionReason.ERROR;

  private String requestId = "TestRequestId";
  private String runtimeCRC = "TestRuntimeCRC";


  @Test
  public void testSendMessageRequest() {

    BotRequest botRequest = BotRequest
        .withMessage(requestMessage)
        .build();

    assertTrue(botRequest instanceof BotSendMessageRequest);
    assertEquals(requestMessage, ((BotSendMessageRequest) botRequest).getMessage());
  }

  @Test
  public void testEndSessionRequest() {

    BotRequest botRequest = BotRequest
        .withEndSession(endSessionReason)
        .build();

    assertTrue(botRequest instanceof BotEndSessionRequest);
    assertEquals(endSessionReason, ((BotEndSessionRequest) botRequest).getEndSessionReason());
  }

  @Test
  public void testOptionalFieldsWithValues() {

    BotRequest botRequest = BotRequest
        .withMessage(requestMessage)
        .requestId(requestId)
        .runtimeCRC(runtimeCRC)
        .build();

    assertTrue(botRequest instanceof BotSendMessageRequest);
    assertEquals(requestMessage, ((BotSendMessageRequest) botRequest).getMessage());
    assertEquals(Optional.of(requestId), botRequest.getRequestId());
    assertEquals(Optional.of(runtimeCRC), botRequest.getRuntimeCRC());
  }

  @Test
  public void testOptionalFieldsWithEmpty() {

    BotRequest botRequest = BotRequest
        .withEndSession(endSessionReason)
        .build();

    assertTrue(botRequest instanceof BotEndSessionRequest);
    assertEquals(endSessionReason, ((BotEndSessionRequest) botRequest).getEndSessionReason());
    assertFalse(botRequest.getRequestId().isPresent());
    assertFalse(botRequest.getRuntimeCRC().isPresent());
  }
}
