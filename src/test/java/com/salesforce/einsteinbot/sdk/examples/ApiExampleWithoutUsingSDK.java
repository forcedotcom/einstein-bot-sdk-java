/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.examples;

import static java.util.Collections.singletonList;

import java.util.Collections;
import java.util.UUID;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

  private static final String START_SESSION_URI = "/v5.0.0/bots/{botId}/sessions";
  private static final String SEND_MESSAGE_URI = "/v5.0.0/sessions/{sessionId}/messages";
  private static final String END_SESSION_URI = "/v5.0.0/sessions/{sessionId}";

  private RestTemplate restTemplate = new RestTemplate();

  public ApiExampleWithoutUsingSDK() {
    String url = UriComponentsBuilder.fromHttpUrl(RUNTIME_URL).path(START_SESSION_URI).buildAndExpand(botId).toString();
    String token = "00DSB0000001ThY!AQEAQO7xRclWBwPMhb2BLOgSKSwQsqG1oTAPQkNVsEnfolKl_cTWfTxfDPcMuCQcAGH92dzr8ZXdmx42G1pIVcxI6r_aYcix";
    HttpHeaders requestHeaders = createHttpHeaders(token);
    String requestBody = createInitRequestBody(newRandomUUID(), "hello");
    HttpEntity<String> httpRequest = new HttpEntity<>(requestBody, requestHeaders);

    ResponseEntity<String> response = restTemplate.postForEntity(url, httpRequest , String.class);

    System.out.println(response);
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

  public static void main(String[] args) {
    new ApiExampleWithoutUsingSDK();

  }

}
