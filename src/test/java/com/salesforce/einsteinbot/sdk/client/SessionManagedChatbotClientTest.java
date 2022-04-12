/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import static com.salesforce.einsteinbot.sdk.client.util.RequestFactory.buildBotSendMessageRequest;
import static com.salesforce.einsteinbot.sdk.client.util.RequestFactory.buildSessionBotEndSessionRequest;
import static com.salesforce.einsteinbot.sdk.client.util.RequestFactory.buildTextMessage;
import static com.salesforce.einsteinbot.sdk.client.model.BotResponseBuilder.fromResponseEnvelopeResponseEntity;
import static com.salesforce.einsteinbot.sdk.util.Constants.CONTEXT_VARIABLE_NAME_INTEGRATION_NAME;
import static com.salesforce.einsteinbot.sdk.util.Constants.CONTEXT_VARIABLE_NAME_INTEGRATION_TYPE;
import static com.salesforce.einsteinbot.sdk.util.Constants.CONTEXT_VARIABLE_VALUE_API;
import static com.salesforce.einsteinbot.sdk.util.TestUtils.createResponseEntity;
import static com.salesforce.einsteinbot.sdk.util.UtilFunctions.createTextVariable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.salesforce.einsteinbot.sdk.cache.Cache;
import com.salesforce.einsteinbot.sdk.client.model.BotEndSessionRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotHttpHeaders;
import com.salesforce.einsteinbot.sdk.client.model.BotResponse;
import com.salesforce.einsteinbot.sdk.client.model.BotSendMessageRequest;
import com.salesforce.einsteinbot.sdk.model.EndSessionReason;
import com.salesforce.einsteinbot.sdk.client.model.ExternalSessionId;
import com.salesforce.einsteinbot.sdk.client.model.RequestConfig;
import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;
import com.salesforce.einsteinbot.sdk.client.model.RuntimeSessionId;
import com.salesforce.einsteinbot.sdk.model.TextMessage;
import com.salesforce.einsteinbot.sdk.model.TextVariable;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for SessionManagedChatbotClient
 *
 * @author relango
 */
@ExtendWith(MockitoExtension.class)
public class SessionManagedChatbotClientTest {

  public static final String messageText = "hello";
  @Mock
  private BasicChatbotClient basicChatbotClient;

  @Mock
  private Cache cache;

  @Captor
  private ArgumentCaptor<BotSendMessageRequest> messageRequestCaptor;

  @Captor
  private ArgumentCaptor<BotEndSessionRequest> endSessionRequestCaptor;

  @Captor
  private ArgumentCaptor<RequestConfig> configCaptor;

  @Captor
  private ArgumentCaptor<ExternalSessionId> externalSessionIdCaptor;

  @Captor
  private ArgumentCaptor<RuntimeSessionId> runtimeSessionIdCaptor;


  private final String integrationName = "integrationName";
  private final String orgId = "00DSB0000001ThY2AU";
  private final String botId = "botId";
  private final String externalSessionKey = "session1";
  private final String chatbotSessionId = "chatbotSessionId";
  private final RuntimeSessionId runtimeSessionId = new RuntimeSessionId(chatbotSessionId);
  private final ExternalSessionId externalSessionId = new ExternalSessionId(externalSessionKey);
  private final String forceConfigEndpoint = "testForceConfig";
  private final EndSessionReason endSessionReason = EndSessionReason.USERREQUEST;
  private final HttpStatus httpStatus = HttpStatus.OK;
  private final BotHttpHeaders httpHeaders = BotHttpHeaders.with().build();

  private BotResponse response;
  private RequestConfig requestConfig;
  private SessionManagedChatbotClient sessionManagedClient;
  private BotSendMessageRequest botSendMessageRequest;
  private BotEndSessionRequest botEndSessionRequest;


  @BeforeEach
  public void setup() {
    response = fromResponseEnvelopeResponseEntity(createResponseEntity(new ResponseEnvelope()
        .sessionId(chatbotSessionId), httpHeaders, httpStatus));

    requestConfig = RequestConfig
        .with()
        .botId(botId)
        .orgId(orgId)
        .forceConfigEndpoint(forceConfigEndpoint)
        .build();

    sessionManagedClient = ChatbotClients.sessionManaged()
        .basicClient(basicChatbotClient)
        .cache(cache)
        .integrationName(integrationName)
        .build();

    botSendMessageRequest = buildBotSendMessageRequest(buildTextMessage(messageText), Optional.empty());

    botEndSessionRequest = buildSessionBotEndSessionRequest(endSessionReason, Optional.empty());
  }

