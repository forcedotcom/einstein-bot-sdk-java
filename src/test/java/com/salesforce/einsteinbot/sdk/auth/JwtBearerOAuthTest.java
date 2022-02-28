/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.salesforce.einsteinbot.sdk.cache.Cache;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JwtBearerOAuthTest {

  private final String connectedAppId = "abcdefghi";
  private final String connectedAppSecret = "secret";
  private final String userId = "botsUser@company.org";
  private final String token = "00DRM0000006k89!AREAQOYXyPiK.xdAWPPD.6tKsBQHt4kZKUPtTBA0JMC20OmW22c4R7g6q22fwcmPJAIgcRq7JIlR9FBU1txEM2j66jRZoGLN";

  private final String tokenResponse = "{\"access_token\":\"" + token + "\","
      + "\"scope\":\"chatbot_api full\",\"instance_url\":\"https://drm0000006k892aa.my.stmfa.stm.salesforce.com\","
      + "\"id\":\"https://login.stmfa.stm.salesforce.com/id/00DRM0000006k892AA/005RM000001a3FgYAI\",\"token_type\":\"Bearer\"}";

  private String loginEndpoint;

  @Mock
  private Introspector mockIntrospector;

  private static PrivateKey privateKey;

  @Mock
  private Cache mockCache;

  public static MockWebServer mockBackEnd;

  @BeforeAll
  static void setUp() throws Exception {
    privateKey = createTestPrivateKey();
    mockBackEnd = new MockWebServer();
    mockBackEnd.start();
  }

  @AfterAll
  static void tearDown() throws IOException {
    mockBackEnd.shutdown();
  }

  @BeforeEach
  void initialize() {
    loginEndpoint = String.format("http://localhost:%s", mockBackEnd.getPort());
  }

  private static PrivateKey createTestPrivateKey() throws Exception {
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    BigInteger randomBigInteger = new BigInteger(512, new Random());

    RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(randomBigInteger, randomBigInteger);

    return keyFactory.generatePrivate(privateKeySpec);
  }

  @Test
  public void getOAuthToken() throws InterruptedException {
    JwtBearerOAuth oAuth = new JwtBearerOAuth(privateKey, loginEndpoint, connectedAppId,
        connectedAppSecret, userId, mockCache);
    oAuth.setIntrospector(mockIntrospector);

    MockResponse mockResponse = new MockResponse()
        .setBody(tokenResponse)
        .addHeader("Content-Type", "application/json");
    mockBackEnd.enqueue(mockResponse);
    mockBackEnd.enqueue(mockResponse);

    IntrospectionResult introspectionResult = new IntrospectionResult(true,
        Instant.now().plus(2, ChronoUnit.HOURS).getEpochSecond()); //expires after two hours

    when(mockIntrospector.introspect(token)).thenReturn(introspectionResult)
        .thenReturn(introspectionResult);

    // get token
    String resultToken = oAuth.getToken();
    assertEquals(token, resultToken);
    assertEquals(JwtBearerOAuth.JWT_AUTH_TOKEN_PREFIX + token, oAuth.getAuthorizationHeader());

    // verify request sent to server
    RecordedRequest recordedRequest = mockBackEnd.takeRequest();
    assertEquals("POST", recordedRequest.getMethod());
    assertEquals("/services/oauth2/token", recordedRequest.getPath());

    // verify token is cached
    ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
    verify(mockCache, times(2)).set(eq(getCacheKey()), eq(token), ttlCaptor.capture());
    assertTrue(ttlCaptor.getValue() > 6500,
        "ttl is too short"); // ttl should be just under two hours
    assertTrue(ttlCaptor.getValue() < 7200, "ttl is too long");

  }

  @Test
  public void getOAuthTokenFromCache() throws InterruptedException {
    JwtBearerOAuth oAuth = new JwtBearerOAuth(privateKey, loginEndpoint, connectedAppId,
        connectedAppSecret, userId, mockCache);
    when(mockCache.get(getCacheKey())).thenReturn(Optional.of(token));

    assertEquals(token, oAuth.getToken());
  }

  private String getCacheKey() {
    return "bots-oAuthToken-" + connectedAppId;
  }
}
