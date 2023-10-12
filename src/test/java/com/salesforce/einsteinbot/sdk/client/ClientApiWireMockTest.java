/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.salesforce.einsteinbot.sdk.client.model.BotHttpHeaders.HEADER_NAME_REQUEST_ID;
import static com.salesforce.einsteinbot.sdk.client.util.RequestFactory.buildBotSendMessageRequest;
import static com.salesforce.einsteinbot.sdk.client.util.RequestFactory.buildSessionBotEndSessionRequest;
import static com.salesforce.einsteinbot.sdk.client.util.RequestFactory.buildTextMessage;
import static com.salesforce.einsteinbot.sdk.util.TestUtils.EXPECTED_SDK_NAME;
import static com.salesforce.einsteinbot.sdk.util.TestUtils.TEST_MOCK_DIR;
import static com.salesforce.einsteinbot.sdk.util.TestUtils.readTestFileAsString;
import static com.salesforce.einsteinbot.sdk.util.UtilFunctions.convertObjectToJson;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.salesforce.einsteinbot.sdk.client.model.BotEndSessionRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotHttpHeaders;
import com.salesforce.einsteinbot.sdk.client.model.BotResponse;
import com.salesforce.einsteinbot.sdk.client.model.BotSendMessageRequest;
import com.salesforce.einsteinbot.sdk.client.model.ExternalSessionId;
import com.salesforce.einsteinbot.sdk.client.model.RequestConfig;
import com.salesforce.einsteinbot.sdk.client.model.RuntimeSessionId;
import com.salesforce.einsteinbot.sdk.exception.ChatbotResponseException;
import com.salesforce.einsteinbot.sdk.model.EndSessionReason;
import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;
import com.salesforce.einsteinbot.sdk.model.Status;
import de.mkammerer.wiremock.WireMockExtension;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientRequestException;

/**
 * ClientApiWireMockTest - Tests Http communication with Bot Runtime using wiremock.
 *
 * @author relango
 */
public class ClientApiWireMockTest {

  private static final String TEST_ORG_ID = "00Dxx0000006GprEAE";
  private static final String TEST_BOT_ID = "0Xxxx0000000001CAA";
  private static final String USER_AGENT_HEADER_KEY = "User-Agent";
  private static final String ORG_ID_HEADER_KEY = "X-Org-Id";
  private static final String REQUEST_ID_HEADER_KEY = "X-Request-ID";
  private static final String TEST_FORCE_CONFIG = "https://esw5.test1.my.pc-rnd.salesforce.com";
  private static final String EXTERNAL_SESSION_KEY = "session1";
  private static final String SESSION_ID = "chatbotSessionId";
  private static final String responseRequestId = "ResponseRequestId";

  private static final String START_SESSION_URI = "/assistants/" + TEST_BOT_ID + "/sessions";
  private static final String SEND_MESSAGE_URI = "/sessions/" + SESSION_ID + "/messages";
  private static final String END_SESSION_URI = "/sessions/" + SESSION_ID;
  private static final String STATUS_URI = "/status";
  private static final String VERSIONS_URI = "/versions";

  private static final String TEST_REQUEST_ID = UUID.randomUUID().toString();
  public static final String SESSION_END_REASON_HEADER_KEY = "X-Session-End-Reason";
  public static final String AUTHORIZATION_HEADER_KEY = "Authorization";

  private final RuntimeSessionId runtimeSessionId = new RuntimeSessionId(SESSION_ID);
  private final ExternalSessionId externalSessionId = new ExternalSessionId(EXTERNAL_SESSION_KEY);
  private final EndSessionReason endSessionReason = EndSessionReason.USERREQUEST;

  private static final RequestConfig requestConfig = RequestConfig
      .with()
      .botId(TEST_BOT_ID)
      .orgId(TEST_ORG_ID)
      .forceConfigEndpoint(TEST_FORCE_CONFIG)
      .build();

  @RegisterExtension
  WireMockExtension wireMock = new WireMockExtension(options()
      .dynamicPort()
      .dynamicHttpsPort());

  private BasicChatbotClient client;
  private BotSendMessageRequest botSendMessageRequest;
  private BotEndSessionRequest botEndSessionRequest;

  @BeforeEach
  private void setup() {
    client = ChatbotClients.basic()
        .basePath(wireMock.getBaseUri().toString())
        .authMechanism(new TestAuthMechanism())
        .build();
    botSendMessageRequest = buildBotSendMessageRequest(buildTextMessage("Hello"),
        Optional.ofNullable(TEST_REQUEST_ID));
    botEndSessionRequest = buildSessionBotEndSessionRequest(endSessionReason,
        Optional.ofNullable(TEST_REQUEST_ID));
    String responseBodyFile = "versionsResponse.json";
    stubVersionsResponse(responseBodyFile);
  }

