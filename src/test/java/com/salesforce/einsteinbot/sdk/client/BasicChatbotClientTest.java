/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import static com.salesforce.einsteinbot.sdk.client.model.BotResponseBuilder.fromResponseEnvelopeResponseEntity;
import static com.salesforce.einsteinbot.sdk.client.util.RequestFactory.buildBotSendMessageRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.salesforce.einsteinbot.sdk.api.BotApi;
import com.salesforce.einsteinbot.sdk.api.HealthApi;
import com.salesforce.einsteinbot.sdk.auth.AuthMechanism;
import com.salesforce.einsteinbot.sdk.client.model.BotEndSessionRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotHttpHeaders;
import com.salesforce.einsteinbot.sdk.client.model.BotRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotResponse;
import com.salesforce.einsteinbot.sdk.client.model.BotSendMessageRequest;
import com.salesforce.einsteinbot.sdk.client.model.ExternalSessionId;
import com.salesforce.einsteinbot.sdk.client.model.RequestConfig;
import com.salesforce.einsteinbot.sdk.client.model.RuntimeSessionId;
import com.salesforce.einsteinbot.sdk.client.util.RequestEnvelopeInterceptor;
import com.salesforce.einsteinbot.sdk.model.AnyRequestMessage;
import com.salesforce.einsteinbot.sdk.model.AnyResponseMessage;
import com.salesforce.einsteinbot.sdk.model.AnyVariable;
import com.salesforce.einsteinbot.sdk.model.ChatMessageEnvelope;
import com.salesforce.einsteinbot.sdk.model.ChatMessageResponseEnvelope;
import com.salesforce.einsteinbot.sdk.model.ChoiceMessage;
import com.salesforce.einsteinbot.sdk.model.EndSessionReason;
import com.salesforce.einsteinbot.sdk.model.ForceConfig;
import com.salesforce.einsteinbot.sdk.model.InitMessageEnvelope;
import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;
import com.salesforce.einsteinbot.sdk.model.Status;
import com.salesforce.einsteinbot.sdk.model.TextInitMessage;
import com.salesforce.einsteinbot.sdk.model.TextMessage;
import com.salesforce.einsteinbot.sdk.model.TextMessage.TypeEnum;
import com.salesforce.einsteinbot.sdk.util.TestUtils;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

/**
 * BasicChatbotClientTest - Unit Tests for BasicChatbotClient
 *
 * @author relango
 */
@ExtendWith(MockitoExtension.class)
public class BasicChatbotClientTest {

  private final String authToken = "C2C TOKEN";
  private final String orgId = "00DSB0000001ThY2AU";
  private final String botId = "testBotId";
  private final String forceConfigEndpoint = "testForceConfigEndpoint";
  private final String sessionId = "testSessionId";
  private final String externalSessionId = "testExternalSessionIdId";
  private final String basePath = "http://runtime-api-na-west.stg.chatbots.sfdc.sh";
  private final String botVersion = "TestBotVersion";
  private final String requestId = "TestRequestId";
  private final String runtimeCRC = null;

  private final RequestConfig config = RequestConfig
      .with()
      .botId(botId)
      .orgId(orgId)
      .forceConfigEndpoint(forceConfigEndpoint)
      .build();

  private final long sequenceId = System.currentTimeMillis();
  private final String messageText = "hello";
  private final HttpStatus httpStatus = HttpStatus.OK;
  private final String responseRequestId = "ResponseRequestId";

  private final BotHttpHeaders httpHeaders = BotHttpHeaders.with()
      .requestId(responseRequestId)
      .build();

  private final AnyRequestMessage message = new TextMessage()
      .type(TypeEnum.TEXT)
      .sequenceId(sequenceId)
      .text(messageText);

  private final TextInitMessage initMessage = new TextInitMessage()
      .text(messageText);

  private BasicChatbotClient client;

  @Mock
  private BotApi mockBotApi;

  @Mock
  private HealthApi mockHealthApi;

  @Mock
  private AnyResponseMessage responseMessage;

  @Mock
  private AnyVariable variable;

  @Mock
  private Object metrics;

  @Mock
  private Status healthStatus;

  @Mock
  private RequestEnvelopeInterceptor requestEnvelopeInterceptor;

  @Captor
  private ArgumentCaptor<Object> requestEnvelopeInterceptorArgCaptor;

  @Mock
  private AuthMechanism mockAuthMechanism;
  private EndSessionReason endSessionReason = EndSessionReason.USERREQUEST;

