/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import com.salesforce.einsteinbot.sdk.client.model.BotEndSessionRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotResponse;
import com.salesforce.einsteinbot.sdk.client.model.BotSessionId;
import com.salesforce.einsteinbot.sdk.client.model.BotSendMessageRequest;
import com.salesforce.einsteinbot.sdk.client.model.RequestConfig;
import com.salesforce.einsteinbot.sdk.model.Status;

/**
 * Base interface for Chatbot client. It provides methods to interact with Bot and get Health status.
 */
public interface ChatbotClient<T extends BotSessionId> {

  BotResponse sendMessage(RequestConfig config,
      T sessionId,
      BotSendMessageRequest requestEnvelope);

  BotResponse endChatSession(RequestConfig config,
      T sessionId,
      BotEndSessionRequest requestEnvelope);

  Status getHealthStatus();
}
