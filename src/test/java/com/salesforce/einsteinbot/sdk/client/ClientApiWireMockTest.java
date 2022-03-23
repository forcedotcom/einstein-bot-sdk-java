/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.salesforce.einsteinbot.sdk.util.TestUtils.EXPECTED_SDK_NAME;
import static com.salesforce.einsteinbot.sdk.util.TestUtils.TEST_MOCK_DIR;
import static com.salesforce.einsteinbot.sdk.util.TestUtils.readTestFileAsString;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.einsteinbot.sdk.model.AnyRequestMessage;
import com.salesforce.einsteinbot.sdk.model.InitMessage;
import com.salesforce.einsteinbot.sdk.model.RequestEnvelope;
import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;
import de.mkammerer.wiremock.WireMockExtension;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * ClientApiWireMockTest - Tests Http communication with Bot Runtime using wiremock.
 * @author relango
 */
public class ClientApiWireMockTest {

  public static final String TEST_ORG_ID = "00Dxx0000006GprEAE";
  public static final String MESSAGES_URI = "/v4.0.0/messages";
  public static final String TEST_BOT_ID = "0Xxxx0000000001CAA";
  public static final String TEST_REQUEST_ID = UUID.randomUUID().toString();

  @RegisterExtension
  WireMockExtension wireMock = new WireMockExtension(options()
      .dynamicPort()
      .dynamicHttpsPort());

  private ObjectMapper mapper = new ObjectMapper();

  private RequestHeaders requestHeaders = RequestHeaders.builder()
      .orgId(TEST_ORG_ID)
      .requestId(TEST_REQUEST_ID)
      .build();

  private ChatbotClient client;

  @BeforeEach
  private void setup() {
    client = BasicChatbotClient.builder()
        .basePath(wireMock.getBaseUri().toString())
        .authMechanism(new TestAuthMechanism())
        .build();
  }

  @Test
  void testMessagesInitMessage() throws Exception {
    String responseBodyFile = "initMessageTextResponse.json";
    stubMessagesInitRequest(responseBodyFile);
    RequestEnvelope requestEnvelope = createRequestEnvelope();

    ResponseEnvelope responseEnvelope = client.sendChatbotRequest(requestEnvelope, requestHeaders);

    wireMock.verify(
        postRequestedFor(
            urlEqualTo(MESSAGES_URI))
            .withHeader("User-Agent", containing(EXPECTED_SDK_NAME + "/"))
            .withHeader("X-Org-Id", equalTo(TEST_ORG_ID))
            .withHeader("X-Request-ID", equalTo(TEST_REQUEST_ID))
    );

    String expected = readTestFileAsString(responseBodyFile);

    Optional<String> actual = toJsonString(responseEnvelope);
    Assertions.assertTrue(actual.isPresent());

    assertThatJson(actual.get())
        .when(Option.TREATING_NULL_AS_ABSENT, Option.IGNORING_EXTRA_FIELDS)
        .isEqualTo(expected);
  }

  private Optional<String> toJsonString(ResponseEnvelope response) {
    try {
      return Optional.of(mapper.writeValueAsString(response));
    } catch (JsonProcessingException e) {
      Assertions.fail("Exception in parsing Response ", e);
      return Optional.empty();
    }
  }

  private RequestEnvelope createRequestEnvelope() {
    RequestEnvelope requestEnvelope = new RequestEnvelope();

    AnyRequestMessage message = new InitMessage()
        .type(InitMessage.TypeEnum.INIT)
        .sequenceId(1l)
        .text("hello");

    requestEnvelope
        .externalSessionKey(UUID.randomUUID().toString())
        .botId(TEST_BOT_ID)
        .messages(Arrays.asList(message));

    return requestEnvelope;
  }


  private void stubMessagesInitRequest(String responseBodyFile) {
    wireMock.stubFor(
        post(MESSAGES_URI)
            .willReturn
                (aResponse()
                    .withHeader("Content-Type", "application/json;charset=UTF-8")
                    .withBodyFile(TEST_MOCK_DIR + responseBodyFile))
    );
  }

}