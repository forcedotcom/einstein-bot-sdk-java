/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.examples;

import static com.salesforce.einsteinbot.sdk.client.util.RequestFactory.buildTextMessage;
import static com.salesforce.einsteinbot.sdk.util.UtilFunctions.convertObjectToJson;

import com.salesforce.einsteinbot.sdk.auth.AuthMechanism;
import com.salesforce.einsteinbot.sdk.auth.JwtBearerOAuth;
import com.salesforce.einsteinbot.sdk.cache.InMemoryCache;
import com.salesforce.einsteinbot.sdk.client.BasicChatbotClient;
import com.salesforce.einsteinbot.sdk.client.ChatbotClients;
import com.salesforce.einsteinbot.sdk.client.SessionManagedChatbotClient;
import com.salesforce.einsteinbot.sdk.client.model.BotEndSessionRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotResponse;
import com.salesforce.einsteinbot.sdk.client.model.BotSendMessageRequest;
import com.salesforce.einsteinbot.sdk.model.EndSessionReason;
import com.salesforce.einsteinbot.sdk.client.model.ExternalSessionId;
import com.salesforce.einsteinbot.sdk.client.model.RequestConfig;
import com.salesforce.einsteinbot.sdk.client.model.BotRequest;
import com.salesforce.einsteinbot.sdk.client.model.RuntimeSessionId;


/**
 * ChatbotClientExamples - Contains example code showing how to use the SDK. This is not supposed to
 * be run as unit test. This class can be run using main method. You'll need to get the keyfile and
 * update the location in `clientKeyFilePath` below.
 *
 * @author relango
 */
public class ChatbotClientExamples {

  private final String basePath = "https://runtime-api-na-west.stg.chatbots.sfdc.sh";
  private final ExternalSessionId externalSessionKey = new ExternalSessionId("c58677cc-76e6-4174-9770-aee33b08384n");

  private final String integrationName = "ConnectorExample";

  private final String orgId = "00DSB0000001ThY2AU";
  private final String botId = "0XxSB00000006rp0AA";
  private final String forceConfigEndPoint = "https://esw5.test1.my.pc-rnd.salesforce.com";

  //Replace following variables with real values before running.
  private final String loginEndpoint = "SALESFORCE_LOGIN_END_POINT";
  private final String connectedAppId = "YOUR_CONNECTED_APP_ID";
  private final String secret = "YOUR_CONNECTED_APP_SECRET";
  private final String userId = "SALESFORCE_LOGIN_USER";
  private final String privateKeyFilePath = "src/test/resources/YourConnectedAppPrivateKey.der";

  private AuthMechanism oAuth = JwtBearerOAuth.with()
      .privateKeyFilePath(privateKeyFilePath)
      .loginEndpoint(loginEndpoint)
      .connectedAppId(connectedAppId)
      .connectedAppSecret(secret)
      .userId(userId)
      .cache(new InMemoryCache(300L))
      .build();

  private RequestConfig config = RequestConfig.with()
      .botId(botId)
      .orgId(orgId)
      .forceConfigEndpoint(forceConfigEndPoint)
      .build();

  public static void main(String[] args) throws Exception {
    new ChatbotClientExamples().run();
  }

  private void run() throws Exception{
    sendUsingBasicClient();
    sendUsingSessionManagedClient();
    getHealthStatus();
  }

  private void sendUsingBasicClient() throws Exception{
    BasicChatbotClient client = ChatbotClients.basic()
        .basePath(basePath)
        .authMechanism(oAuth)
        .build();

    BotSendMessageRequest botSendInitMessageRequest = BotRequest.withMessage(buildTextMessage("Initial Message")).build();

    BotResponse resp = client.startChatSession(config, externalSessionKey, botSendInitMessageRequest);

    System.out.println("Init Message Response :" + convertObjectToJson(resp));
    String sessionId = resp.getResponseEnvelope().getSessionId();
    BotSendMessageRequest botSendMessageRequest =  BotRequest.withMessage(buildTextMessage("Order Status")).build();

    BotResponse textMsgResponse = client
        .sendMessage(config, new RuntimeSessionId(sessionId), botSendMessageRequest);

    System.out.println("Text Message Response :" + convertObjectToJson(textMsgResponse));

    BotEndSessionRequest botEndSessionRequest = BotRequest.withEndSession(EndSessionReason.USERREQUEST).build();

    BotResponse endSessionResponse = client
        .endChatSession(config, new RuntimeSessionId(sessionId), botEndSessionRequest);

    System.out.println("End Session Response :" + convertObjectToJson(endSessionResponse));

  }

  private void sendUsingSessionManagedClient() throws Exception {

    SessionManagedChatbotClient client = ChatbotClients
        .sessionManaged()
        .basicClient(ChatbotClients.basic()
            .basePath(basePath)
            .authMechanism(oAuth)
            .build())
        .cache(new InMemoryCache(600))
        .integrationName(integrationName)
        .build();

    BotSendMessageRequest botSendFirstMessageRequest = BotRequest.withMessage(buildTextMessage("Initial Message")).build();

    BotResponse firstMsgResp = client.sendMessage(config, externalSessionKey, botSendFirstMessageRequest);

    System.out.println("First Message Response: " + convertObjectToJson(firstMsgResp));

    BotSendMessageRequest botSendSecondMessageRequest =  BotRequest.withMessage(buildTextMessage("Order Status")).build();

    BotResponse secondMsgResp = client.sendMessage(config, externalSessionKey, botSendSecondMessageRequest);

    System.out.println("Second Message Response: " + convertObjectToJson(secondMsgResp));

    BotEndSessionRequest botEndSessionRequest = BotRequest.withEndSession(EndSessionReason.USERREQUEST).build();

    BotResponse endSessionResponse = client
        .endChatSession(config, externalSessionKey, botEndSessionRequest);

    System.out.println("End Session Response :" + convertObjectToJson(endSessionResponse));
  }

  private void getHealthStatus() throws Exception{
    BasicChatbotClient client = ChatbotClients.basic()
        .basePath(basePath)
        .authMechanism(oAuth)
        .build();

    System.out.println("Health Status: " + convertObjectToJson(client.getHealthStatus()));
  }
}
