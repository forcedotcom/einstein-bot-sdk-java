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

    //RuntimeCRC is not provided, so it should not be present
    assertTrue(botHeader.getRuntimeCRCHeaderAsCSV().isEmpty());

    assertFalse(botHeader.getRequestIdHeaderAsCSV().isEmpty());
    assertEquals(requestId, String.join(",", botHeader.getRequestIdHeaderAsCSV()));

    Map<String, Collection<String>> allHeaders = botHeader.getAll();
    assertTrue(botHeader.containsHeader(customHeaderName));

    //Verify we can store multiple values for same header.
    assertTrue(botHeader.get(customHeaderName)
        .containsAll(Arrays.asList(customHeaderValue1, customHeaderValue2)));

    //Verify reading header value in case insensitive way
    assertTrue(botHeader.get(customHeaderName.toLowerCase())
        .containsAll(Arrays.asList(customHeaderValue1, customHeaderValue2)));
  }
}