  @BeforeEach
  public void setup() {
    lenient().when(mockAuthMechanism.getToken()).thenReturn(authToken);
    client = ChatbotClients.basic()
        .basePath(basePath)
        .authMechanism(mockAuthMechanism)
        .build();

    ((BasicChatbotClientImpl) client).setBotApi(mockBotApi);
  }

  @Test
  public void testStartSession() {
    ResponseEntity<ResponseEnvelope> responseEntity = TestUtils
        .createResponseEntity(buildResponseEnvelope(), httpHeaders, httpStatus);
    BotResponse startSessionBotResponse = fromResponseEnvelopeResponseEntity(responseEntity);

    InitMessageEnvelope initMessageEnvelope = buildInitMessageEnvelope();

    when(mockBotApi.establishChatSessionWithHttpInfo(eq(botId), eq(orgId), eq(requestId),
        eq(initMessageEnvelope)))
        .thenReturn(createMonoApiResponse(responseEntity));

    BotResponse response = client.startChatSession(config, new ExternalSessionId(externalSessionId),
        buildBotSendMessageRequest(message, Optional.of(requestId)));

    verifyResponseHeaders(response.getHttpHeaders());
    assertEquals(httpStatus.value(), response.getHttpStatusCode());
    assertEquals(startSessionBotResponse, response);
  }

  @Test
  public void testStartSessionWithInvalidFirstMessageType() {

    AnyRequestMessage invalidFirstMessageType = buildChoiceMessage();

    Throwable exception = assertThrows(IllegalArgumentException.class, () ->
        client.startChatSession(config, new ExternalSessionId(externalSessionId),
            buildBotSendMessageRequest(invalidFirstMessageType, Optional.of(requestId))));

    assertTrue(exception.getMessage()
        .contains("Message needs to be of type TextMessage to create a new session"));
  }

  @Test
  public void testSendMessage() {

    ChatMessageResponseEnvelope sendMessageResponseEnvelope = buildChatMessageResponseEnvelope();
    ResponseEntity<ChatMessageResponseEnvelope> responseEntity = TestUtils
        .createResponseEntity(sendMessageResponseEnvelope, httpHeaders, httpStatus);

    ChatMessageEnvelope chatMessageEnvelope = buildChatMessageEnvelope();

    when(mockBotApi.continueChatSessionWithHttpInfo(eq(sessionId), eq(orgId), eq(requestId),
        eq(chatMessageEnvelope), eq(runtimeCRC)))
        .thenReturn(createMonoApiResponse(responseEntity));

    BotResponse response = client.sendMessage(config, new RuntimeSessionId(sessionId),
        buildBotSendMessageRequest(message, Optional.of(requestId)));
    verifyResponse(sendMessageResponseEnvelope, response);
  }

  @Test
  public void testSendMessageWithRequestInterceptor() {

    ChatMessageResponseEnvelope sendMessageResponseEnvelope = buildChatMessageResponseEnvelope();
    ResponseEntity<ChatMessageResponseEnvelope> responseEntity = TestUtils
        .createResponseEntity(sendMessageResponseEnvelope, httpHeaders, httpStatus);

    ChatMessageEnvelope chatMessageEnvelope = buildChatMessageEnvelope();

    when(mockBotApi.continueChatSessionWithHttpInfo(eq(sessionId), eq(orgId), eq(requestId),
        eq(chatMessageEnvelope), eq(runtimeCRC)))
        .thenReturn(createMonoApiResponse(responseEntity));

    BotSendMessageRequest botSendMessageReq = BotRequest
        .withMessage(message)
        .requestId(requestId)
        .requestEnvelopeInterceptor(requestEnvelopeInterceptor)
        .build();
    BotResponse response = client
        .sendMessage(config, new RuntimeSessionId(sessionId), botSendMessageReq);
    verifyResponse(sendMessageResponseEnvelope, response);
    verifyRequestEnvelopInterceptorInvocation(chatMessageEnvelope);
  }

  private void verifyRequestEnvelopInterceptorInvocation(ChatMessageEnvelope chatMessageEnvelope) {
    verify(requestEnvelopeInterceptor).accept(requestEnvelopeInterceptorArgCaptor.capture());
    Object requestEnvelope = requestEnvelopeInterceptorArgCaptor.getValue();
    assertTrue(requestEnvelope instanceof ChatMessageEnvelope);
    assertEquals(chatMessageEnvelope, requestEnvelope);
  }