  @Test
  public void testSendMessageWithoutSessionIdInCache() {

    stubCacheToReturnEmptyOrExistingSession(Optional.empty());
    stubStartChatSession();

    // call method being tested
    sessionManagedClient.sendMessage(requestConfig, externalSessionId, botSendMessageRequest);

    // verify startChatSession is called
    verify(basicChatbotClient)
        .startChatSession(configCaptor.capture(), externalSessionIdCaptor.capture(), messageRequestCaptor.capture());

    assertEquals(requestConfig, configCaptor.getValue());
    assertEquals(externalSessionId, externalSessionIdCaptor.getValue());

    BotSendMessageRequest sentRequest = messageRequestCaptor.getValue();
    verifySentRequest(sentRequest, TextMessage.class);

    // verify cache is updated
    verify(cache)
        .set(String.format("chatbot-%s-%s-%s", orgId, botId, externalSessionKey), chatbotSessionId);

    TextVariable integrationType = createTextVariable(CONTEXT_VARIABLE_NAME_INTEGRATION_TYPE, CONTEXT_VARIABLE_VALUE_API);
    TextVariable integrationNameVar = createTextVariable(CONTEXT_VARIABLE_NAME_INTEGRATION_NAME, integrationName);

    assertThat(sentRequest.getVariables(),
        contains(integrationType, integrationNameVar));
  }

  @Test
  public void testSendMessageWithSessionIdInCache() {

    stubCacheToReturnEmptyOrExistingSession(Optional.of(chatbotSessionId));
    stubSendMessage();

    // call method being tested
    sessionManagedClient.sendMessage(requestConfig, externalSessionId, botSendMessageRequest);

    // verify sendMessage is invoked (i.e not startChatSession invoked)
    verify(basicChatbotClient).sendMessage(configCaptor.capture(), runtimeSessionIdCaptor.capture(), messageRequestCaptor.capture());

    verifyRequestConfigAndRuntimeSessionId();

    BotSendMessageRequest sentRequest = messageRequestCaptor.getValue();
    verifySentRequest(sentRequest, TextMessage.class);

    // verify cache is updated
    verify(cache)
        .set(String.format("chatbot-%s-%s-%s", orgId, botId, externalSessionKey), chatbotSessionId);
  }

  @Test
  public void testEndChatWithoutSessionIdInCache() {

    stubCacheToReturnEmptyOrExistingSession(Optional.empty());
    // call method being tested
    Throwable exception = assertThrows(IllegalStateException.class,
        () -> sessionManagedClient.endChatSession(requestConfig, externalSessionId,
            botEndSessionRequest)
    );

    assertTrue(exception.getMessage().contains("No session found"));
  }

  @Test
  public void testEndChatWithSessionIdInCache() {

    stubCacheToReturnEmptyOrExistingSession(Optional.of(chatbotSessionId));
    stubEndChat();

    // call method being tested
    sessionManagedClient.endChatSession(requestConfig, externalSessionId, botEndSessionRequest);

    // verify sendMessage is invoked (i.e not startChatSession invoked)
    verify(basicChatbotClient).endChatSession(configCaptor.capture(), runtimeSessionIdCaptor.capture(), endSessionRequestCaptor.capture());

    verifyRequestConfigAndRuntimeSessionId();

    BotEndSessionRequest sentRequest = endSessionRequestCaptor.getValue();
    assertEquals(endSessionReason, sentRequest.getEndSessionReason());
  }

  private void verifyRequestConfigAndRuntimeSessionId() {
    assertEquals(requestConfig, configCaptor.getValue());
    assertEquals(runtimeSessionId, runtimeSessionIdCaptor.getValue());
  }

  private void stubStartChatSession() {
    when(basicChatbotClient
        .startChatSession(any(RequestConfig.class), any(ExternalSessionId.class), any(
            BotSendMessageRequest.class)))
        .thenReturn(response);
  }

  private void stubSendMessage() {
    when(basicChatbotClient.sendMessage(any(RequestConfig.class), any(RuntimeSessionId.class), any(
        BotSendMessageRequest.class)))
        .thenReturn(response);
  }

  private void stubEndChat() {
    when(basicChatbotClient
        .endChatSession(
            any(RequestConfig.class),
            any(RuntimeSessionId.class),
            any(BotEndSessionRequest.class)))
        .thenReturn(response);
  }

  @Test
  public void invalidIntegrationNames() {
    assertNotNull(
        ChatbotClients.sessionManaged()
            .basicClient(basicChatbotClient)
            .cache(cache)
            .integrationName(null)
            .build()
    );

    assertThrows(IllegalArgumentException.class, () -> ChatbotClients.sessionManaged()
        .basicClient(basicChatbotClient)
        .cache(cache)
        .integrationName("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
            + "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz")
        .build());
  }

  private void stubCacheToReturnEmptyOrExistingSession(Optional<String> chatbotSessionId) {
    when(cache.get(String.format("chatbot-%s-%s-%s", orgId, botId, externalSessionKey)))
        .thenReturn(chatbotSessionId);
  }

  private <T> void verifySentRequest(BotSendMessageRequest sentRequest, Class<T> messageType) {

    assertTrue(messageType.isInstance(sentRequest.getMessage()),
        "Message should be of type " + messageType.getName());

    assertEquals(getMessageText(sentRequest), messageText, "Message should say " + messageText);
    assertTrue(getMessageSequenceId(sentRequest) > 0L, "SequenceID should have been updated");

  }

  private String getMessageText(BotSendMessageRequest sentRequest) {
    return ((TextMessage) sentRequest.getMessage()).getText();
  }

  private long getMessageSequenceId(BotSendMessageRequest sentRequest) {
    return ((TextMessage) sentRequest.getMessage()).getSequenceId();
  }
}