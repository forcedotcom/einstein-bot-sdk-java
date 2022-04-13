/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.examples;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.UUID;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class ApiExampleWithoutUsingSDK2 {

  private static final String RUNTIME_URL = "https://runtime-api-na-west.stg.chatbots.sfdc.sh";

  private final String orgId = "00DSB0000001ThY2AU";
  private final String botId = "0XxSB00000006rp0AA";
  private final String forceConfigEndPoint = "https://esw5.test1.my.pc-rnd.salesforce.com";

  //Replace following variables with real values before running.
  /*private final String loginEndpoint = "SALESFORCE_LOGIN_END_POINT";
  private final String connectedAppId = "YOUR_CONNECTED_APP_ID";
  private final String userId = "SALESFORCE_LOGIN_USER";
  private final String privateKeyFile = "src/test/resources/YourPrivateKeyFile.der";*/

  private final String loginEndpoint = "https://login.test1.pc-rnd.salesforce.com/";
  private final String connectedAppId = "3MVG9l3R9F9mHOGZUZs8TSRIINrHRklsp6OjPsKLQTUznlbLRyH_KMLfPG8SdPJugUtFa2UArLzpvtS74qDQ.";
  private final String userId = "admin1@esw5.sdb3";
  private final String privateKeyFile = "src/test/resources/PrivateKeyFalconTest1.der";


  private final int jwtExpiryMinutes = 60;

  private static ObjectMapper mapper = new ObjectMapper();

  private static final String START_SESSION_URI = "/v5.0.0/bots/{botId}/sessions";
  private static final String SEND_MESSAGE_URI = "/v5.0.0/sessions/{sessionId}/messages";
  private static final String END_SESSION_URI = "/v5.0.0/sessions/{sessionId}";
  private final String OAUTH_URL = loginEndpoint + "/services/oauth2/token";

  private RestTemplate restTemplate = new RestTemplate();

  private static final DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();

  static {
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
  }

  private void runExampleRequests() throws Exception {

    //1. Create JWT

    // Create Private Key
    File f = new File(privateKeyFile);
    DataInputStream dis = new DataInputStream(new FileInputStream(f));
    byte[] keyBytes = new byte[(int) f.length()];
    dis.readFully(keyBytes);
    dis.close();

    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    PrivateKey privateKey = kf.generatePrivate(spec);

    Map<String, Object> headers = new HashMap<String, Object>();
    headers.put("alg", "RS256");
    Algorithm algorithm = Algorithm.RSA256(null, (RSAPrivateKey) privateKey);

    Instant now = Instant.now();
    //Create JWT
    String jwt = JWT.create()
        .withHeader(headers)
        .withAudience(loginEndpoint)
        .withExpiresAt(Date.from(now.plus(jwtExpiryMinutes, ChronoUnit.MINUTES)))
        .withIssuer(connectedAppId)
        .withSubject(userId)
        .sign(algorithm);

    //2. Get OAuth Access Token
    // Create Http Post Form data.
    MultiValueMap<String, String> formData= new LinkedMultiValueMap<>();
    formData.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
    formData.add("assertion", jwt);

    // Create Http Headers.
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    // Create Http Request.
    HttpEntity<Map> oAuthHttpRequest = new HttpEntity<>(formData, httpHeaders);

    // Post Request to OAuth Endpoint
    ResponseEntity<String> response = restTemplate
        .postForEntity(OAUTH_URL, oAuthHttpRequest, String.class);

    //Parse Response to get access_token
    ObjectNode node = new ObjectMapper().readValue(response.getBody(), ObjectNode.class);
    String token = node.get("access_token").asText();

    //3. Send Start Chat Session Request

    // Create Http Headers
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.setContentType(MediaType.APPLICATION_JSON);
    requestHeaders.setBearerAuth(token);
    requestHeaders.add("X-Org-Id", orgId);

    // Create Request Body
    String message = "Hello";

    // Create payload in TextMessage format defined in Schema.
    String requestBody =
        "{\n"
          + "  \"externalSessionKey\": \"" + newRandomUUID() + "\",\n"
          + "  \"message\": {\n"
          + "    \"sequenceId\": " + System.currentTimeMillis() + ",\n"
          + "    \"text\": \"" + message + "\"\n"
          + "  },\n"
          + "  \"forceConfig\": {\n"
          + "    \"endpoint\": \"" + forceConfigEndPoint + "\"\n"
          + "  }\n"
        + "}";

    // Create HTTP Request
    HttpEntity<String> httpRequest = new HttpEntity<>(requestBody, requestHeaders);

    // Create URL with URI format v5.0.0/bots/{botId}/sessions
    String url = UriComponentsBuilder
        .fromHttpUrl(RUNTIME_URL)
        .path(START_SESSION_URI)
        .buildAndExpand(botId).toString();

    //Send Start Chat Session Request
    ResponseEntity<String> startSessionResponse = restTemplate
        .postForEntity(url, httpRequest , String.class);

    // Print Response Body that has response from Chatbot.
    System.out.println("Bot Start Session Response : " + getPrettyPrintedJson(startSessionResponse.getBody()));

    // Get SessionId from Response to send message to existing Chat Session.
    JsonNode responseNode = mapper
        .readValue(startSessionResponse.getBody(), JsonNode.class);
    String sessionId = responseNode.get("sessionId").asText();
  }

  private PrivateKey getPrivateKey(String filename) {
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

  private String createJwt(PrivateKey privateKey){

    Map<String, Object> headers = new HashMap<String, Object>();
    headers.put("alg", "RS256");
    Algorithm algorithm = Algorithm.RSA256(null, (RSAPrivateKey) privateKey);
    return JWT.create()
        .withHeader(headers)
        .withAudience(loginEndpoint)
        .withExpiresAt(Date.from(Instant.now().plus(jwtExpiryMinutes, ChronoUnit.MINUTES)))
        .withIssuer(connectedAppId)
        .withSubject(userId)
        .sign(algorithm);
  }

  private String getOAuthToken(String jwt) throws Exception{
    MultiValueMap<String, String> formData= new LinkedMultiValueMap<>();
    formData.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
    formData.add("assertion", jwt);

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    HttpEntity<Map> httpRequest = new HttpEntity<>(formData, httpHeaders);

    ResponseEntity<String> response = restTemplate.postForEntity(OAUTH_URL, httpRequest, String.class);
    System.out.println(response.getStatusCode());
    return getTokenFromOAuthResponse(response.getBody());
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



  private String getTokenFromOAuthResponse(String response) throws Exception{
    ObjectNode node = new ObjectMapper().readValue(response, ObjectNode.class);
    return node.get("access_token").asText();
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
          + "  }\n"
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



  public static void main(String[] args) throws Exception {
    new ApiExampleWithoutUsingSDK2()
        .runExampleRequests();

  }
}
