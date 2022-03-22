/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.salesforce.einsteinbot.sdk.model.*;
import com.salesforce.einsteinbot.sdk.model.AnyVariable;
import de.mkammerer.wiremock.WireMockExtension;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.salesforce.einsteinbot.sdk.utils.TestUtils.TEST_MOCK_DIR;
import static com.salesforce.einsteinbot.sdk.utils.TestUtils.readTestFileAsString;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

public class ClientApiWireMockTest {

  //TODO copied from https://git.soma.salesforce.com/chatbots/module-api-sdk-java/blob/06f716b18ef68d5ce4260828953f10f1e4a85e88/src/test/java/com/salesforce/chatbot/sdk/test/ServiceApiSdkMockTest.java
  @RegisterExtension
  WireMockExtension wireMock = new WireMockExtension(options().port(3001));//TODO get random available port

  private ObjectMapper mapper = new ObjectMapper();

  private ChatbotClient client;

  @BeforeEach
  private void setup(){
    client = BasicChatbotClient.builder()
        .basePath(wireMock.getBaseUri().toString())
        .authMechanism(new TestAuthMechanism())
        .build();
  }

  @Test
  void testMessagesInitMessage() throws Exception {
    String responseBodyFile = "initMessageTextResponse.json";
    stubMessagesInitRequest(responseBodyFile);
    String xOrgId = "00Dxx0000006GprEAE";
    RequestEnvelope requestEnvelope = createRequestEnvelope();

    ResponseEnvelope responseEnvelope = client.sendChatbotRequest(requestEnvelope, RequestHeaders.builder().orgId(xOrgId).build());

    String expected = readTestFileAsString(responseBodyFile);

    Optional<String> actual = toJsonString(responseEnvelope);
    Assertions.assertTrue(actual.isPresent());

    wireMock.verify(
        postRequestedFor(
            urlEqualTo("/v4.0.0/messages"))
            .withHeader("User-Agent", containing("einstein-bot-sdk-java")
            )
    );

    assertThatJson(actual.get())
        .when(Option.TREATING_NULL_AS_ABSENT, Option.IGNORING_EXTRA_FIELDS)
        .isEqualTo(expected);

    /*StepVerifier.create(response.map(this::toJsonString))
        .assertNext(actual -> {
          Assertions.assertTrue(actual.isPresent());
          System.out.println("Actual : " + actual);
          assertThatJson(actual.get())
              .when(Option.TREATING_NULL_AS_ABSENT, Option.IGNORING_EXTRA_FIELDS)
              .isEqualTo(expected);
        })
        .verifyComplete();*/


  }

   /* TODO need to explore error handling : https://stackoverflow.com/questions/49485523/get-api-response-error-message-using-web-client-mono-in-spring-boot
     @Test
    void testMessagesErrorMessage() throws Exception {
        String responseBodyFile = "initMessageTextResponse.json";
        stubMessagesInitRequest(responseBodyFile);
        String xOrgId = "00Dxx0000006GprEAE";
        String xRequestID = "08c38dcf-af09-4d96-899f-6247052d6f00";
        RequestEnvelope requestEnvelope = createRequestEnvelope();
        String xBotMode = null;
        String xRuntimeCRC = null;
        Mono<ResponseEnvelope> response = messagesApiForError
          .sendMessages(xOrgId, xRequestID, requestEnvelope, xBotMode, xRuntimeCRC);

        String expected = readTestFileAsString(responseBodyFile);

        messagesApi.getApiClient().

        response.doOnError(v -> System.out.println(" RAJA ERROR: " + v)).block();

       StepVerifier.create(response)
          .ex
          .assertNext(actual -> {
              Assertions.assertTrue(actual.isPresent());
              System.out.println("Actual : " + actual);
              assertThatJson(actual.get())
                .when(Option.TREATING_NULL_AS_ABSENT, Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo(expected);
          })
          .verifyComplete();
    }*/

  private Optional<String> toJsonString(ResponseEnvelope response){
    try {
      return Optional.of(mapper.writeValueAsString(response));
    } catch (JsonProcessingException e) {
      Assertions.fail("Exception in parsing Response ", e);
      return Optional.empty();
    }
  }

  private RequestEnvelope createRequestEnvelope(){
    RequestEnvelope requestEnvelope = new RequestEnvelope();

    AnyRequestMessage message = new InitMessage()
        .type(InitMessage.TypeEnum.INIT)
        .sequenceId(1l)
        .variables(createVariables())
        .text("error");

    requestEnvelope
        .sessionId("175e31d1-3ba8-4f07-bf25-924ebe3a7ce8")
        .externalSessionKey("175e31d1-3ba8-4f07-bf25-924ebe3a7ce8")
        .botId("0Xxxx0000000001CAA")
        .messages(Arrays.asList(message));

    return requestEnvelope;
  }

  private List<AnyVariable> createVariables() {
    List<AnyVariable> variables = new ArrayList<>();
    variables.add(new TextVariable().name("textVar")
        .type(TextVariable.TypeEnum.TEXT)
        .value("text_value"));

    variables.add(new DateTimeVariable()
        .name("dateTimeVar")
        .type(DateTimeVariable.TypeEnum.DATETIME)
        .value("12/11/2020"));

    return variables;
  }

  private void stubMessagesInitRequest(String responseBodyFile){
    wireMock.stubFor(
        post("/v4.0.0/messages")
            .willReturn
                (aResponse()
                    .withHeader("Content-Type", "application/json;charset=UTF-8")
                    .withBodyFile(TEST_MOCK_DIR + responseBodyFile))
    );
  }

}