  @Test
  void testStartSessionRequest() throws Exception {
    String responseBodyFile = "startSessionResponse.json";
    stubStartSessionResponse(responseBodyFile, HttpStatus.OK.value());

    BotResponse botResponse = client.startChatSession(requestConfig, externalSessionId,
        botSendMessageRequest);

    verifyRequestUriAndHeaders(START_SESSION_URI);
    verifyResponse(responseBodyFile, botResponse);
  }

  @Test
  void testSendMessageRequest() throws Exception {
    String responseBodyFile = "sendMessageResponse.json";
    stubSendMessageResponse(responseBodyFile);

    BotResponse botResponse = client.sendMessage(requestConfig, runtimeSessionId,
        botSendMessageRequest);
    verifyRequestUriAndHeaders(SEND_MESSAGE_URI);
    verifyResponse(responseBodyFile, botResponse);
  }

  @Test
  void testEndSessionRequest() throws Exception {
    String responseBodyFile = "endSessionResponse.json";
    stubEndSessionResponse(responseBodyFile);

    BotResponse botResponse = client.endChatSession(requestConfig, runtimeSessionId,
        botEndSessionRequest);
    verifyRequestUriAndHeadersForEndSession();
    verifyResponse(responseBodyFile, botResponse);
  }

  @Test
  void testStatus() throws Exception {
    String responseBodyFile = "statusResponse.json";
    stubStatusResponse(responseBodyFile);
    Status status = client.getHealthStatus();

    verifyResponseEnvelope(responseBodyFile, status);
  }

  @Test
  void testVerifyVersionFile() throws Exception {
    Properties properties;
    InputStream is = getClass().getClassLoader()
        .getResourceAsStream("properties-from-pom.properties");
    properties = new Properties();
    properties.load(is);
    String property = properties.getProperty("api-spec-yaml-file");
    assertEquals("v5_3_0_api_specs.yml", property);
  }

  @Test
  void testStartSessionRequestWithErrorResponse() throws Exception {
    String responseBodyFile = "errorResponse.json";
    int errorStatusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
    stubStartSessionResponse(responseBodyFile, errorStatusCode);

    Throwable exceptionThrown = assertThrows(java.lang.RuntimeException.class,
        () -> client.startChatSession(requestConfig, externalSessionId,
            botSendMessageRequest));

    ChatbotResponseException chatbotResponseException = validateAndGetCause(exceptionThrown,
        ChatbotResponseException.class);
    assertEquals(errorStatusCode, chatbotResponseException.getStatus());
    verifyResponseEnvelope(responseBodyFile, chatbotResponseException.getErrorResponse());
  }

  @Test
  void testStartSessionRequestWithErrorBeforeHittingServer() throws Exception {
    String errorMessage = "fault filter abort";
    int errorStatusCode = HttpStatus.NOT_FOUND.value();
    stubStartSessionResponseWithBodyText(errorMessage, errorStatusCode);

    Throwable exceptionThrown = assertThrows(java.lang.RuntimeException.class,
            () -> client.startChatSession(requestConfig, externalSessionId,
                    botSendMessageRequest));

    ChatbotResponseException chatbotResponseException = validateAndGetCause(exceptionThrown,
            ChatbotResponseException.class);
    assertEquals(errorStatusCode, chatbotResponseException.getStatus());
    assertEquals(errorMessage, chatbotResponseException.getErrorResponse().getError());
  }

  @Test
  void testConnectionError() {

    BasicChatbotClient clientForError = ChatbotClients.basic()
        .basePath(replaceUriWithInvalidPort(wireMock.getBaseUri()))
        .authMechanism(new TestAuthMechanism())
        .build();

    Throwable exceptionThrown = assertThrows(java.lang.RuntimeException.class,
        () -> clientForError.startChatSession(requestConfig, externalSessionId,
            botSendMessageRequest));

    WebClientRequestException cause = validateAndGetCause(exceptionThrown,
        WebClientRequestException.class);
    assertTrue(cause.getMessage().contains("Connection"));
  }

  private <T> T validateAndGetCause(Throwable throwable, Class<T> clazz) {
    Throwable cause = throwable.getCause().getCause();
    assertTrue(clazz.isInstance(cause));
    return clazz.cast(cause);
  }

  private String replaceUriWithInvalidPort(URI currentUri) {
    int currentPort = currentUri.getPort();
    String invalidPort = String.valueOf(currentUri.getPort() + 1);
    return currentUri.toString().replace(String.valueOf(currentPort), invalidPort);
  }

  private void verifyRequestUriAndHeaders(String expectedUri) {
    wireMock.verify(
        postRequestedFor(
            urlEqualTo(expectedUri))
            .withHeader(USER_AGENT_HEADER_KEY, containing(EXPECTED_SDK_NAME + "/"))
            .withHeader(ORG_ID_HEADER_KEY, equalTo(TEST_ORG_ID))
            .withHeader(REQUEST_ID_HEADER_KEY, equalTo(TEST_REQUEST_ID))
    );
  }

