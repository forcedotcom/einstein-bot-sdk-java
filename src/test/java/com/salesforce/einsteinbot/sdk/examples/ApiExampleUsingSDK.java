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
import com.salesforce.einsteinbot.sdk.client.model.BotRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotResponse;
import com.salesforce.einsteinbot.sdk.client.model.BotSendMessageRequest;
import com.salesforce.einsteinbot.sdk.client.model.ExternalSessionId;
import com.salesforce.einsteinbot.sdk.client.model.RequestConfig;
import com.salesforce.einsteinbot.sdk.client.model.RuntimeSessionId;
import com.salesforce.einsteinbot.sdk.model.AnyRequestMessage;
import com.salesforce.einsteinbot.sdk.model.EndSessionReason;
import com.salesforce.einsteinbot.sdk.model.TextMessage;
import com.salesforce.einsteinbot.sdk.util.UtilFunctions;


/**
 * ApiExampleUsingSDK - Contains example code showing how to use the SDK. This is not supposed to
 * be run as unit test. This class can be run using main method. You'll need to get the keyfile and
 * update the location in `clientKeyFilePath` below.
 *
 * @author relango
 */
public class ApiExampleUsingSDK {

  private final String basePath = "https://runtime-api-na-west.stg.chatbots.sfdc.sh";

  private final String orgId = "00DSB0000001ThY2AU";
  private final String botId = "0XxSB00000006rp0AA";
  private final String forceConfigEndPoint = "https://esw5.test1.my.pc-rnd.salesforce.com";

  //Replace following variables with real values before running.
  private final String loginEndpoint = "SALESFORCE_LOGIN_END_POINT";
  private final String connectedAppId = "YOUR_CONNECTED_APP_ID";
  private final String secret = "YOUR_CONNECTED_APP_SECRET";
  private final String userId = "SALESFORCE_LOGIN_USER";
  private final String privateKeyFilePath = "src/test/resources/YourConnectedAppPrivateKey.der";

  public static void main(String[] args) throws Exception {
    new ApiExampleUsingSDK().run();
  }

  private void run() throws Exception{
    sendUsingBasicClient();
  }

  private void sendUsingBasicClient() throws Exception{

    //1. Create JwtBearer Auth Mechanism.
    AuthMechanism oAuth = new JwtBearerOAuth(privateKeyFilePath,
        loginEndpoint, connectedAppId, secret, userId, new InMemoryCache(300L));

    //2. Create Chatbot Client
    BasicChatbotClient client = ChatbotClients.basic()
        .basePath(basePath)
        .authMechanism(oAuth)
        .build();

    //3. Create Request Config
    RequestConfig config = RequestConfig.with()
        .botId(botId)
        .orgId(orgId)
        .forceConfigEndpoint(forceConfigEndPoint)
        .build();

    //4. Bot Send Message Request
    AnyRequestMessage message = new TextMessage()
        .text("Hello")
        .type(TextMessage.TypeEnum.TEXT)
        .sequenceId(System.currentTimeMillis());

    BotSendMessageRequest botSendInitMessageRequest = BotRequest
        .withMessage(message)
        .build();

    //5. Send Request to Start Chat Session.
    ExternalSessionId externalSessionKey = new ExternalSessionId(UtilFunctions.newRandomUUID());
    BotResponse resp = client.startChatSession(config, externalSessionKey, botSendInitMessageRequest);

    System.out.println("Init Message Response :" + convertObjectToJson(resp));

    // Get SessionId from Response.
    String sessionId = resp.getResponseEnvelope().getSessionId();

    // Build Bot Send Message Request
    BotSendMessageRequest botSendMessageRequest =  BotRequest
        .withMessage(buildTextMessage("Order Status"))
        .build();

    // Send a message to existing Session
    BotResponse textMsgResponse = client
        .sendMessage(config, new RuntimeSessionId(sessionId), botSendMessageRequest);

    System.out.println("Text Message Response :" + convertObjectToJson(textMsgResponse));

    // Build Bot End Session Message Request
    BotEndSessionRequest botEndSessionRequest = BotRequest
        .withEndSession(EndSessionReason.USERREQUEST).build();

    // Send Request to End Chat session
    BotResponse endSessionResponse = client
        .endChatSession(config, new RuntimeSessionId(sessionId), botEndSessionRequest);

    System.out.println("End Session Response :" + convertObjectToJson(endSessionResponse));

  }
}
