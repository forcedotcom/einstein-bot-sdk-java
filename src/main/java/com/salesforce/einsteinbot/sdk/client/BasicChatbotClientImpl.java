/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import static com.salesforce.einsteinbot.sdk.client.model.BotResponseBuilder.fromChatMessageResponseEnvelopeResponseEntity;
import static com.salesforce.einsteinbot.sdk.client.util.RequestFactory.buildChatMessageEnvelope;
import static com.salesforce.einsteinbot.sdk.client.util.RequestFactory.buildInitMessageEnvelope;
import static com.salesforce.einsteinbot.sdk.util.WebClientUtil.createErrorResponseProcessor;
import static com.salesforce.einsteinbot.sdk.util.WebClientUtil.createFilter;
import static com.salesforce.einsteinbot.sdk.util.WebClientUtil.createLoggingRequestProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.salesforce.einsteinbot.sdk.api.BotApi;
import com.salesforce.einsteinbot.sdk.api.HealthApi;
import com.salesforce.einsteinbot.sdk.auth.AuthMechanism;
import com.salesforce.einsteinbot.sdk.client.model.BotEndSessionRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotResponse;
import com.salesforce.einsteinbot.sdk.client.model.BotResponseBuilder;
import com.salesforce.einsteinbot.sdk.client.model.BotSendMessageRequest;
import com.salesforce.einsteinbot.sdk.client.model.ExternalSessionId;
import com.salesforce.einsteinbot.sdk.client.model.RequestConfig;
import com.salesforce.einsteinbot.sdk.client.model.RuntimeSessionId;
import com.salesforce.einsteinbot.sdk.exception.ChatbotResponseException;
import com.salesforce.einsteinbot.sdk.handler.ApiClient;
import com.salesforce.einsteinbot.sdk.model.ChatMessageEnvelope;
import com.salesforce.einsteinbot.sdk.model.EndSessionReason;
import com.salesforce.einsteinbot.sdk.model.Error;
import com.salesforce.einsteinbot.sdk.model.InitMessageEnvelope;
import com.salesforce.einsteinbot.sdk.model.Status;
import com.salesforce.einsteinbot.sdk.util.LoggingJsonEncoder;
import com.salesforce.einsteinbot.sdk.util.ReleaseInfo;
import com.salesforce.einsteinbot.sdk.util.UtilFunctions;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * This is a basic implementation of {@link BasicChatbotClient}. It does not perform session
 * management. So user has to explicitly call methods to start a chat session, send a message to
 * existing session and end chat session.
 *
 * @author relango
 */
public class BasicChatbotClientImpl implements BasicChatbotClient {

  protected BotApi botApi;
  protected HealthApi healthApi;
  protected ApiClient apiClient;
  protected AuthMechanism authMechanism;
  protected ReleaseInfo releaseInfo = ReleaseInfo.getInstance();

  protected BasicChatbotClientImpl(String basePath,
      AuthMechanism authMechanism,
      WebClient.Builder webClientBuilder) {

    this.authMechanism = authMechanism;
    this.apiClient = new ApiClient(createWebClient(webClientBuilder), UtilFunctions.getMapper(),
        UtilFunctions
            .createDefaultDateFormat());
    apiClient.setBasePath(basePath);
    apiClient.setUserAgent(releaseInfo.getAsUserAgent());
    botApi = new BotApi(apiClient);
    healthApi = new HealthApi(apiClient);
  }

  @VisibleForTesting
  void setBotApi(BotApi botApi) {
    this.botApi = botApi;
  }

  @VisibleForTesting
  void setHealthApi(HealthApi healthApi) {
    this.healthApi = healthApi;
  }

