/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import static com.salesforce.einsteinbot.sdk.util.UtilFunctions.addIntegrationTypeAndNameToContextVariables;

import com.salesforce.einsteinbot.sdk.cache.Cache;
import com.salesforce.einsteinbot.sdk.client.model.BotEndSessionRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotResponse;
import com.salesforce.einsteinbot.sdk.client.model.BotSendMessageRequest;
import com.salesforce.einsteinbot.sdk.client.model.ExternalSessionId;
import com.salesforce.einsteinbot.sdk.client.model.RequestConfig;
import com.salesforce.einsteinbot.sdk.client.model.RuntimeSessionId;
import com.salesforce.einsteinbot.sdk.model.AnyVariable;
import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;
import com.salesforce.einsteinbot.sdk.model.Status;
import java.util.List;
import java.util.Optional;

/**
 * This implementation of {@link SessionManagedChatbotClient} includes Session Management.
 * <p>
 * It provides session management capabilities, caching Runtime's session ID based on an external
 * session ID. {@link com.salesforce.einsteinbot.sdk.cache.RedisCache} is supported out of the box.
 * There is an {@link com.salesforce.einsteinbot.sdk.cache.InMemoryCache} included that is meant for
 * testing purposes only. Additional caching methods can be used by implementing the {@link Cache}
 * interface.
 * <p>
 * SequenceId will also get set automatically.
 */
public class SessionManagedChatbotClientImpl implements SessionManagedChatbotClient {

  private BasicChatbotClient basicClient;
  private Cache cache;
  private Optional<String> integrationName;

  SessionManagedChatbotClientImpl(BasicChatbotClient basicChatbotClient,
      Optional<String> integrationName,
      Cache cache) {

    basicClient = basicChatbotClient;
    this.cache = cache;
    this.integrationName = integrationName;
  }

  @Override
  public BotResponse sendMessage(RequestConfig config, ExternalSessionId externalSessionId,
      BotSendMessageRequest botSendMessageRequest) {

    String externalSessionIdValue = externalSessionId.getValue();
    String botId = config.getBotId();
    String orgId = config.getOrgId();

    Optional<String> runtimeSessionIdOptional = cache
        .get(getCacheKey(orgId, botId, externalSessionIdValue));

    BotResponse botResponse;
    if (!runtimeSessionIdOptional.isPresent()) {
      botResponse = startNewChatSession(config, externalSessionId, botSendMessageRequest);
    } else {
      botResponse = continueExistingSession(config, botSendMessageRequest,
          runtimeSessionIdOptional);
    }

    //TODO: Also cache runtimeCRC and send it in subsequent request for both v5 and v4.
    cacheSessionId(botId, externalSessionIdValue, botResponse.getResponseEnvelope(), orgId);
    return botResponse;
  }

  private BotResponse continueExistingSession(RequestConfig config,
      BotSendMessageRequest botSendMessageRequest,
      Optional<String> runtimeSessionIdOptional) {

    addSequenceIds(botSendMessageRequest);
    String runtimeSessionId = runtimeSessionIdOptional.get();

    return basicClient
        .sendMessage(config, new RuntimeSessionId(runtimeSessionId), botSendMessageRequest);
  }

  private BotResponse startNewChatSession(RequestConfig config, ExternalSessionId externalSessionId,
      BotSendMessageRequest requestEnvelope) {

    requestEnvelope = updateContextVariables(requestEnvelope);
    addSequenceIds(requestEnvelope);

    return basicClient
        .startChatSession(config, externalSessionId, requestEnvelope);
  }

  @Override
  public BotResponse endChatSession(RequestConfig config, ExternalSessionId externalSessionId,
      BotEndSessionRequest botEndSessionRequest) {

    String cacheKey = getCacheKey(
        config.getOrgId(),
        config.getBotId(),
        externalSessionId.getValue());

    String sessionId = cache.get(cacheKey)
        .orElseThrow(() ->
            new IllegalStateException("No session found for given cacheKey : " + cacheKey));

    BotResponse botResonse = basicClient
        .endChatSession(config, new RuntimeSessionId(sessionId),
            botEndSessionRequest);

    removeFromCache(cacheKey);
    return botResonse;
  }

  private BotSendMessageRequest updateContextVariables(
      BotSendMessageRequest botSendMessageRequest) {
    List<AnyVariable> updatedVariables = addIntegrationTypeAndNameToContextVariables(
        botSendMessageRequest.getVariables(), integrationName);

    return botSendMessageRequest
        .clone()
        .setVariables(updatedVariables)
        .build();
  }

  private String getCacheKey(String orgId, String botId, String externalSessionId) {
    return String.format("chatbot-%s-%s-%s", orgId, botId, externalSessionId);
  }

  private void cacheSessionId(String botId, String externalSessionId,
      ResponseEnvelope responseEnvelope,
      String orgId) {
    cache.set(getCacheKey(orgId, botId, externalSessionId),
        responseEnvelope.getSessionId());
  }

  private void removeFromCache(String cacheKey) {
    cache.remove(cacheKey);
  }

  @Override
  public Status getHealthStatus() {
    return basicClient.getHealthStatus();
  }

  private void addSequenceIds(BotSendMessageRequest requestEnvelope) {
    Long sequenceId = System.currentTimeMillis();
    requestEnvelope.getMessage().setSequenceId(sequenceId);
  }
}
