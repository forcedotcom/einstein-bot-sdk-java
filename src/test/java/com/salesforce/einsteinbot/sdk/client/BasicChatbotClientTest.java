/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.salesforce.einsteinbot.sdk.api.HealthApi;
import com.salesforce.einsteinbot.sdk.api.MessagesApi;
import com.salesforce.einsteinbot.sdk.auth.AuthMechanism;
import com.salesforce.einsteinbot.sdk.model.RequestEnvelope;
import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;
import com.salesforce.einsteinbot.sdk.model.Status;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class BasicChatbotClientTest {

  private final String authToken = "C2C TOKEN";
  private final String orgId = "00DSB0000001ThY2AU";
  private final String basePath = "http://runtime-api-na-west.stg.chatbots.sfdc.sh";

  @Mock
  private MessagesApi mockMessagesApi;

  @Mock
  private HealthApi mockHealthApi;

  @Mock
  private ResponseEnvelope response;

  @Mock
  private Status healthStatus;

  @Mock
  private AuthMechanism mockAuthMechanism;

  @Test
  public void testSendChatbotRequest() {
    Mono<ResponseEnvelope> monoResponse = Mono.fromCallable(() -> response);

    RequestEnvelope requestEnvelope = new RequestEnvelope();
    RequestHeaders headers = RequestHeaders.builder()
        .orgId(orgId)
        .build();

    when(mockAuthMechanism.getToken()).thenReturn(authToken);
    when(mockMessagesApi.sendMessages(eq(orgId), anyString(), eq(requestEnvelope), isNull()))
        .thenReturn(monoResponse);

    BasicChatbotClient cut = BasicChatbotClient.builder()
        .basePath(basePath)
        .authMechanism(mockAuthMechanism)
        .build();
    cut.setMessagesApi(mockMessagesApi);

    assertEquals(response, cut.sendChatbotRequest(requestEnvelope, headers));
  }

  @Test
  public void testGetHealthStatus() {
    Mono<Status> monoResponse = Mono.fromCallable(new Callable<Status>() {
      @Override
      public Status call() throws Exception {
        return healthStatus;
      }
    });

    when(mockHealthApi.statusGet()).thenReturn(monoResponse);

    BasicChatbotClient cut = BasicChatbotClient.builder()
        .basePath(basePath)
        .authMechanism(mockAuthMechanism)
        .build();
    cut.setHealthApi(mockHealthApi);

    assertEquals(healthStatus, cut.getHealthStatus());

  }
}
