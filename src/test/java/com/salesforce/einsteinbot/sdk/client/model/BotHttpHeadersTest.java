/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for BotHttpHeaders
 *
 * @author relango
 */
public class BotHttpHeadersTest {

  @Test
  public void testHeaderValuesWriteAndRead() {
    String requestId = "requestId";
    String customHeaderName = "customName";
    String customHeaderValue1 = "customValue1";
    String customHeaderValue2 = "customValue2";

    BotHttpHeaders botHeader = BotHttpHeaders
        .with()
        .requestId(requestId)
        .header(customHeaderName, customHeaderValue1)
        .header(customHeaderName, customHeaderValue2)
        .build();

    //RuntimeCRC is not provided, so it should be present
    assertFalse(botHeader.getRuntimeCRCHeader().isPresent());

    assertTrue(botHeader.getRequestIdHeader().isPresent());
    assertEquals(requestId, botHeader.getRequestIdHeader().get());

    Map<String, Collection<String>> allHeaders = botHeader.getAll();
    assertTrue(allHeaders.containsKey(customHeaderName));

    //Verify we can store multiple values for same header.
    assertTrue(allHeaders.get(customHeaderName)
        .containsAll(Arrays.asList(customHeaderValue1, customHeaderValue2)));
  }
}
