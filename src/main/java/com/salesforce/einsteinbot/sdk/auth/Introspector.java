
/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

public class Introspector {

  private String connectedAppId;
  private String connectedAppSecret;
  private WebClient webClient;

  public Introspector(String connectedAppId, String connectedAppSecret, String endpoint) {
    this.connectedAppId = connectedAppId;
    this.connectedAppSecret = connectedAppSecret;
    this.webClient = WebClient.create(endpoint);
  }

  public IntrospectionResult introspect(String token) {
    return webClient.post()
        .uri("/services/oauth2/introspect")
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .header(HttpHeaders.AUTHORIZATION, getAuthorization())
        .body(BodyInserters.fromFormData("token", token)
            .with("token_type", "access_token"))
        .retrieve()
        .bodyToMono(IntrospectionResult.class)
        .block();
  }

  private String getAuthorization() {
    String auth = this.connectedAppId + ":" + this.connectedAppSecret;
    byte[] encodedAuth = Base64.getEncoder().encode(
        auth.getBytes(StandardCharsets.US_ASCII));
    return "Basic " + new String(encodedAuth);
  }

}
