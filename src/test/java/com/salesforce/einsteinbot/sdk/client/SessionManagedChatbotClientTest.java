/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.salesforce.einsteinbot.sdk.auth.AuthMechanism;
import com.salesforce.einsteinbot.sdk.cache.Cache;
import com.salesforce.einsteinbot.sdk.model.ChoiceMessage;
import com.salesforce.einsteinbot.sdk.model.InitMessage;
import com.salesforce.einsteinbot.sdk.model.RequestEnvelope;
import com.salesforce.einsteinbot.sdk.model.AnyRequestMessage;
import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;
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

@ExtendWith(MockitoExtension.class)
public class SessionManagedChatbotClientTest {

  @Mock
  private ChatbotClient chatbotClient;

  @Mock
  private AuthMechanism authMechanism;

  @Mock
  private Cache cache;

  @Captor
  private ArgumentCaptor<RequestEnvelope> requestCaptor;

  @Captor
  private ArgumentCaptor<RequestHeaders> headerCaptor;

  private final String basePath = "basePath";
  private final String integrationName = "integrationName";
  private final String orgId = "00DSB0000001ThY2AU";
  private final String botId = "botId";
  private final String externalSessionKey = "session1";
  private final String chatbotSessionId = "chatbotSessionId";

  private RequestEnvelope requestEnvelope;
  private ResponseEnvelope response;
  private RequestHeaders requestHeaders;
  //private ChatbotClient basicClient;
  private ChatbotClient sessionManagedClient;

  @BeforeEach
  public void setup() {
    response = new ResponseEnvelope();
    response.setOrgId(orgId);
    response.setBotId(botId);
    response.setSessionId(chatbotSessionId);

    requestEnvelope = new RequestEnvelope();
    requestEnvelope.setBotId(botId);
    requestEnvelope.setExternalSessionKey(externalSessionKey);
    requestHeaders = RequestHeaders.builder()
        .orgId(orgId)
        .build();

  /*  basicClient = BasicChatbotClient.builder()
        .basePath(basePath)
        .authMechanism(authMechanism).build();*/

    sessionManagedClient = SessionManagedChatbotClient.builder()
        .basicClient(chatbotClient)
        .integrationName(integrationName)
        .cache(cache)
        .build();
  }

  @Test
  public void send_withoutSessionIdInCache() {
    addTextMessageToRequest();
    doReturn(Optional.empty()).when(cache)
        .get(String.format("chatbot-%s-%s-%s", orgId, botId, externalSessionKey));

    doReturn(response).when(chatbotClient).sendChatbotRequest(any(), any());

    // call method being tested
    sessionManagedClient.sendChatbotRequest(requestEnvelope, requestHeaders);

    // verify init message is sent
    verify(chatbotClient).sendChatbotRequest(requestCaptor.capture(), headerCaptor.capture());

    RequestEnvelope sentRequest = requestCaptor.getValue();
    verifySentRequestAndHeader(sentRequest, headerCaptor.getValue(), InitMessage.class);

    TextVariable integrationType = new TextVariable();
    integrationType.setName("$Context.IntegrationType");
    integrationType.setType(TextVariable.TypeEnum.TEXT);
    integrationType.setValue("API");

    TextVariable integrationNameVar = new TextVariable();
    integrationNameVar.setName("$Context.IntegrationName");
    integrationNameVar.setType(TextVariable.TypeEnum.TEXT);
    integrationNameVar.setValue(integrationName);

    assertThat(((InitMessage) sentRequest.getMessages().get(0)).getVariables(),
        contains(integrationType, integrationNameVar));

    // verify cache is updated
    verify(cache)
        .set(String.format("chatbot-%s-%s-%s", orgId, botId, externalSessionKey), chatbotSessionId);
  }

  private <T> void verifySentRequestAndHeader(RequestEnvelope sentRequest,
      RequestHeaders sentHeader, Class<T> messageType) {

    assertEquals(sentRequest.getMessages().size(), 1, "There should only be one message sent.");
    assertTrue(messageType.isInstance(sentRequest.getMessages().get(0)),
        "Message should be of type " + messageType.getName());
    assertEquals(orgId, sentHeader.getOrgId(), "OrgId in Request Header is incorrect");

    assertEquals(getMessageText(sentRequest), "Hello", "Message should say Hello");
    assertTrue(getMessageSequenceId(sentRequest) > 0L, "SequenceID should have been updated");

  }

  private String getMessageText(RequestEnvelope sentRequest) {
    AnyRequestMessage message = sentRequest.getMessages().get(0);
    if (message instanceof InitMessage) {
      return ((InitMessage) message).getText();
    } else if (message instanceof TextMessage) {
      return ((TextMessage) message).getText();
    } else {
      throw new IllegalArgumentException(
          "Unsupported Request Message Type : " + message.getClass());
    }
  }

