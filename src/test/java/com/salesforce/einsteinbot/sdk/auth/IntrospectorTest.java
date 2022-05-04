/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for Introspector
 *
 * @author relango
 */
@ExtendWith(MockitoExtension.class)
public class IntrospectorTest {

  private final String connectedAppId = "abcdefghi";
  private final String connectedAppSecret = "secret";
  private final String introspectResponse =
      "{\"active\":true,\"scope\":\"full chatbot_api\",\"client_id\":\"3MVG9qKMKuRGRcbvcbIIXp10B7zSBvx.NOWQt0UlNx0BGwWtLF2Gzzlb0GB_ALlwga1JbYzYD4hkKrBi5aBRU\","
          + "\"username\":\"yxu@salesforce.com\",\"sub\":\"https://login.stmfa.stm.salesforce.com/id/00DRM0000006k892AA/005RM000001a3FgYAI\","
          + "\"token_type\":\"access_token\",\"exp\":1639091586,\"iat\":1639084386,\"nbf\":1639084386}";

  private String endpoint;

  public static MockWebServer mockBackEnd;

  @BeforeAll
  static void setUp() throws IOException {
    mockBackEnd = new MockWebServer();
    mockBackEnd.start();
  }

  @AfterAll
  static void tearDown() throws IOException {
    mockBackEnd.shutdown();
  }

  @BeforeEach
  void initialize() {
    endpoint = String.format("http://localhost:%s", mockBackEnd.getPort());
  }

  @Test
  public void introspect() throws InterruptedException {
    String token = "token123";
    Introspector introspector = new Introspector(connectedAppId, connectedAppSecret, endpoint);

    mockBackEnd.enqueue(new MockResponse()
        .setBody(introspectResponse)
        .addHeader("Content-Type", "application/json"));

    IntrospectionResult result = introspector.introspect(token);

    // verify request sent to server
    RecordedRequest recordedRequest = mockBackEnd.takeRequest();
    assertEquals("POST", recordedRequest.getMethod());
    assertEquals("/services/oauth2/introspect", recordedRequest.getPath());
    assertEquals(String.format("[text=token=%s&token_type=access_token]", token),
        recordedRequest.getBody().toString());
    assertEquals(getAuthorization(), recordedRequest.getHeader(HttpHeaders.AUTHORIZATION));

    // verify result
    assertEquals(true, result.isActive());
    assertEquals(1639091586L, result.getExp());
  }

  private String getAuthorization() {
    String auth = this.connectedAppId + ":" + this.connectedAppSecret;
    byte[] encodedAuth = Base64.getEncoder().encode(
        auth.getBytes(StandardCharsets.US_ASCII));
    return "Basic " + new String(encodedAuth);
  }
}
