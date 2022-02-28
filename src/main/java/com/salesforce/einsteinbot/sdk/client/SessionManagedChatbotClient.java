/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import com.google.common.base.Preconditions;
import com.salesforce.einsteinbot.sdk.cache.Cache;
import com.salesforce.einsteinbot.sdk.client.validators.IntegrationNameValidator;
import com.salesforce.einsteinbot.sdk.model.AnyVariable;
import com.salesforce.einsteinbot.sdk.model.ChoiceMessage;
import com.salesforce.einsteinbot.sdk.model.EndSessionMessage;
import com.salesforce.einsteinbot.sdk.model.InitMessage;
import com.salesforce.einsteinbot.sdk.model.InitMessage.TypeEnum;
import com.salesforce.einsteinbot.sdk.model.RedirectMessage;
import com.salesforce.einsteinbot.sdk.model.RequestEnvelope;
import com.salesforce.einsteinbot.sdk.model.RequestEnvelopeMessagesOneOf;
import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;
import com.salesforce.einsteinbot.sdk.model.TextMessage;
import com.salesforce.einsteinbot.sdk.model.TextVariable;
import com.salesforce.einsteinbot.sdk.model.TransferFailedRequestMessage;
import com.salesforce.einsteinbot.sdk.model.TransferSucceededRequestMessage;
import java.util.ArrayList;
import java.util.List;
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
  private String integrationName;

  private SessionManagedChatbotClient(ChatbotClient chatbotClient,
      String integrationName,
      Cache cache) {
    IntegrationNameValidator.validateIntegrationName(integrationName);

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

  private void addSequenceIds(RequestEnvelope requestEnvelope) {
    Long sequenceId = System.currentTimeMillis();
    for (RequestEnvelopeMessagesOneOf message : requestEnvelope.getMessages()) {
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

    RequestEnvelopeMessagesOneOf firstMessage = requestEnvelope.getMessages().get(0);

    InitMessage initMessage;
    if (firstMessage instanceof InitMessage) {
      initMessage = (InitMessage) firstMessage;
    } else if (firstMessage instanceof TextMessage) {
      TextMessage firstTextMessage = (TextMessage) firstMessage;
      initMessage = new InitMessage();
      initMessage.setText(firstTextMessage.getText());
      initMessage.setType(TypeEnum.INIT);
    } else {
      throw new IllegalArgumentException(
          "Message needs to be of type TextMessage to create a new session.");
    }

    TextVariable integrationType = new TextVariable();
    integrationType.setName("$Context.IntegrationType");
    integrationType.setType(TextVariable.TypeEnum.TEXT);
    integrationType.setValue("API");

    TextVariable integrationNameVar = new TextVariable();
    integrationNameVar.setName("$Context.IntegrationName");
    integrationNameVar.setType(TextVariable.TypeEnum.TEXT);
    integrationNameVar.setValue(integrationName);

    List<AnyVariable> currentContextVariables = initMessage.getVariables();
    List<AnyVariable> contextVariables = currentContextVariables == null ? new ArrayList<>()
        : new ArrayList<>(currentContextVariables);
    contextVariables.add(integrationType);
    contextVariables.add(integrationNameVar);
    initMessage.setVariables(contextVariables);

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
  public static class FluentBuilder implements BasicClientBuilder, IntegrationNameBuilder,
      CacheBuilder, Builder {

    private ChatbotClient basicClient;
    private String integrationName;
    private Cache cache;

    FluentBuilder() {
    }

    public FluentBuilder basicClient(ChatbotClient basicClient) {
      this.basicClient = basicClient;
      return this;
    }

    public FluentBuilder integrationName(String integrationName) {
      this.integrationName = integrationName;
      return this;
    }

    public FluentBuilder cache(Cache cache) {
      this.cache = cache;
      return this;
    }

    public SessionManagedChatbotClient build() {
      String errorMessageTemplate = "Please provide non-null value for %s ";
      Objects.requireNonNull(basicClient, () -> String.format(errorMessageTemplate, "basicClient"));
      Objects.requireNonNull(integrationName,
          () -> String.format(errorMessageTemplate, "integrationName"));
      Objects.requireNonNull(cache, () -> String.format(errorMessageTemplate, "cache"));
      return new SessionManagedChatbotClient(this.basicClient, this.integrationName, this.cache);
    }

  }

  public interface BasicClientBuilder {

    SessionManagedChatbotClient.IntegrationNameBuilder basicClient(ChatbotClient basicClient);
  }

  public interface IntegrationNameBuilder {

    SessionManagedChatbotClient.CacheBuilder integrationName(String integrationName);
  }

  public interface CacheBuilder {

    SessionManagedChatbotClient.Builder cache(Cache cache);
  }

  public interface Builder {

    SessionManagedChatbotClient build();
  }

}
