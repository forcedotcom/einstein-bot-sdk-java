/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.examples;

import static com.salesforce.einsteinbot.sdk.client.util.RequestFactory.buildTextMessage;
import static com.salesforce.einsteinbot.sdk.util.UtilFunctions.convertObjectToJson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.salesforce.einsteinbot.sdk.auth.AuthMechanism;
import com.salesforce.einsteinbot.sdk.auth.JwtBearerOAuth;
import com.salesforce.einsteinbot.sdk.cache.InMemoryCache;
import com.salesforce.einsteinbot.sdk.client.BasicChatbotClient;
import com.salesforce.einsteinbot.sdk.client.ChatbotClients;
import com.salesforce.einsteinbot.sdk.client.model.BotEndSessionRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotResponse;
import com.salesforce.einsteinbot.sdk.client.model.BotSendMessageRequest;
import com.salesforce.einsteinbot.sdk.client.model.ExternalSessionId;
import com.salesforce.einsteinbot.sdk.client.model.RequestConfig;
import com.salesforce.einsteinbot.sdk.client.model.RuntimeSessionId;
import com.salesforce.einsteinbot.sdk.client.util.RequestFactory;
import com.salesforce.einsteinbot.sdk.model.AnyRequestMessage;
import com.salesforce.einsteinbot.sdk.model.AnyVariable;
import com.salesforce.einsteinbot.sdk.model.ChoiceMessage;
import com.salesforce.einsteinbot.sdk.model.EndSessionReason;
import com.salesforce.einsteinbot.sdk.model.TextVariable;
import com.salesforce.einsteinbot.sdk.model.TextVariable.TypeEnum;
import com.salesforce.einsteinbot.sdk.model.TransferFailedRequestMessage;
import com.salesforce.einsteinbot.sdk.model.TransferSucceededRequestMessage;
import com.salesforce.einsteinbot.sdk.util.UtilFunctions;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;


/**
 * ApiExampleUsingSDK - Contains example code showing how to use the SDK. This is not supposed to
 * be run as unit test. This class can be run using main method. You'll need to get the keyfile and
 * update the location in `clientKeyFilePath` below.
 *
 * @author relango
 */
public class ApiExampleForUserGuide {

  private final String basePath = "https://runtime-api-na-west.stg.chatbots.sfdc.sh";

  private final String orgId = "00DSB0000001ThY2AU";
  private final String botId = "0XxSB00000006rp0AA";
  private final String forceConfigEndPoint = "https://esw5.test1.my.pc-rnd.salesforce.com";

  //Replace following variables with real values before running.
  /*private final String loginEndpoint = "SALESFORCE_LOGIN_END_POINT";
  private final String connectedAppId = "YOUR_CONNECTED_APP_ID";
  private final String secret = "YOUR_CONNECTED_APP_SECRET";
  private final String userId = "SALESFORCE_LOGIN_USER";
  private final String privateKeyFilePath = "src/test/resources/YourConnectedAppPrivateKey.der"; */ //TODO

  private final String loginEndpoint = "https://login.test1.pc-rnd.salesforce.com/";
  private final String connectedAppId = "3MVG9l3R9F9mHOGZUZs8TSRIINrHRklsp6OjPsKLQTUznlbLRyH_KMLfPG8SdPJugUtFa2UArLzpvtS74qDQ.";
  private final String userId = "admin1@esw5.sdb3";
  private final String secret = "1B57EFD4F6D22302A6D4FA9077430191CFFDFAEA22C6ABDA6FCB45993A8AD421";
  private final String privateKeyFilePath = "src/test/resources/PrivateKeyFalconTest1.der";

  public static void main(String[] args) throws Exception {
    new ApiExampleForUserGuide().run();
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
    RequestConfig config = createRequestConfig();

    String sessionId = sendStartChatSession(client, config);
    sessionId = sendStartChatSessionWithOptionalFields(client, config);

    sendTextMessage(client, config, sessionId);
    sendChoiceMessageWithIndex(client, config, sessionId);
    sendChoiceMessageWithId(client, config, sessionId);
    sendTransferSuccessMessage(client, config, sessionId);
    sendTransferFailureMessage(client, config, sessionId);
    sendEndSessionMessage(client, config, sessionId);

  }

  private void sendEndSessionMessage(BasicChatbotClient client, RequestConfig config, String sessionId)
      throws JsonProcessingException {
    // Build Bot End Session Message Request
    BotEndSessionRequest botEndSessionRequest = BotRequest
        .withEndSession(EndSessionReason.USERREQUEST).build();

    // Send Request to End Chat session
    BotResponse endSessionResponse = client
        .endChatSession(config, new RuntimeSessionId(sessionId), botEndSessionRequest);

    System.out.println("End Session Response :" + convertObjectToJson(endSessionResponse));
  }

  private void sendTextMessage(BasicChatbotClient client, RequestConfig config, String sessionId)
      throws JsonProcessingException {
    // Build Bot Send Message Request
    BotSendMessageRequest botSendMessageRequest =  BotRequest
        .withMessage(RequestFactory.buildTextMessage("Order Status"))
        .build();

    // Send a message to existing Session
    BotResponse textMsgResponse = client
        .sendMessage(config, new RuntimeSessionId(sessionId), botSendMessageRequest);

    System.out.println("Text Message Response :" + convertObjectToJson(textMsgResponse));
  }