  private long getMessageSequenceId(RequestEnvelope sentRequest) {
    AnyRequestMessage message = sentRequest.getMessages().get(0);
    if (message instanceof InitMessage) {
      return ((InitMessage) message).getSequenceId();
    } else if (message instanceof TextMessage) {
      return ((TextMessage) message).getSequenceId();
    } else {
      throw new IllegalArgumentException(
          "Unsupported Request Message Type : " + message.getClass());
    }
  }

  @Test
  public void send_withSessionIdInCache() {
    addTextMessageToRequest();
    doReturn(Optional.of(chatbotSessionId)).when(cache)
        .get(String.format("chatbot-%s-%s-%s", orgId, botId, externalSessionKey));

    doReturn(response).when(chatbotClient).sendChatbotRequest(any(), any());

    // call method being tested
    sessionManagedClient.sendChatbotRequest(requestEnvelope, requestHeaders);

    // verify text message is sent as is (i.e. no init message)

    verify(chatbotClient).sendChatbotRequest(requestCaptor.capture(), headerCaptor.capture());

    RequestEnvelope sentRequest = requestCaptor.getValue();
    verifySentRequestAndHeader(sentRequest, headerCaptor.getValue(), TextMessage.class);

    // verify cache is updated
    verify(cache)
        .set(String.format("chatbot-%s-%s-%s", orgId, botId, externalSessionKey), chatbotSessionId);
  }

  @Test
  public void send_withSessionIdAlreadySetInRequest() {
    addTextMessageToRequest();
    requestEnvelope.setSessionId(chatbotSessionId);

    doReturn(response).when(chatbotClient).sendChatbotRequest(any(), any());

    // call method being tested
    sessionManagedClient.sendChatbotRequest(requestEnvelope, requestHeaders);

    // verify text message is sent as is (i.e. no init message)
    ArgumentCaptor<RequestEnvelope> requestCaptor = ArgumentCaptor.forClass(RequestEnvelope.class);
    verify(chatbotClient).sendChatbotRequest(requestCaptor.capture(), headerCaptor.capture());

    RequestEnvelope sentRequest = requestCaptor.getValue();
    verifySentRequestAndHeader(sentRequest, headerCaptor.getValue(), TextMessage.class);

    // verify cache was only updated, it was never checked since sessionId was passed in
    verify(cache)
        .set(String.format("chatbot-%s-%s-%s", orgId, botId, externalSessionKey), chatbotSessionId);
    verifyNoMoreInteractions(cache);
  }

  @Test
  public void send_requestWithoutAnyMessages() {
    doReturn(Optional.empty()).when(cache)
        .get(String.format("chatbot-%s-%s-%s", orgId, botId, externalSessionKey));

    // call method being tested
    assertThrows(IllegalArgumentException.class, () -> {
      sessionManagedClient.sendChatbotRequest(requestEnvelope, requestHeaders);
    });

  }

  @Test
  public void send_invalidFirstMessageTypeForNewSession() {
    addChoiceMessageToRequest();
    doReturn(Optional.empty()).when(cache)
        .get(String.format("chatbot-%s-%s-%s", orgId, botId, externalSessionKey));

    // call method being tested
    assertThrows(IllegalArgumentException.class, () -> {
      sessionManagedClient.sendChatbotRequest(requestEnvelope, requestHeaders);
    });
  }

  @Test
  public void invalidIntegrationNames() {
    assertThrows(NullPointerException.class, () -> {
      SessionManagedChatbotClient.builder()
          .basicClient(chatbotClient)
          .integrationName(null)
          .cache(cache)
          .build();
    });

    assertThrows(IllegalArgumentException.class, () -> {
      SessionManagedChatbotClient.builder()
          .basicClient(chatbotClient)
          .integrationName("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
              + "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz")
          .cache(cache)
          .build();
    });
  }

  /*@Test
  public void getHealthStatus() {
    Status healthStatus = new Status();

    doReturn(healthStatus).when(chatbotClient).getHealthStatus();

    SessionManagedChatbotClient cut = SessionManagedChatbotClient.builder()
        .basePath(basePath)
        .authMechanism(authMechanism)
        .integrationName(integrationName)
        .cache(cache)
        .build();
    cut.setChatbotClient(chatbotClient);

    assertEquals(healthStatus, cut.getHealthStatus());
  }*/

  private void addTextMessageToRequest() {
    TextMessage textMessage = new TextMessage();
    textMessage.setText("Hello");
    textMessage.setType(TextMessage.TypeEnum.TEXT);
    textMessage.setSequenceId(0L);

    requestEnvelope.getMessages().add(textMessage);
  }

  private void addChoiceMessageToRequest() {
    ChoiceMessage choiceMessage = new ChoiceMessage();
    choiceMessage.setType(ChoiceMessage.TypeEnum.CHOICE);
    choiceMessage.setChoiceId("1");
    choiceMessage.setSequenceId(0L);

    requestEnvelope.getMessages().add(choiceMessage);
  }
}
