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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.salesforce.einsteinbot.sdk.api.BotApi;
import com.salesforce.einsteinbot.sdk.api.HealthApi;
import com.salesforce.einsteinbot.sdk.api.VersionsApi;
import com.salesforce.einsteinbot.sdk.auth.AuthMechanism;
import com.salesforce.einsteinbot.sdk.cache.InMemoryCache;
import com.salesforce.einsteinbot.sdk.client.model.BotEndSessionRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotResponse;
import com.salesforce.einsteinbot.sdk.client.model.BotResponseBuilder;
import com.salesforce.einsteinbot.sdk.client.model.BotSendMessageRequest;
import com.salesforce.einsteinbot.sdk.client.model.ExternalSessionId;
import com.salesforce.einsteinbot.sdk.client.model.RequestConfig;
import com.salesforce.einsteinbot.sdk.client.model.RuntimeSessionId;
import com.salesforce.einsteinbot.sdk.client.util.ClientFactory;
import com.salesforce.einsteinbot.sdk.client.util.ClientFactory.ClientWrapper;
import com.salesforce.einsteinbot.sdk.exception.UnsupportedSDKException;
import com.salesforce.einsteinbot.sdk.model.ChatMessageEnvelope;
import com.salesforce.einsteinbot.sdk.model.EndSessionReason;
import com.salesforce.einsteinbot.sdk.model.InitMessageEnvelope;
import com.salesforce.einsteinbot.sdk.model.Status;
import com.salesforce.einsteinbot.sdk.model.SupportedVersions;
import com.salesforce.einsteinbot.sdk.model.SupportedVersionsVersions;
import com.salesforce.einsteinbot.sdk.model.SupportedVersionsVersions.StatusEnum;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * This is a basic implementation of {@link BasicChatbotClient}. It does not perform session
 * management. So user has to explicitly call methods to start a chat session, send a message to
 * existing session and end chat session.
 *
 * @author relango
 */
public class BasicChatbotClientImpl implements BasicChatbotClient {

  private static final Long DEFAULT_TTL_SECONDS = Duration.ofDays(3).getSeconds();
  private static final ObjectMapper MAPPER = new ObjectMapper();
  static final String API_INFO_URI = "/services/data/v58.0/connect/bots/api-info";

  protected InMemoryCache cache;
  protected String basePath;
  protected WebClient.Builder webClientBuilder;
  protected AuthMechanism authMechanism;
  protected ClientWrapper clientWrapper;

  protected BasicChatbotClientImpl(String basePath,
      AuthMechanism authMechanism,
      WebClient.Builder webClientBuilder) {

    this.authMechanism = authMechanism;
    this.basePath = basePath;
    this.webClientBuilder = webClientBuilder;
    this.clientWrapper = ClientFactory.createClient(basePath, webClientBuilder);
    this.cache = new InMemoryCache(DEFAULT_TTL_SECONDS);
  }

  @VisibleForTesting
  void setBotApi(BotApi botApi) {
    this.clientWrapper.setBotApi(botApi);
  }

  @VisibleForTesting
  void setHealthApi(HealthApi healthApi) {
    this.clientWrapper.setHealthApi(healthApi);
  }

  @VisibleForTesting
  void setVersionsApi(VersionsApi versionsApi) {
    this.clientWrapper.setVersionsApi(versionsApi);
  }

  @VisibleForTesting
  void setCache(InMemoryCache cache) {
    this.cache = cache;
  }

