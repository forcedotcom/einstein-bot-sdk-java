/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import static com.salesforce.einsteinbot.sdk.util.UtilFunctions.addIntegrationTypeAndNameToContextVariables;

import com.google.common.base.Preconditions;
import com.salesforce.einsteinbot.sdk.cache.Cache;
import com.salesforce.einsteinbot.sdk.client.validators.IntegrationNameValidator;
import com.salesforce.einsteinbot.sdk.model.AnyRequestMessage;
import com.salesforce.einsteinbot.sdk.model.ChoiceMessage;
import com.salesforce.einsteinbot.sdk.model.EndSessionMessage;
import com.salesforce.einsteinbot.sdk.model.InitMessage;
import com.salesforce.einsteinbot.sdk.model.InitMessage.TypeEnum;
import com.salesforce.einsteinbot.sdk.model.RedirectMessage;
import com.salesforce.einsteinbot.sdk.model.RequestEnvelope;
import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;
import com.salesforce.einsteinbot.sdk.model.Status;
import com.salesforce.einsteinbot.sdk.model.TextMessage;
import com.salesforce.einsteinbot.sdk.model.TransferFailedRequestMessage;
import com.salesforce.einsteinbot.sdk.model.TransferSucceededRequestMessage;
import java.util.Objects;
import java.util.Optional;

/**
 * This implementation of {@link ChatbotClient} includes Session Management.
 * <p>
 * SessionId will be set on outgoing messages based on the externalSessionId provided as part of the
 * RequestEnvelope. SequenceId will also get set automatically.
 */
public class SessionManagedChatbotClient implements ChatbotClient {

  private ChatbotClient basicClient;
  private Cache cache;
  private Optional<String> integrationName;

  private SessionManagedChatbotClient(ChatbotClient chatbotClient,
      Optional<String> integrationName,
      Cache cache) {

    basicClient = chatbotClient;
    this.cache = cache;
    this.integrationName = integrationName;
  }

  @Override
  public ResponseEnvelope sendChatbotRequest(RequestEnvelope requestEnvelope,
      RequestHeaders requestHeaders) {
    if (requestEnvelope.getSessionId() == null || requestEnvelope.getSessionId().trim()
        .equals("")) {
      setSessionIdOrAddInitMessage(requestEnvelope, requestHeaders);
    }
    addSequenceIds(requestEnvelope);
    ResponseEnvelope response = basicClient.sendChatbotRequest(requestEnvelope, requestHeaders);
    cacheSessionId(requestEnvelope.getExternalSessionKey(), response, requestHeaders.getOrgId());

    return response;
  }

  @Override
  public Status getHealthStatus() {
    return basicClient.getHealthStatus();
  }

  private void addSequenceIds(RequestEnvelope requestEnvelope) {
    Long sequenceId = System.currentTimeMillis();
    for (AnyRequestMessage message : requestEnvelope.getMessages()) {
      if (message instanceof ChoiceMessage) {
        ChoiceMessage choiceMessage = (ChoiceMessage) message;
        choiceMessage.setSequenceId(sequenceId);
      } else if (message instanceof TextMessage) {
        TextMessage textMessage = (TextMessage) message;
        textMessage.setSequenceId(sequenceId);
      } else if (message instanceof InitMessage) {
        InitMessage initMessage = (InitMessage) message;
        initMessage.setSequenceId(sequenceId);
      } else if (message instanceof EndSessionMessage) {
        EndSessionMessage endSessionMessage = (EndSessionMessage) message;
        endSessionMessage.setSequenceId(sequenceId);
      } else if (message instanceof RedirectMessage) {
        RedirectMessage redirectMessage = (RedirectMessage) message;
        redirectMessage.setSequenceId(sequenceId);
      } else if (message instanceof TransferSucceededRequestMessage) {
        TransferSucceededRequestMessage transferMessage = (TransferSucceededRequestMessage) message;
        transferMessage.setSequenceId(sequenceId);
      } else if (message instanceof TransferFailedRequestMessage) {
        TransferFailedRequestMessage transferMessage = (TransferFailedRequestMessage) message;
        transferMessage.setSequenceId(sequenceId);
      } else {
        throw new IllegalArgumentException("Invalid message type: " + message.getClass());
      }
      sequenceId++;
    }
  }

