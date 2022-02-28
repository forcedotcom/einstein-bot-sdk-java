/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.examples;

import com.salesforce.einsteinbot.sdk.auth.AuthMechanism;
import com.salesforce.einsteinbot.sdk.auth.JwtBearerOAuth;
import com.salesforce.einsteinbot.sdk.cache.InMemoryCache;
import com.salesforce.einsteinbot.sdk.client.BasicChatbotClient;
import com.salesforce.einsteinbot.sdk.client.ChatbotClient;
import com.salesforce.einsteinbot.sdk.client.RequestHeaders;
import com.salesforce.einsteinbot.sdk.client.SessionManagedChatbotClient;
import com.salesforce.einsteinbot.sdk.model.ForceConfig;
import com.salesforce.einsteinbot.sdk.model.InitMessage;
import com.salesforce.einsteinbot.sdk.model.RequestEnvelope;
import com.salesforce.einsteinbot.sdk.model.RequestEnvelopeMessagesOneOf;
import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;
import com.salesforce.einsteinbot.sdk.model.TextMessage;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * ChatbotClientExamples - Contains example code showing how to use the SDK. This is not supposed to
 * be run as unit test. This class can be run using main method. You'll need to get the keyfile and
 * update the location in `clientKeyFilePath` below.
 */
public class ChatbotClientExamples {

  private final String basePath = "https://runtime-api-na-west.stg.chatbots.sfdc.sh";
  private final String externalSessionKey = "c58677cc-76e6-4174-9770-aee33b08384n";

  private final String integrationName = "ConnectorExample";

  private final String orgId = "00DSB0000001ThY2AU";
  private final String botId = "0XxSB00000006rp0AA";
  private final String forceConfigEndPoint = "https://esw5.test1.my.pc-rnd.salesforce.com";
  private final String loginEndpoint = "https://login.test1.pc-rnd.salesforce.com/";
  private final String connectedAppId = "3MVG9l3R9F9mHOGZUZs8TSRIINrHRklsp6OjPsKLQTUznlbLRyH_KMLfPG8SdPJugUtFa2UArLzpvtS74qDQ.";
  private final String secret = "1B57EFD4F6D22302A6D4FA9077430191CFFDFAEA22C6ABDA6FCB45993A8AD421";
  private final String userId = "admin1@esw5.sdb3";
  private AuthMechanism oAuth = new JwtBearerOAuth("src/test/resources/YourPrivateKey.der",
      loginEndpoint, connectedAppId, secret, userId, new InMemoryCache(300L));

  public static void main(String[] args) throws Exception {
    new ChatbotClientExamples().run();
  }

  private void run() {
    //   sendUsingBasicClient();
    sendUsingSessionManagedClient();
    // getHealthStatus();
  }

  private void sendUsingBasicClient() {
    ChatbotClient client = BasicChatbotClient.builder()
        .basePath(basePath)
        .authMechanism(oAuth)
        .build();

    RequestEnvelopeMessagesOneOf textMessage = buildInitMessage(Optional.of("Initial message"));
    RequestEnvelope envelope = buildRequestEnvelop(externalSessionKey, orgId, botId,
        forceConfigEndPoint, Arrays.asList(textMessage));
    RequestHeaders headers = RequestHeaders.builder()
        .orgId(orgId)
        .build();
    ResponseEnvelope resp = client.sendChatbotRequest(envelope, headers);

    System.out.println(resp);
  }

  private void sendUsingSessionManagedClient() {

    SessionManagedChatbotClient client = SessionManagedChatbotClient
        .builder()
        .basicClient(BasicChatbotClient.builder()
            .basePath(basePath)
            .authMechanism(oAuth)
            .build())
        .integrationName(integrationName)
        .cache(new InMemoryCache(600))
        .build();

    RequestEnvelopeMessagesOneOf textMessage = buildTextMessage("Initial message");
    RequestEnvelope envelope = buildRequestEnvelop(externalSessionKey, orgId, botId,
        forceConfigEndPoint, Arrays.asList(textMessage));

    RequestHeaders headers = RequestHeaders.builder()
        .orgId(orgId)
        .build();

    ResponseEnvelope resp = client.sendChatbotRequest(envelope, headers);

    System.out.println(resp);
  }

  public static RequestEnvelopeMessagesOneOf buildTextMessage(String msg) {
    return new TextMessage()
        .text(msg)
        .type(TextMessage.TypeEnum.TEXT)
        .sequenceId(System.currentTimeMillis());
  }

  public static RequestEnvelopeMessagesOneOf buildInitMessage(Optional<String> msg) {
    return new InitMessage()
        .text(msg.orElse(""))
        .type(InitMessage.TypeEnum.INIT)
        .sequenceId(System.currentTimeMillis());
  }

  public static RequestEnvelope buildRequestEnvelop(String sessionId,
      String orgId, String botId,
      String forceConfigEndPoint,
      List<RequestEnvelopeMessagesOneOf> messages) {
    return new RequestEnvelope()
        .externalSessionKey(sessionId)
        .botId(botId)
        .forceConfig(new ForceConfig().endpoint(forceConfigEndPoint))
        .messages(messages);
  }

  /*private void getHealthStatus() {
    ChatbotClient client = BasicChatbotClient.builder()
        .basePath(basePath)
        .authMechanism(oAuth)
        .build();

    System.out.println(client.getHealthStatus());
  }*/
}
