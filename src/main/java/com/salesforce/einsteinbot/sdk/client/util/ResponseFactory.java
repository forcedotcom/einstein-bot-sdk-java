/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client.util;

import com.salesforce.einsteinbot.sdk.model.ChatMessageResponseEnvelope;
import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;

/**
 * ResponseFactory - ResponseFactory provides factor methods to create model classes used for Bots
 * Response.
 *
 * @author relango
 */
public class ResponseFactory {

  public static ResponseEnvelope buildResponseEnvelope(String sessionId,
      ChatMessageResponseEnvelope chatMessageResponseEnvelope) {
    return new ResponseEnvelope()
        .sessionId(sessionId)
        .processedSequenceIds(chatMessageResponseEnvelope.getProcessedSequenceIds())
        .messages(chatMessageResponseEnvelope.getMessages())
        .botVersion(chatMessageResponseEnvelope.getBotVersion())
        .variables(chatMessageResponseEnvelope.getVariables())
        .metrics(chatMessageResponseEnvelope.getMetrics());
  }
}