  private void setSessionIdOrAddInitMessage(RequestEnvelope requestEnvelope,
      RequestHeaders requestHeaders) {
    Optional<String> sessionId = cache.get(getCacheKey(requestEnvelope, requestHeaders));
    if (sessionId.isPresent()) {
      requestEnvelope.setSessionId(sessionId.get());
    } else {
      addInitMessage(requestEnvelope);
    }
  }

  private void addInitMessage(RequestEnvelope requestEnvelope) {
    Preconditions.checkArgument(
        requestEnvelope.getMessages() != null && !requestEnvelope.getMessages().isEmpty(),
        "No messages found in requestEnvelope");

    AnyRequestMessage firstMessage = requestEnvelope.getMessages().get(0);

    InitMessage initMessage;
    if (firstMessage instanceof InitMessage) {
      initMessage = (InitMessage) firstMessage;
    } else if (firstMessage instanceof TextMessage) {
      TextMessage firstTextMessage = (TextMessage) firstMessage;
      initMessage = new InitMessage()
          .text(firstTextMessage.getText())
          .type(TypeEnum.INIT);
    } else {
      throw new IllegalArgumentException(
          "Message needs to be of type TextMessage to create a new session. But received : "
              + firstMessage.getClass());
    }

    initMessage.setVariables(
        addIntegrationTypeAndNameToContextVariables(initMessage.getVariables(), integrationName));

    //replace first message with init message
    requestEnvelope.getMessages().set(0, initMessage);

  }

  private String getCacheKey(RequestEnvelope request, RequestHeaders requestHeaders) {
    return getCacheKey(request.getExternalSessionKey(), requestHeaders.getOrgId(),
        request.getBotId());
  }

  private String getCacheKey(String externalSessionId, String orgId, String botId) {
    return String.format("chatbot-%s-%s-%s", orgId, botId, externalSessionId);
  }

  private void cacheSessionId(String externalSessionId, ResponseEnvelope responseEnvelope,
      String orgId) {
    cache.set(getCacheKey(externalSessionId, orgId, responseEnvelope.getBotId()),
        responseEnvelope.getSessionId());
  }

  public static BasicClientBuilder builder() {
    return new FluentBuilder();
  }

  /**
   * FluentBuilder provides Fluent API to create Session Managed Chatbot Client.
   */
  public static class FluentBuilder implements BasicClientBuilder,
      CacheBuilder, Builder {

    private ChatbotClient basicClient;
    private Optional<String> integrationName = Optional.empty();
    private Cache cache;

    FluentBuilder() {
    }

    public FluentBuilder basicClient(ChatbotClient basicClient) {
      this.basicClient = basicClient;
      return this;
    }

    public FluentBuilder integrationName(String integrationName) {
      IntegrationNameValidator.validateIntegrationName(integrationName);
      this.integrationName = Optional.ofNullable(integrationName);
      return this;
    }

    public FluentBuilder cache(Cache cache) {
      this.cache = cache;
      return this;
    }

    public SessionManagedChatbotClient build() {
      String errorMessageTemplate = "Please provide non-null value for %s ";
      Objects.requireNonNull(basicClient, () -> String.format(errorMessageTemplate, "basicClient"));
      Objects.requireNonNull(cache, () -> String.format(errorMessageTemplate, "cache"));
      return new SessionManagedChatbotClient(this.basicClient, this.integrationName, this.cache);
    }

  }

  public interface BasicClientBuilder {

    SessionManagedChatbotClient.CacheBuilder basicClient(ChatbotClient basicClient);
  }

  public interface CacheBuilder {

    SessionManagedChatbotClient.Builder cache(Cache cache);
  }

  public interface Builder {

    SessionManagedChatbotClient.Builder integrationName(String integrationName);
    SessionManagedChatbotClient build();
  }

}