  private void verifyRequestUriAndHeadersForEndSession() {
    wireMock.verify(
        deleteRequestedFor(
            urlEqualTo(ClientApiWireMockTest.END_SESSION_URI))
            .withHeader(USER_AGENT_HEADER_KEY, containing(EXPECTED_SDK_NAME + "/"))
            .withHeader(ORG_ID_HEADER_KEY, equalTo(TEST_ORG_ID))
            .withHeader(REQUEST_ID_HEADER_KEY, equalTo(TEST_REQUEST_ID))
            .withHeader(SESSION_END_REASON_HEADER_KEY, equalTo(endSessionReason.getValue()))
    );
  }

  private void verifyResponse(String responseBodyFile, BotResponse botResponse)
      throws Exception {

    assertEquals(botResponse.getHttpStatusCode(), HttpStatus.OK.value());
    BotHttpHeaders actualHttpHeaders = botResponse.getHttpHeaders();

    assertTrue(actualHttpHeaders.getRuntimeCRCHeaderAsCSV().isEmpty());
    assertEquals(responseRequestId, String.join(", ", actualHttpHeaders.getRequestIdHeaderAsCSV()));
    verifyResponseEnvelope(responseBodyFile, botResponse.getResponseEnvelope());

    ResponseEnvelope responseEnvelope = botResponse.getResponseEnvelope();
    String expected = readTestFileAsString(responseBodyFile);

    Optional<String> actual = toJsonString(responseEnvelope);
    assertTrue(actual.isPresent());

    assertThatJson(actual.get())
        .when(Option.TREATING_NULL_AS_ABSENT, Option.IGNORING_EXTRA_FIELDS)
        .isEqualTo(expected);
  }

  private <T> void verifyResponseEnvelope(String responseBodyFile, T responseEnvelope)
      throws Exception {
    String expected = readTestFileAsString(responseBodyFile);

    Optional<String> actual = toJsonString(responseEnvelope);
    assertTrue(actual.isPresent());

    assertThatJson(actual.get())
        .when(Option.TREATING_NULL_AS_ABSENT, Option.IGNORING_EXTRA_FIELDS)
        .isEqualTo(expected);
  }

  private Optional<String> toJsonString(Object response) {
    try {
      return Optional.of(convertObjectToJson(response));
    } catch (JsonProcessingException e) {
      Assertions.fail("Exception in parsing Response ", e);
      return Optional.empty();
    }
  }

  private void stubStartSessionResponse(String responseBodyFile, int statusCode) {
    wireMock.stubFor(
        post(START_SESSION_URI)
            .willReturn
                (aResponse()
                    .withStatus(statusCode)
                    .withHeader("Content-Type", "application/json;charset=UTF-8")
                    .withHeader(HEADER_NAME_REQUEST_ID, responseRequestId)
                    .withBodyFile(TEST_MOCK_DIR + responseBodyFile))
    );
  }

  private void stubStartSessionResponseWithBodyText(String responseBody, int statusCode) {
    wireMock.stubFor(
            post(START_SESSION_URI)
                    .willReturn
                            (aResponse()
                                    .withStatus(statusCode)
                                    .withHeader("Content-Type", "text/html;charset=UTF-8")
                                    .withHeader(HEADER_NAME_REQUEST_ID, responseRequestId)
                                    .withBody(responseBody))
    );
  }

  private void stubSendMessageResponse(String responseBodyFile) {
    wireMock.stubFor(
        post(SEND_MESSAGE_URI)
            .willReturn
                (aResponse()
                    .withHeader("Content-Type", "application/json;charset=UTF-8")
                    .withHeader(HEADER_NAME_REQUEST_ID, responseRequestId)
                    .withBodyFile(TEST_MOCK_DIR + responseBodyFile))
    );
  }

  private void stubEndSessionResponse(String responseBodyFile) {
    wireMock.stubFor(
        delete(END_SESSION_URI)
            .willReturn
                (aResponse()
                    .withHeader("Content-Type", "application/json;charset=UTF-8")
                    .withHeader(HEADER_NAME_REQUEST_ID, responseRequestId)
                    .withBodyFile(TEST_MOCK_DIR + responseBodyFile))
    );
  }

  private void stubStatusResponse(String responseBodyFile) {
    wireMock.stubFor(
        get(STATUS_URI)
            .willReturn
                (aResponse()
                    .withHeader("Content-Type", "application/json;charset=UTF-8")
                    .withBodyFile(TEST_MOCK_DIR + responseBodyFile))
    );
  }

  private void stubVersionsResponse(String responseBodyFile) {
    wireMock.stubFor(
        get(VERSIONS_URI)
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json;charset=UTF-8")
                    .withBodyFile(TEST_MOCK_DIR + responseBodyFile)
            )
    );
  }

  private void stubVersionsResponse(String responseBodyFile, int statusCode) {
    wireMock.stubFor(
        get(VERSIONS_URI)
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json;charset=UTF-8")
                    .withBodyFile(TEST_MOCK_DIR + responseBodyFile)
                    .withStatus(statusCode)
            )
    );
  }

}