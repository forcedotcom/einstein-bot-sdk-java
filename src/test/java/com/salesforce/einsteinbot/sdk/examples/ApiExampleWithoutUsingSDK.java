/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.examples;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.UUID;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class ApiExampleWithoutUsingSDK {

  private static final String RUNTIME_URL = "https://runtime-api-na-west.stg.chatbots.sfdc.sh";
  private final String integrationName = "ConnectorExample";

  private final String orgId = "00DSB0000001ThY2AU";
  private final String botId = "0XxSB00000006rp0AA";
  private final String forceConfigEndPoint = "https://esw5.test1.my.pc-rnd.salesforce.com";

  //Replace following variables with real values before running. //TODO
  private final String loginEndpoint = "https://login.test1.pc-rnd.salesforce.com/";
  private final String connectedAppId = "3MVG9l3R9F9mHOGZUZs8TSRIINrHRklsp6OjPsKLQTUznlbLRyH_KMLfPG8SdPJugUtFa2UArLzpvtS74qDQ.";
  private final String secret = "1B57EFD4F6D22302A6D4FA9077430191CFFDFAEA22C6ABDA6FCB45993A8AD421";
  private final String userId = "admin1@esw5.sdb3";
  private static ObjectMapper mapper = new ObjectMapper();

  private static final String START_SESSION_URI = "/v5.0.0/bots/{botId}/sessions";
  private static final String SEND_MESSAGE_URI = "/v5.0.0/sessions/{sessionId}/messages";
  private static final String END_SESSION_URI = "/v5.0.0/sessions/{sessionId}";

  private RestTemplate restTemplate = new RestTemplate();

  private static final DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();

  static {
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
  }

  private void runExampleRequests() throws JsonProcessingException {
    String token = "00DSB0000001ThY!AQEAQO7xRclWBwPMhb2BLOgSKSwQsqG1oTAPQkNVsEnfolKl_cTWfTxfDPcMuCQcAGH92dzr8ZXdmx42G1pIVcxI6r_aYcix";

    String startChatSessionResponse = startChatSession(token, "hello");
    System.out.println("Bot Start Session Response : " + getPrettyPrintedJson(startChatSessionResponse));

    String sessionId = getSessionIdFromResponse(startChatSessionResponse);
    System.out.println(sessionId);

    String botResponse = sendTextMessage(token, sessionId, "Order Status");
    System.out.println("Bot Response : " + getPrettyPrintedJson(botResponse));

    String endSessionResponse = endChatConversation(token, sessionId, "UserRequest");
    System.out.println("Bot End Session Response : " + getPrettyPrintedJson(endSessionResponse));
  }

  private String startChatSession(String token, String initMessage){
    String url = createStartSessionUrl();
    HttpHeaders requestHeaders = createHttpHeaders(token);
    String requestBody = createInitRequestBody(newRandomUUID(), initMessage);

    HttpEntity<String> httpRequest = createHttpEntityForRequest(requestHeaders, requestBody);
    ResponseEntity<String> response = restTemplate.postForEntity(url, httpRequest , String.class);

    System.out.println(response.getStatusCode());
    return response.getBody();
  }

  private String sendTextMessage(String token, String sessionId, String message){
    String url = createContinueSessionUrl(sessionId);
    HttpHeaders requestHeaders = createHttpHeaders(token);
    String requestBody = createTextMessageRequestBody(message);

    HttpEntity<String> httpRequest = createHttpEntityForRequest(requestHeaders, requestBody);
    ResponseEntity<String> response = restTemplate.postForEntity(url, httpRequest , String.class);

    System.out.println(response.getStatusCode());
    return response.getBody();
  }

  private String endChatConversation(String token, String sessionId, String reason){
    String url = createEndSessionUrl(sessionId);
    HttpHeaders requestHeaders = createHttpHeaders(token);
    requestHeaders.add("X-Session-End-Reason", reason);

    HttpEntity<String> httpRequest = new HttpEntity<>(requestHeaders);
    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, httpRequest, String.class);

    System.out.println(response.getStatusCode());
    System.out.println(response.getBody());
    return response.getBody();
  }

  private String getSessionIdFromResponse(String json) throws JsonProcessingException {
    JsonNode node = mapper.readValue(json, JsonNode.class);
    JsonNode sessionIdNode = node.get("sessionId");
    return sessionIdNode.asText();
  }


  public static String newRandomUUID() {
    return UUID.randomUUID().toString();
  }

  private HttpHeaders createHttpHeaders(String token){
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);
    headers.add("X-Org-Id", orgId);
    return headers;
  }

  private HttpEntity<String> createHttpEntityForRequest(HttpHeaders requestHeaders, String requestBody){
    return new HttpEntity<>(requestBody, requestHeaders);
  }

  private String createStartSessionUrl(){
    return UriComponentsBuilder
        .fromHttpUrl(RUNTIME_URL)
        .path(START_SESSION_URI)
        .buildAndExpand(botId).toString();
  }

  private String createContinueSessionUrl(String sessionId){
    return UriComponentsBuilder
        .fromHttpUrl(RUNTIME_URL)
        .path(SEND_MESSAGE_URI)
        .buildAndExpand(sessionId).toString();
  }

  private String createEndSessionUrl(String sessionId){
    return UriComponentsBuilder
        .fromHttpUrl(RUNTIME_URL)
        .path(END_SESSION_URI)
        .buildAndExpand(sessionId).toString();
  }

  private String createInitRequestBody(String externalSessionId, String message){
    return
        "{\n"
          + "  \"externalSessionKey\": \"" + externalSessionId + "\",\n"
          + "  \"message\": {\n"
          + "    \"sequenceId\": " + System.currentTimeMillis() + ",\n"
          + "    \"text\": \"" + message + "\"\n"
          + "  },\n"
          + "  \"forceConfig\": {\n"
          + "    \"endpoint\": \"" + forceConfigEndPoint + "\"\n"
          + "  },\n"
          + "  \"variables\": [\n" //TODO remove variable if not needed
          + "    \n"
          + "  ]\n"
        + "}";
  }

  private String createTextMessageRequestBody(String message){
    return
        "{\n"
          + "  \"message\" : {\n"
          + "    \"type\" : \"text\",\n"
          + "    \"sequenceId\": " + System.currentTimeMillis() + ",\n"
          + "    \"text\": \"" + message + "\"\n"
          + "  }\n"
          + ""
        + "}";
  }

  public String getPrettyPrintedJson(String json) throws JsonProcessingException {
    return mapper.writer(prettyPrinter).writeValueAsString(mapper.readValue(json, Object.class));
  }

  public static void main(String[] args) throws JsonProcessingException {
    new ApiExampleWithoutUsingSDK()
        .runExampleRequests();

  }

}
