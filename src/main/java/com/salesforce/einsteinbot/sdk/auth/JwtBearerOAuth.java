
/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.salesforce.einsteinbot.sdk.cache.Cache;
import com.salesforce.einsteinbot.sdk.exception.OAuthResponseException;
import com.salesforce.einsteinbot.sdk.util.WebClientUtil;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Implementation of AuthMechanism interface that is used to integrate with Einstein Bots using
 * OAuth.
 */
public class JwtBearerOAuth implements AuthMechanism {

  private static final Logger logger = LoggerFactory.getLogger(JwtBearerOAuth.class);

  public static final String JWT_AUTH_TOKEN_PREFIX = "Bearer ";

  private final int jwtExpiryMinutes = 15;
  private final String cacheKeyPrefix = "bots-oAuthToken-";

  private String loginEndpoint;
  private String connectedAppId;
  private String userId;
  private PrivateKey privateKey;
  private WebClient webClient;
  private Optional<Cache> cache;
  private Introspector introspector;

  private JwtBearerOAuth(PrivateKey privateKey, String loginEndpoint, String connectedAppId,
      String connectedAppSecret,
      String userId, Optional<Cache> cache) {
    Objects.nonNull(privateKey);
    Objects.nonNull(loginEndpoint);
    Objects.nonNull(connectedAppId);
    Objects.nonNull(connectedAppSecret);
    Objects.nonNull(userId);
    Objects.nonNull(cache);
    this.privateKey = privateKey;
    this.userId = userId;
    this.connectedAppId = connectedAppId;
    this.loginEndpoint = loginEndpoint;
    this.webClient = WebClient
        .builder()
        .baseUrl(loginEndpoint)
        .filter(WebClientUtil.createFilter(
            clientRequest -> WebClientUtil.createLoggingRequestProcessor(clientRequest),
            clientResponse -> WebClientUtil
                .createErrorResponseProcessor(clientResponse, this::mapErrorResponse)))
        .build();
    this.cache = cache;
    this.introspector = new Introspector(connectedAppId, connectedAppSecret, loginEndpoint);
  }

  private Mono<ClientResponse> mapErrorResponse(ClientResponse clientResponse) {
    return clientResponse
        .bodyToMono(String.class)
        .flatMap(errorDetails -> Mono
            .error(new OAuthResponseException(clientResponse.statusCode(), errorDetails)));
  }

  @VisibleForTesting
  void setIntrospector(Introspector introspector) {
    this.introspector = introspector;
  }

  public static PrivateKeyBuilder with() {
    return new FluentBuilder();
  }