  private String sendStartChatSession(BasicChatbotClient client, RequestConfig config)
      throws JsonProcessingException {

    AnyRequestMessage message = buildTextMessage("Hello");

    BotSendMessageRequest botSendInitMessageRequest = BotRequest
        .withMessage(message)
        .build();

    //5. Send Request to Start Chat Session.
    ExternalSessionId externalSessionKey = new ExternalSessionId(UtilFunctions.newRandomUUID());
    BotResponse resp = client.startChatSession(config, externalSessionKey, botSendInitMessageRequest);

    System.out.println("Init Message Response :" + convertObjectToJson(resp));

    return resp.getResponseEnvelope().getSessionId();
  }

  private String sendStartChatSessionWithOptionalFields(BasicChatbotClient client, RequestConfig config)
      throws JsonProcessingException {

    AnyRequestMessage message = buildTextMessage("Hello");

    List<AnyVariable> variables = Collections
        .singletonList(new TextVariable()
            .name("FirstName")
            .type(TypeEnum.TEXT)
            .value("YourName")
        );

    BotSendMessageRequest botSendInitMessageRequest = BotRequest
        .withMessage(message)
        .variables(variables)
        .requestId(UUID.randomUUID().toString())
        .build();

    //5. Send Request to Start Chat Session.
    ExternalSessionId externalSessionKey = new ExternalSessionId(UtilFunctions.newRandomUUID());
    BotResponse resp = client.startChatSession(config, externalSessionKey, botSendInitMessageRequest);

    System.out.println("Init Message With Optional fields Response :" + convertObjectToJson(resp));

    return resp.getResponseEnvelope().getSessionId();
  }

  private void sendChoiceMessageWithIndex(BasicChatbotClient client, RequestConfig config, String sessionId)
      throws JsonProcessingException {

    // Build Bot Send Message Request
    BotSendMessageRequest botSendMessageRequest =  BotRequest
        .withMessage(buildChoiceMessageWithIndex(1))
        .build();

    // Send a message to existing Session
    BotResponse response = client
        .sendMessage(config, new RuntimeSessionId(sessionId), botSendMessageRequest);

    System.out.println("Choice With Index Message Response :" + convertObjectToJson(response));
  }

  private void sendChoiceMessageWithId(BasicChatbotClient client, RequestConfig config, String sessionId)
      throws JsonProcessingException {

    // Build Bot Send Message Request
    BotSendMessageRequest botSendMessageRequest =  BotRequest
        .withMessage(buildChoiceMessageWithId("todo"))
        .build();

    // Send a message to existing Session
    BotResponse response = client
        .sendMessage(config, new RuntimeSessionId(sessionId), botSendMessageRequest);

    System.out.println("Choice With Id Message Response :" + convertObjectToJson(response));
  }

  private void sendTransferSuccessMessage(BasicChatbotClient client, RequestConfig config, String sessionId)
      throws JsonProcessingException {

    // Build Bot Send Message Request
    BotSendMessageRequest botSendMessageRequest =  BotRequest
        .withMessage(buildTransferSuccessMessage())
        .build();

    // Send a message to existing Session
    BotResponse response = client
        .sendMessage(config, new RuntimeSessionId(sessionId), botSendMessageRequest);

    System.out.println("Transfer Success Message Response :" + convertObjectToJson(response));
  }

  private void sendTransferFailureMessage(BasicChatbotClient client, RequestConfig config, String sessionId)
      throws JsonProcessingException {

    // Build Bot Send Message Request
    BotSendMessageRequest botSendMessageRequest =  BotRequest
        .withMessage(buildTransferFailedMessage())
        .build();

    // Send a message to existing Session
    BotResponse response = client
        .sendMessage(config, new RuntimeSessionId(sessionId), botSendMessageRequest);

    System.out.println("Transfer Failure Message Response :" + convertObjectToJson(response));
  }

  private RequestConfig createRequestConfig() {
    return RequestConfig.with()
        .botId(botId)
        .orgId(orgId)
        .forceConfigEndpoint(forceConfigEndPoint)
        .build();
  }

  public static AnyRequestMessage buildChoiceMessageWithIndex(int index) {
    return new ChoiceMessage()
        .type(ChoiceMessage.TypeEnum.CHOICE)
        .sequenceId(System.currentTimeMillis())
        .choiceIndex(index);
  }

  public static AnyRequestMessage buildChoiceMessageWithId(String choiceId) {
    return new ChoiceMessage()
        .type(ChoiceMessage.TypeEnum.CHOICE)
        .sequenceId(System.currentTimeMillis())
        .choiceId(choiceId);
  }

  public static AnyRequestMessage buildTransferSuccessMessage() {
    return new TransferSucceededRequestMessage()
        .type(TransferSucceededRequestMessage.TypeEnum.TRANSFERSUCCEEDED)
        .sequenceId(System.currentTimeMillis());
  }

  public static AnyRequestMessage buildTransferFailedMessage() {
    return new TransferFailedRequestMessage()
        .type(TransferFailedRequestMessage.TypeEnum.TRANSFERFAILED)
        .reason(TransferFailedRequestMessage.ReasonEnum.NOAGENTAVAILABLE)
        .sequenceId(System.currentTimeMillis());
  }
}
