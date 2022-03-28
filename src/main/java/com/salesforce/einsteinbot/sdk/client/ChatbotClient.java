/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import com.salesforce.einsteinbot.sdk.model.RequestEnvelope;
import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;
import com.salesforce.einsteinbot.sdk.model.Status;

/**
 * Base interface for Chatbot client. It provides a single method to send a chat request to Einstein
 * Bots and receive a response.
 */
public interface ChatbotClient {

  ResponseEnvelope sendChatbotRequest(RequestEnvelope requestEnvelope,
      RequestHeaders requestHeaders);

  Status getHealthStatus();
}