  @Override
  public String getToken() {
    Optional<String> token = cache.flatMap(c -> c.get(getCacheKey()));
    if (token.isPresent()) {
      logger.debug("Found cached OAuth token.");
      return token.get();
    }

    logger.debug("Did not find OAuth token in cache. Will retrieve from OAuth server.");
    Instant now = Instant.now();
    String jwt = null;

    try {
      Map<String, Object> headers = new HashMap<String, Object>();
      headers.put("alg", "RS256");
      Algorithm algorithm = Algorithm.RSA256(null, (RSAPrivateKey) privateKey);
      jwt = JWT.create()
          .withHeader(headers)
          .withAudience(loginEndpoint)
          .withExpiresAt(Date.from(now.plus(jwtExpiryMinutes, ChronoUnit.MINUTES)))
          .withIssuer(connectedAppId)
          .withSubject(userId)
          .sign(algorithm);

      logger.debug("Generated jwt: {} ", jwt);

    } catch (JWTCreationException exception) {
      //Invalid Signing configuration / Couldn't convert Claims.
      throw new RuntimeException(exception);
    }

    String response = webClient.post()
        .uri("/services/oauth2/token")
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .body(
            BodyInserters.fromFormData("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                .with("assertion", jwt))
        .retrieve()
        .bodyToMono(String.class)
        .block();

    String oAuthToken = null;
    try {
      ObjectNode node = new ObjectMapper().readValue(response, ObjectNode.class);
      oAuthToken = node.get("access_token").asText();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    IntrospectionResult iResult = introspector.introspect(oAuthToken);
    if (!iResult.isActive()) {
      throw new RuntimeException("OAuth token is not active.");
    }

    Instant expiry = Instant.ofEpochSecond(iResult.getExp());
    long ttl = Math.max(0, Instant.now().until(expiry, ChronoUnit.SECONDS) - 300);

    if (cache.isPresent()) {
      cache.get().set(getCacheKey(), oAuthToken, ttl);
    }
    return oAuthToken;
  }

  @Override
  public String getAuthorizationHeader() {
    return JWT_AUTH_TOKEN_PREFIX + getToken();
  }

  private static PrivateKey getPrivateKey(String filename) {
    try {
      File f = new File(filename);
      FileInputStream fis = new FileInputStream(f);
      DataInputStream dis = new DataInputStream(fis);
      byte[] keyBytes = new byte[(int) f.length()];
      dis.readFully(keyBytes);
      dis.close();

      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return kf.generatePrivate(spec);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private String getCacheKey() {
    return cacheKeyPrefix + connectedAppId;
  }

  public static class FluentBuilder implements PrivateKeyBuilder,
      LoginEndpointBuilder,
      ConnectedAppIdBuilder,
      ConnectedAppSecretBuilder,
      UserIdBuilder,
      FinalBuilder {

    PrivateKey privateKey;
    String loginEndpoint;
    String connectedAppId;
    String connectedAppSecret;
    String userId;
    Optional<Cache> cache = Optional.empty();

    @Override
    public LoginEndpointBuilder privateKey(PrivateKey privateKey) {
      this.privateKey = privateKey;
      return this;
    }

    @Override
    public LoginEndpointBuilder privateKeyFilePath(String privateKeyFilePath) {
      this.privateKey = getPrivateKey(privateKeyFilePath);
      return this;
    }

    @Override
    public ConnectedAppIdBuilder loginEndpoint(String loginEndpoint) {
      this.loginEndpoint = loginEndpoint;
      return this;
    }

    @Override
    public ConnectedAppSecretBuilder connectedAppId(String connectedAppId) {
      this.connectedAppId = connectedAppId;
      return this;
    }

    @Override
    public UserIdBuilder connectedAppSecret(String connectedAppSecret) {
      this.connectedAppSecret = connectedAppSecret;
      return this;
    }

    @Override
    public FinalBuilder userId(String userId) {
      this.userId = userId;
      return this;
    }

    @Override
    public FinalBuilder cache(Cache cache) {
      this.cache = Optional.ofNullable(cache);
      return this;
    }

    @Override
    public FinalBuilder cache(Optional<Cache> cache) {
      this.cache = cache;
      return this;
    }

    @Override
    public JwtBearerOAuth build() {
      return new JwtBearerOAuth(privateKey, loginEndpoint, connectedAppId, connectedAppSecret,
          userId, cache);
    }
  }

  public interface PrivateKeyBuilder {

    LoginEndpointBuilder privateKey(PrivateKey privateKey);

    LoginEndpointBuilder privateKeyFilePath(String privateKeyFilePath);
  }

  public interface LoginEndpointBuilder {

    ConnectedAppIdBuilder loginEndpoint(String loginEndpoint);
  }

  public interface ConnectedAppIdBuilder {

    ConnectedAppSecretBuilder connectedAppId(String connectedAppId);
  }

  public interface ConnectedAppSecretBuilder {

    UserIdBuilder connectedAppSecret(String connectedAppSecret);
  }

  public interface UserIdBuilder {

    FinalBuilder userId(String userId);
  }

  public interface FinalBuilder {

    FinalBuilder cache(Cache cache);

    FinalBuilder cache(Optional<Cache> cache);

    AuthMechanism build();
  }
}
