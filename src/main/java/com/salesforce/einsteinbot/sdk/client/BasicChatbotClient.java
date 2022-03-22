/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import static com.salesforce.einsteinbot.sdk.util.WebClientUtil.createErrorResponseProcessor;
import static com.salesforce.einsteinbot.sdk.util.WebClientUtil.createFilter;
import static com.salesforce.einsteinbot.sdk.util.WebClientUtil.createLoggingRequestProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.salesforce.einsteinbot.sdk.api.MessagesApi;
import com.salesforce.einsteinbot.sdk.auth.AuthMechanism;
import com.salesforce.einsteinbot.sdk.exception.ChatbotResponseException;
import com.salesforce.einsteinbot.sdk.handler.ApiClient;
import com.salesforce.einsteinbot.sdk.model.Error;
import com.salesforce.einsteinbot.sdk.model.RequestEnvelope;
import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;
import com.salesforce.einsteinbot.sdk.util.LoggingJsonEncoder;
import com.salesforce.einsteinbot.sdk.util.ReleaseInfo;
import java.util.Objects;
import java.util.UUID;
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
 * This is a basic implementation of {@link ChatbotClient}. It does not perform session management.
 * Handling of sessionId and sequenceId will need to be done by the user.
 */
public class BasicChatbotClient implements ChatbotClient {

  protected MessagesApi messagesApi;
  protected ApiClient apiClient;
  protected AuthMechanism authMechanism;
  protected ReleaseInfo releaseInfo = ReleaseInfo.getInstance();

  protected BasicChatbotClient(String basePath,
      AuthMechanism authMechanism,
      WebClient.Builder webClientBuilder) {

    this.authMechanism = authMechanism;
    this.apiClient = new ApiClient(createWebClient(webClientBuilder), ClientBuilderUtil.getMapper(),
        ClientBuilderUtil
            .createDefaultDateFormat());
    apiClient.setBasePath(basePath);
    apiClient.setUserAgent(releaseInfo.getAsUserAgent());
    messagesApi = new MessagesApi(apiClient);
  }

  @VisibleForTesting
  void setMessagesApi(MessagesApi messagesApi) {
    this.messagesApi = messagesApi;
  }

  @Override
  public ResponseEnvelope sendChatbotRequest(RequestEnvelope requestEnvelope,
      RequestHeaders requestHeaders) {
    CompletableFuture<ResponseEnvelope> futureResponse = sendMessages(requestEnvelope,
        requestHeaders);
    try {
      return futureResponse.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  protected CompletableFuture<ResponseEnvelope> sendMessages(RequestEnvelope requestEnvelope,
      RequestHeaders requestHeaders) {
    apiClient.setBearerToken(authMechanism.getToken());
    CompletableFuture<ResponseEnvelope> futureResponse = messagesApi
        .sendMessages(requestHeaders.getOrgId(),
            requestHeaders.getRequestIdOr(UUID.randomUUID().toString()), requestEnvelope,
            requestHeaders.getRuntimeCRC()).toFuture();
    return futureResponse;
  }

  private WebClient createWebClient(WebClient.Builder webClientBuilder) {

    return webClientBuilder
        .codecs(createCodecsConfiguration(ClientBuilderUtil.getMapper()))
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

  public static BasePathBuilder builder() {
    return new FluentBuilder();
  }

  /**
   * FluentBuilder provides Fluent API to create Basic Chatbot Client.
   */
  public static class FluentBuilder implements BasePathBuilder, AuthMechanismBuilder, Builder {

    protected String basePath;
    protected AuthMechanism authMechanism;
    protected WebClient.Builder webClientBuilder = WebClient.builder();

    protected FluentBuilder() {
    }

    public FluentBuilder basePath(String basePath) {
      this.basePath = basePath;
      return this;
    }

    public FluentBuilder authMechanism(AuthMechanism authMechanism) {
      this.authMechanism = authMechanism;
      return this;
    }

    public FluentBuilder webClientBuilder(WebClient.Builder webClientBuilder) {
      this.webClientBuilder = webClientBuilder;
      return this;
    }

    public BasicChatbotClient build() {
      validate();
      return new BasicChatbotClient(this.basePath, this.authMechanism, this.webClientBuilder);
    }

    protected void validate() {
      String errorMessageTemplate = "Please provide non-null value for %s";
      Objects.requireNonNull(basePath, () -> String.format(errorMessageTemplate, "basePath"));
      Objects.requireNonNull(authMechanism,
          () -> String.format(errorMessageTemplate, "authMechanism"));
    }
  }

  public interface BasePathBuilder {

    AuthMechanismBuilder basePath(String basePath);
  }

  public interface AuthMechanismBuilder {

    Builder authMechanism(AuthMechanism authMechanism);
  }

  public interface Builder {

    Builder webClientBuilder(WebClient.Builder webClientBuilder);

    BasicChatbotClient build();
  }
}