  @Test
  public void testEndSession() {

    ChatMessageResponseEnvelope endSessionResponseEnvelope = buildChatMessageResponseEnvelope();
    ResponseEntity<ChatMessageResponseEnvelope> responseEntity = TestUtils
        .createResponseEntity(endSessionResponseEnvelope, httpHeaders, httpStatus);

    when(mockBotApi
        .endChatSessionWithHttpInfo(eq(sessionId), eq(orgId), eq(endSessionReason), eq(requestId),
            eq(runtimeCRC)))
        .thenReturn(createMonoApiResponse(responseEntity));

    BotResponse response = client
        .endChatSession(config, new RuntimeSessionId(sessionId), buildEndSessionRequestEnvelope());
    verifyResponse(endSessionResponseEnvelope, response);
  }

  private ChoiceMessage buildChoiceMessage() {
    return new ChoiceMessage()
        .type(ChoiceMessage.TypeEnum.CHOICE)
        .choiceId("1").sequenceId(0L);
  }

  private <T> Mono<ResponseEntity<T>> createMonoApiResponse(ResponseEntity<T> responseEntity) {
    return Mono.fromCallable(() -> responseEntity);
  }

  private ChatMessageEnvelope buildChatMessageEnvelope() {
    return new ChatMessageEnvelope().message(message);
  }

  private InitMessageEnvelope buildInitMessageEnvelope() {
    return new InitMessageEnvelope()
        .forceConfig(
            new ForceConfig()
                .endpoint(forceConfigEndpoint)
        )
        .externalSessionKey(externalSessionId)
        .message(initMessage)
        .variables(Collections.emptyList());
  }

  private BotEndSessionRequest buildEndSessionRequestEnvelope() {
    return BotRequest
        .withEndSession(endSessionReason)
        .requestId(requestId)
        .build();
  }

  private ChatMessageResponseEnvelope buildChatMessageResponseEnvelope() {
    return new ChatMessageResponseEnvelope()
        .addProcessedSequenceIdsItem(System.currentTimeMillis())
        .addMessagesItem(responseMessage)
        .botVersion(botVersion)
        .addVariablesItem(variable)
        .metrics(metrics);
  }

  private ResponseEnvelope buildResponseEnvelope() {
    return new ResponseEnvelope()
        .sessionId(sessionId)
        .botVersion(botVersion)
        .addProcessedSequenceIdsItem(System.currentTimeMillis())
        .addMessagesItem(responseMessage)
        .addVariablesItem(variable)
        .metrics(metrics);
  }

  private void verifyResponse(ChatMessageResponseEnvelope expectedResponseEnvelope,
      BotResponse actualResponse) {
    verifyResponseHeaders(actualResponse.getHttpHeaders());
    assertEquals(httpStatus.value(), actualResponse.getHttpStatusCode());
    ResponseEnvelope actualResponseEnvelope = actualResponse.getResponseEnvelope();
    assertEquals(sessionId, actualResponseEnvelope.getSessionId());
    assertEquals(expectedResponseEnvelope.getProcessedSequenceIds(),
        actualResponseEnvelope.getProcessedSequenceIds());
    assertEquals(expectedResponseEnvelope.getMessages(), actualResponseEnvelope.getMessages());
    assertEquals(expectedResponseEnvelope.getProcessedSequenceIds(),
        actualResponseEnvelope.getProcessedSequenceIds());
    assertEquals(expectedResponseEnvelope.getBotVersion(), actualResponseEnvelope.getBotVersion());
    assertEquals(expectedResponseEnvelope.getVariables(), actualResponseEnvelope.getVariables());
    assertEquals(expectedResponseEnvelope.getMetrics(), actualResponseEnvelope.getMetrics());
  }

  private void verifyResponseHeaders(BotHttpHeaders actualHttpHeaders) {
    assertEquals(Optional.empty(), actualHttpHeaders.getRuntimeCRCHeader());
    assertEquals(Optional.ofNullable(responseRequestId), actualHttpHeaders.getRequestIdHeader());
  }

  @Test
  public void testGetHealthStatus() {
    Mono<Status> monoResponse = Mono.fromCallable(() -> healthStatus);

    when(mockHealthApi.statusGet()).thenReturn(monoResponse);

    BasicChatbotClient client = ChatbotClients.basic()
        .basePath(basePath)
        .authMechanism(mockAuthMechanism)
        .build();

    ((BasicChatbotClientImpl) client).setHealthApi(mockHealthApi);

    assertEquals(healthStatus, client.getHealthStatus());

  }
}