  @Override
  public BotResponse startChatSession(RequestConfig config,
      ExternalSessionId sessionId,
      BotSendMessageRequest botSendMessageRequest) {

    InitMessageEnvelope initMessageEnvelope = createInitMessageEnvelope(config, sessionId,
        botSendMessageRequest);

    notifyRequestEnvelopeInterceptor(botSendMessageRequest, initMessageEnvelope);
    CompletableFuture<BotResponse> futureResponse = invokeEstablishChatSession(config,
        initMessageEnvelope,
        botSendMessageRequest);
    try {
      return futureResponse.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  protected InitMessageEnvelope createInitMessageEnvelope(RequestConfig config,
      ExternalSessionId sessionId, BotSendMessageRequest botSendMessageRequest) {
    return buildInitMessageEnvelope(sessionId.getValue(),
        config.getForceConfigEndpoint(),
        botSendMessageRequest.getMessage(),
        botSendMessageRequest);
  }

  @Override
  public BotResponse sendMessage(RequestConfig config,
      RuntimeSessionId sessionId,
      BotSendMessageRequest botSendMessageRequest) {

    ChatMessageEnvelope chatMessageEnvelope = createChatMessageEnvelope(botSendMessageRequest);

    notifyRequestEnvelopeInterceptor(botSendMessageRequest, chatMessageEnvelope);
    CompletableFuture<BotResponse> futureResponse = invokeContinueChatSession(config.getOrgId(),
        sessionId.getValue(),
        chatMessageEnvelope,
        botSendMessageRequest);

    try {
      return futureResponse.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  protected ChatMessageEnvelope createChatMessageEnvelope(BotSendMessageRequest botSendMessageRequest) {
    return buildChatMessageEnvelope(
        botSendMessageRequest.getMessage(),
        botSendMessageRequest.getResponseOptions());
  }

  @Override
  public BotResponse endChatSession(RequestConfig config,
      RuntimeSessionId sessionId,
      BotEndSessionRequest botEndSessionRequest) {

    EndSessionReason endSessionReason = botEndSessionRequest.getEndSessionReason();
    notifyRequestEnvelopeInterceptor(botEndSessionRequest, "EndSessionReason: " + endSessionReason);
    CompletableFuture<BotResponse> futureResponse = invokeEndChatSession(config.getOrgId(),
        sessionId.getValue(),
        endSessionReason,
        botEndSessionRequest);
    try {
      return futureResponse.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private void notifyRequestEnvelopeInterceptor(BotRequest botRequest, Object requestEnvelope) {
    botRequest.getRequestEnvelopeInterceptor()
        .accept(requestEnvelope);
  }

  protected CompletableFuture<BotResponse> invokeEndChatSession(String orgId, String sessionId,
      EndSessionReason endSessionReason, BotEndSessionRequest botRequest) {

    apiClient.setBearerToken(authMechanism.getToken());
    CompletableFuture<BotResponse> futureResponse = botApi
        .endChatSessionWithHttpInfo(sessionId,
            orgId,
            endSessionReason,
            botRequest.getOrCreateRequestId(),
            botRequest.getRuntimeCRC().orElse(null))
        .toFuture()
        .thenApply(responseEntity -> fromChatMessageResponseEnvelopeResponseEntity(responseEntity,
            sessionId));

    return futureResponse;
  }

  protected CompletableFuture<BotResponse> invokeEstablishChatSession(RequestConfig config,
      InitMessageEnvelope initMessageEnvelope,
      BotSendMessageRequest botRequest) {

    apiClient.setBearerToken(authMechanism.getToken());
    CompletableFuture<BotResponse> futureResponse = botApi
        .establishChatSessionWithHttpInfo(config.getBotId(), config.getOrgId(),
            botRequest.getOrCreateRequestId(), initMessageEnvelope)
        .toFuture()
        .thenApply(BotResponseBuilder::fromResponseEnvelopeResponseEntity);

    return futureResponse;
  }

  protected CompletableFuture<BotResponse> invokeContinueChatSession(String orgId, String sessionId,
      ChatMessageEnvelope messageEnvelope,
      BotSendMessageRequest botRequest) {

    apiClient.setBearerToken(authMechanism.getToken());
    CompletableFuture<BotResponse> futureResponse = botApi
        .continueChatSessionWithHttpInfo(sessionId,
            orgId,
            botRequest.getOrCreateRequestId(),
            messageEnvelope,
            botRequest.getRuntimeCRC().orElse(null))
        .toFuture()
        .thenApply(responseEntity -> fromChatMessageResponseEnvelopeResponseEntity(responseEntity,
            sessionId));

    return futureResponse;
  }

  public Status getHealthStatus() {
    CompletableFuture<Status> statusFuture = healthApi.statusGet().toFuture();

    try {
      return statusFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private WebClient createWebClient(WebClient.Builder webClientBuilder) {

    return webClientBuilder
        .codecs(createCodecsConfiguration(UtilFunctions.getMapper()))
        .filter(createFilter(clientRequest -> createLoggingRequestProcessor(clientRequest),
            clientResponse -> createErrorResponseProcessor(clientResponse, this::mapErrorResponse)))
        .build();
  }

  private Consumer<ClientCodecConfigurer> createCodecsConfiguration(ObjectMapper mapper) {
    return clientDefaultCodecsConfigurer -> {
      //isEnabled for LoggingJsonEncoder should be false if we don't want to Request Body for GDPR compliance.  TODO: Make this configurable in future
      clientDefaultCodecsConfigurer.defaultCodecs()
          .jackson2JsonEncoder(new LoggingJsonEncoder(mapper, MediaType.APPLICATION_JSON, true));
      clientDefaultCodecsConfigurer.defaultCodecs()
          .jackson2JsonDecoder(new Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON));
    };
  }

  private Mono<ClientResponse> mapErrorResponse(ClientResponse clientResponse) {
    return clientResponse
        .bodyToMono(Error.class)
        .flatMap(errorDetails -> Mono
            .error(new ChatbotResponseException(clientResponse.statusCode(), errorDetails)));
  }
}