  @Override
  public BotResponse startChatSession(RequestConfig config,
      ExternalSessionId sessionId,
      BotSendMessageRequest botSendMessageRequest) {

    if (!isApiVersionSupported(config)) {
      throw new UnsupportedSDKException(getCurrentApiVersion(), getLatestApiVersion(config));
    }

    ClientWrapper clientWrapper = getOrCreateClientWrapper(config);
    this.clientWrapper = clientWrapper;

    InitMessageEnvelope initMessageEnvelope = createInitMessageEnvelope(config, sessionId,
        botSendMessageRequest);

    notifyRequestEnvelopeInterceptor(botSendMessageRequest, initMessageEnvelope);
    CompletableFuture<BotResponse> futureResponse = invokeEstablishChatSession(config,
        initMessageEnvelope,
        botSendMessageRequest,
        clientWrapper);
    try {
      BotResponse botResponse = futureResponse.get();
      this.cache.set(botResponse.getResponseEnvelope().getSessionId(), basePath);
      this.cache.setObject(basePath, clientWrapper);
      return botResponse;
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

    ClientWrapper clientWrapper = getCachedClientWrapper(sessionId);
    notifyRequestEnvelopeInterceptor(botSendMessageRequest, chatMessageEnvelope);
    CompletableFuture<BotResponse> futureResponse = invokeContinueChatSession(config.getOrgId(),
        sessionId.getValue(),
        chatMessageEnvelope,
        botSendMessageRequest,
        clientWrapper);

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

    ClientWrapper clientWrapper = getCachedClientWrapper(sessionId);
    notifyRequestEnvelopeInterceptor(botEndSessionRequest, "EndSessionReason: " + endSessionReason);
    CompletableFuture<BotResponse> futureResponse = invokeEndChatSession(config.getOrgId(),
        sessionId.getValue(),
        endSessionReason,
        botEndSessionRequest,
        clientWrapper);
    try {
      return futureResponse.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  protected void notifyRequestEnvelopeInterceptor(BotRequest botRequest, Object requestEnvelope) {
    botRequest.getRequestEnvelopeInterceptor()
        .accept(requestEnvelope);
  }

  private ClientWrapper getCachedClientWrapper(RuntimeSessionId sessionId) {
    Optional<String> basePath = this.cache.get(sessionId.getValue());
    if (!basePath.isPresent()) {
      throw new RuntimeException("No base path found in cache for session ID: " + sessionId.getValue());
    }
    Optional<Object> clientOptional = this.cache.getObject(basePath.get());
    if (!clientOptional.isPresent()) {
      throw new RuntimeException("No client implementation found in cache for base path: " + basePath.get());
    }
    return (ClientWrapper) clientOptional.get();
  }

  protected CompletableFuture<BotResponse> invokeEndChatSession(String orgId, String sessionId,
      EndSessionReason endSessionReason, BotEndSessionRequest botRequest, ClientWrapper clientWrapper) {

    clientWrapper.getApiClient().setBearerToken(authMechanism.getToken());
    CompletableFuture<BotResponse> futureResponse = clientWrapper.getBotApi()
        .endSessionWithHttpInfo(sessionId,
            orgId,
            endSessionReason,
            botRequest.getRequestId().orElse(null),
            botRequest.getRuntimeCRC().orElse(null))
        .toFuture()
        .thenApply(responseEntity -> fromChatMessageResponseEnvelopeResponseEntity(responseEntity,
            sessionId));

    return futureResponse;
  }

  protected CompletableFuture<BotResponse> invokeEstablishChatSession(RequestConfig config,
      InitMessageEnvelope initMessageEnvelope,
      BotSendMessageRequest botRequest,
      ClientWrapper clientWrapper) {

    clientWrapper.getApiClient().setBearerToken(authMechanism.getToken());
    CompletableFuture<BotResponse> futureResponse = clientWrapper.getBotApi()
        .startSessionWithHttpInfo(config.getBotId(), config.getOrgId(),
            initMessageEnvelope, botRequest.getRequestId().orElse(null))
        .toFuture()
        .thenApply(BotResponseBuilder::fromResponseEnvelopeResponseEntity);

    return futureResponse;
  }

  protected CompletableFuture<BotResponse> invokeContinueChatSession(String orgId, String sessionId,
      ChatMessageEnvelope messageEnvelope,
      BotSendMessageRequest botRequest,
      ClientWrapper clientWrapper) {

    clientWrapper.getApiClient().setBearerToken(authMechanism.getToken());
    CompletableFuture<BotResponse> futureResponse = clientWrapper.getBotApi()
        .continueSessionWithHttpInfo(sessionId,
            orgId,
            messageEnvelope,
            botRequest.getRequestId().orElse(null),
            botRequest.getRuntimeCRC().orElse(null))
        .toFuture()
        .thenApply(responseEntity -> fromChatMessageResponseEnvelopeResponseEntity(responseEntity,
            sessionId));

    return futureResponse;
  }

  public Status getHealthStatus(RequestConfig requestConfig) {
    ClientWrapper clientWrapper = getOrCreateClientWrapper(requestConfig);
    CompletableFuture<Status> statusFuture = clientWrapper.getHealthApi().checkHealthStatus().toFuture();

    try {
      return statusFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public SupportedVersions getSupportedVersions(RequestConfig requestConfig) {
    ClientWrapper clientWrapper = getOrCreateClientWrapper(requestConfig);
    CompletableFuture<SupportedVersions> versionsFuture = clientWrapper.getVersionsApi().getAPIVersions().toFuture();

    try {
      SupportedVersions versions = versionsFuture.get();
      if (versions.getVersions().size() == 0) {
        throw new RuntimeException("Versions response was incorrect");
      }
      return versions;
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Error in getting versions response", e);
    }
  }

  private ClientWrapper getOrCreateClientWrapper(RequestConfig requestConfig) {
    String basePath = getRuntimeUrl(requestConfig.getForceConfigEndpoint());
    Optional<Object> clientOptional = this.cache.getObject(basePath);
    ClientWrapper clientWrapper = ClientFactory.createClient(basePath, webClientBuilder);
    if (clientOptional.isPresent()) {
      clientWrapper = (ClientWrapper) clientOptional.get();
    }
    return clientWrapper;
  }

  private String getRuntimeUrl(String forceEndpoint) {
    try {
      URI uri = URI.create(forceEndpoint);
      HttpHost forceHost = URIUtils.extractHost(uri);
      String infoPath = uri.getRawPath().replace("/$", "") + API_INFO_URI;
      WebClient webClient = WebClient.builder()
          .baseUrl(forceHost.toString())
          .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .build();
      JsonNode node = webClient.get()
          .uri(infoPath)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.authMechanism.getToken())
          .retrieve()
          .bodyToMono(JsonNode.class)
          .block();
      if (node == null) {
        throw new RuntimeException("Could not get runtime URL");
      }
      return node.get("runtimeBaseUrl").asText();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private String getCurrentApiVersion() {
    Properties properties;
    InputStream is = getClass().getClassLoader()
        .getResourceAsStream("properties-from-pom.properties");
    properties = new Properties();
    try {
      properties.load(is);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load pom properties file.", e);
    }

    // Replacement needed here as the same property is used to know the api version
    // (for example, 5.0.0), and for the api spec file naming convention (for example, v5_0_0).
    return properties.getProperty("api-spec-version").replace("_", ".");
  }

  private String getLatestApiVersion(RequestConfig requestConfig) {
    SupportedVersions versions = getSupportedVersions(requestConfig);
    Optional<SupportedVersionsVersions> supportedVersions = versions.getVersions()
        .stream()
        .filter(v -> Objects.equals(v.getStatus(), StatusEnum.ACTIVE))
        .findFirst();
    return supportedVersions.isPresent() ? supportedVersions.get().getVersionNumber() : getCurrentApiVersion();
  }

  private boolean isApiVersionSupported(RequestConfig requestConfig) {
    String currentApiVersion = getCurrentApiVersion();
    SupportedVersions versions = getSupportedVersions(requestConfig);
    Optional<SupportedVersionsVersions> supportedVersions = versions.getVersions()
        .stream()
        .filter(v -> Objects.equals(v.getVersionNumber(), currentApiVersion))
        .findFirst();
    return supportedVersions.isPresent();
  }
}
