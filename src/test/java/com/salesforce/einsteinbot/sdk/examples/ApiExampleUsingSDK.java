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
import com.salesforce.einsteinbot.sdk.client.SessionManagedChatbotClient;
import com.salesforce.einsteinbot.sdk.client.model.BotEndSessionRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotRequest;
import com.salesforce.einsteinbot.sdk.client.model.BotResponse;
import com.salesforce.einsteinbot.sdk.client.model.BotSendMessageRequest;
import com.salesforce.einsteinbot.sdk.client.model.ExternalSessionId;
import com.salesforce.einsteinbot.sdk.client.model.RequestConfig;
import com.salesforce.einsteinbot.sdk.client.model.RuntimeSessionId;
import com.salesforce.einsteinbot.sdk.client.util.RequestFactory;
import com.salesforce.einsteinbot.sdk.exception.ChatbotResponseException;
import com.salesforce.einsteinbot.sdk.model.AnyRequestMessage;
import com.salesforce.einsteinbot.sdk.model.AnyResponseMessage;
import com.salesforce.einsteinbot.sdk.model.AnyVariable;
import com.salesforce.einsteinbot.sdk.model.ChoiceMessage;
import com.salesforce.einsteinbot.sdk.model.ChoicesResponseMessage;
import com.salesforce.einsteinbot.sdk.model.ChoicesResponseMessageChoicesInner;
import com.salesforce.einsteinbot.sdk.model.EndSessionReason;
import com.salesforce.einsteinbot.sdk.model.EscalateResponseMessage;
import com.salesforce.einsteinbot.sdk.model.EscalateResponseMessageTargetsInner;
import com.salesforce.einsteinbot.sdk.model.SessionEndedResponseMessage;
import com.salesforce.einsteinbot.sdk.model.SessionEndedResponseMessage.ReasonEnum;
import com.salesforce.einsteinbot.sdk.model.Status;
import com.salesforce.einsteinbot.sdk.model.Status.StatusEnum;
import com.salesforce.einsteinbot.sdk.model.TextResponseMessage;
import com.salesforce.einsteinbot.sdk.model.TextVariable;
import com.salesforce.einsteinbot.sdk.model.TextVariable.TypeEnum;
import com.salesforce.einsteinbot.sdk.model.TransferFailedRequestMessage;
import com.salesforce.einsteinbot.sdk.model.TransferSucceededRequestMessage;
import com.salesforce.einsteinbot.sdk.util.UtilFunctions;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


/**
 * ApiExampleUsingSDK - Contains example code showing how to use the SDK. This is fully working
 * example for code snippets used in SDK client user guide.
 * <p>
 * This is not supposed to be run as unit test. This class can be run using main method.
 *
 * @author relango
 */
public class ApiExampleUsingSDK {

  private final String basePath = "https://runtime-api-na-west.stg.chatbots.sfdc.sh";

  private final String orgId = "00DSB0000001ThY2AU";
  private final String botId = "0XxSB00000007UX0AY";
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

  private void run() throws Exception {
    sendUsingBasicClient();
    sendUsingSessionManagedClient();
    getHealthStatus();
  }

  private void sendUsingBasicClient() throws Exception {

    //1. Create JwtBearer Auth Mechanism.
    AuthMechanism oAuth = JwtBearerOAuth.with()
        .privateKeyFilePath(privateKeyFilePath)
        .loginEndpoint(loginEndpoint)
        .connectedAppId(connectedAppId)
        .connectedAppSecret(secret)
        .userId(userId)
        .build();

    //2. Create Chatbot Client
    BasicChatbotClient client = ChatbotClients.basic()
        .basePath(basePath)
        .authMechanism(oAuth)
        .build();

    //3. Create Request Config
    RequestConfig config = createRequestConfig();

    String sessionId = sendStartChatSession(client, config);
    sessionId = sendStartChatSessionWithOptionalFields(client, config);

    sendTextMessageChoiceAlias(client, config, sessionId);
    sendChoiceMessageWithIndex(client, config, sessionId);
    sendTextMessage(client, config, sessionId);
    sendTransferSuccessMessage(client, config, sessionId);
    sendTransferFailureMessage(client, config, sessionId);
    sendEndSessionMessage(client, config, sessionId);
    sendMessageWithErrorHandling(client, config, sessionId);
  }

  private void sendUsingSessionManagedClient() throws Exception {

    //1. Create JwtBearer Auth Mechanism.
    AuthMechanism oAuth = JwtBearerOAuth.with()
        .privateKeyFilePath(privateKeyFilePath)//TODO
        .loginEndpoint(loginEndpoint)
        .connectedAppId(connectedAppId)
        .connectedAppSecret(secret)
        .userId(userId)
        .build();

    //2. Create Session Managed Client
    SessionManagedChatbotClient client = ChatbotClients
        .sessionManaged()
        .basicClient(ChatbotClients.basic()
            .basePath(basePath)
            .authMechanism(oAuth)
            .build())
        .cache(new InMemoryCache(600))
        .build();

    //3. Create Request Config
    RequestConfig config = createRequestConfig();

    ExternalSessionId externalSessionKey = new ExternalSessionId(UUID.randomUUID().toString());

    //4. When you sent a message for first time with new externalSessionKey,
    // a new chat session will be automatically started
    sendMessageUsingSessionManagedClient(client, config, externalSessionKey, "Hello");

    //5. When you sent a message with same externalSessionKey,
    // it will send message to existing session associated with externalSessionKey.
    sendMessageUsingSessionManagedClient(client, config, externalSessionKey, "Order Status");

    //6. For Ending session, use same externalSessionKey
    sendEndSessionMessageUsingSessionManagedClient(client, config, externalSessionKey);
  }

  private void sendMessageUsingSessionManagedClient(SessionManagedChatbotClient client,
      RequestConfig config,
      ExternalSessionId externalSessionKey, String message) throws JsonProcessingException {

    BotSendMessageRequest botSendFirstMessageRequest = BotRequest
        .withMessage(buildTextMessage(message))
        .build();

    BotResponse firstMsgResp = client
        .sendMessage(config, externalSessionKey, botSendFirstMessageRequest);

    System.out
        .println("Response for message " + message + ": " + convertObjectToJson(firstMsgResp));
    System.out.println("Response for message as Text : \n" + getResponseMessageAsText(
        firstMsgResp.getResponseEnvelope().getMessages()));
  }

  private void sendEndSessionMessage(BasicChatbotClient client, RequestConfig config,
      String sessionId)
      throws JsonProcessingException {
    // Build Bot End Session Message Request
    BotEndSessionRequest botEndSessionRequest = BotRequest
        .withEndSession(EndSessionReason.USER_REQUEST).build();

    // Send Request to End Chat session
    BotResponse endSessionResponse = client
        .endChatSession(config, new RuntimeSessionId(sessionId), botEndSessionRequest);

    System.out.println("End Session Response :" + convertObjectToJson(endSessionResponse));
    System.out.println("End Session Response as Text : \n" + getResponseMessageAsText(
        endSessionResponse.getResponseEnvelope().getMessages()));
  }

  private void sendMessageWithErrorHandling(BasicChatbotClient client, RequestConfig config,
      String sessionId) throws JsonProcessingException {
    // Build Bot End Session Message Request
    BotEndSessionRequest botEndSessionRequest = BotRequest
        .withEndSession(EndSessionReason.USER_REQUEST).build();

    // Send Request to End Chat session with invalid session Id and handle error.
    RuntimeSessionId invalidSessionId = new RuntimeSessionId("invalid session id");
    try {
      BotResponse endSessionResponse = client
          .endChatSession(config, invalidSessionId, botEndSessionRequest);
      System.out.println("End Session Response :" + convertObjectToJson(endSessionResponse));
    } catch (Exception e) {
      //Actual Error is wrapped in RuntimeException and ExecutionException
      ChatbotResponseException chatbotResponseException = ((ChatbotResponseException) e.getCause()
          .getCause());
      System.out.println("Error Http Status Code : " + chatbotResponseException.getStatus());
      System.out.println("Error Payload : " + chatbotResponseException.getErrorResponse());
    }
  }

  private void sendEndSessionMessageUsingSessionManagedClient(SessionManagedChatbotClient client,
      RequestConfig config, ExternalSessionId externalSessionId)
      throws JsonProcessingException {
    // Build Bot End Session Message Request
    BotEndSessionRequest botEndSessionRequest = BotRequest
        .withEndSession(EndSessionReason.USER_REQUEST).build();

    // Send Request to End Chat session
    BotResponse endSessionResponse = client
        .endChatSession(config, externalSessionId, botEndSessionRequest);

    System.out.println("End Session Response :" + convertObjectToJson(endSessionResponse));
    System.out.println("End Session Response as Text : \n" + getResponseMessageAsText(
        endSessionResponse.getResponseEnvelope().getMessages()));
  }

  private void sendTextMessage(BasicChatbotClient client, RequestConfig config, String sessionId)
      throws JsonProcessingException {
    // Build Bot Send Message Request
    BotSendMessageRequest botSendMessageRequest = BotRequest
        .withMessage(RequestFactory.buildTextMessage("Transfer To Agent"))
        .build();

    // Send a message to existing Session
    BotResponse textMsgResponse = client
        .sendMessage(config, new RuntimeSessionId(sessionId), botSendMessageRequest);

    System.out
        .println("Transfer To Agent Message Response :" + convertObjectToJson(textMsgResponse));
    System.out.println("Transfer To Agent Message Response as Text : \n" + getResponseMessageAsText(
        textMsgResponse.getResponseEnvelope().getMessages()));

  }

  private void sendTextMessageChoiceAlias(BasicChatbotClient client, RequestConfig config,
      String sessionId)
      throws JsonProcessingException {
    // Build Bot Send Message Request
    BotSendMessageRequest botSendMessageRequest = BotRequest
        .withMessage(RequestFactory.buildTextMessage("1"))
        .build();

    // Send a message to existing Session
    BotResponse textMsgResponse = client
        .sendMessage(config, new RuntimeSessionId(sessionId), botSendMessageRequest);

    System.out.println(
        "Text Message With Choice Alias Response :" + convertObjectToJson(textMsgResponse));
    System.out.println(
        "Text Message With Choice Alias Response as Text : \n" + getResponseMessageAsText(
            textMsgResponse.getResponseEnvelope().getMessages()));
  }

  private String sendStartChatSession(BasicChatbotClient client, RequestConfig config)
      throws JsonProcessingException {

    AnyRequestMessage message = buildTextMessage("Hello");

    BotSendMessageRequest botSendInitMessageRequest = BotRequest
        .withMessage(message)
        .build();

    //5. Send Request to Start Chat Session.
    ExternalSessionId externalSessionKey = new ExternalSessionId(UtilFunctions.newRandomUUID());
    BotResponse resp = client
        .startChatSession(config, externalSessionKey, botSendInitMessageRequest);

    System.out.println("Init Message Response :" + convertObjectToJson(resp));
    System.out.println("Init Message Response as Text : \n" + getResponseMessageAsText(
        resp.getResponseEnvelope().getMessages()));

    return resp.getResponseEnvelope().getSessionId();
  }

  private String sendStartChatSessionWithOptionalFields(BasicChatbotClient client,
      RequestConfig config)
      throws JsonProcessingException {

    AnyRequestMessage message = buildTextMessage("Hello");

    List<AnyVariable> variables = Collections
        .singletonList(new TextVariable()
            .name("CustomerName")
            .type(TypeEnum.TEXT)
            .value("YourName") //TODO
        );

    BotSendMessageRequest botSendInitMessageRequest = BotRequest
        .withMessage(message)
        .variables(variables)
        .requestId(UUID.randomUUID().toString())
        .build();

    //5. Send Request to Start Chat Session.
    ExternalSessionId externalSessionKey = new ExternalSessionId(UtilFunctions.newRandomUUID());
    BotResponse resp = client
        .startChatSession(config, externalSessionKey, botSendInitMessageRequest);

    System.out.println("Init Message With Optional fields Response :" + convertObjectToJson(resp));
    System.out.println(
        "Init Message With Optional fields Response as Text : \n" + getResponseMessageAsText(
            resp.getResponseEnvelope().getMessages()));

    return resp.getResponseEnvelope().getSessionId();
  }

  private void sendChoiceMessageWithIndex(BasicChatbotClient client, RequestConfig config,
      String sessionId)
      throws JsonProcessingException {

    // Build Bot Send Message Request
    BotSendMessageRequest botSendMessageRequest = BotRequest
        .withMessage(buildChoiceMessageWithIndex(1))
        .build();

    // Send a message to existing Session
    BotResponse response = client
        .sendMessage(config, new RuntimeSessionId(sessionId), botSendMessageRequest);

    System.out.println("Choice With Index Message Response :" + convertObjectToJson(response));
    System.out.println("Choice With Index Message Response as Text : \n" + getResponseMessageAsText(
        response.getResponseEnvelope().getMessages()));
  }

  private void sendChoiceMessageWithId(BasicChatbotClient client, RequestConfig config,
      String sessionId)
      throws JsonProcessingException {

    // Build Bot Send Message Request
    BotSendMessageRequest botSendMessageRequest = BotRequest
        .withMessage(buildChoiceMessageWithId("da83a84f-1325-4d37-80df-5a4931284961"))
        //ChoiceId of Frequently Asked Question Option from bot's response payload.
        .build();

    // Send a message to existing Session
    BotResponse response = client
        .sendMessage(config, new RuntimeSessionId(sessionId), botSendMessageRequest);

    System.out.println("Choice With Id Message Response :" + convertObjectToJson(response));
    System.out.println("Choice With Id Message Response as Text : \n" + getResponseMessageAsText(
        response.getResponseEnvelope().getMessages()));
  }

  private void sendTransferSuccessMessage(BasicChatbotClient client, RequestConfig config,
      String sessionId)
      throws JsonProcessingException {

    // Build Bot Send Message Request
    BotSendMessageRequest botSendMessageRequest = BotRequest
        .withMessage(buildTransferSuccessMessage())
        .build();

    // Send a message to existing Session
    BotResponse response = client
        .sendMessage(config, new RuntimeSessionId(sessionId), botSendMessageRequest);

    System.out.println("Transfer Success Message Response :" + convertObjectToJson(response));
    System.out.println("Transfer Success Message Response as Text : \n" + getResponseMessageAsText(
        response.getResponseEnvelope().getMessages()));
  }

  private void sendTransferFailureMessage(BasicChatbotClient client, RequestConfig config,
      String sessionId)
      throws JsonProcessingException {

    // Build Bot Send Message Request
    BotSendMessageRequest botSendMessageRequest = BotRequest
        .withMessage(buildTransferFailedMessage())
        .build();

    // Send a message to existing Session
    BotResponse response = client
        .sendMessage(config, new RuntimeSessionId(sessionId), botSendMessageRequest);

    System.out.println("Transfer Failure Message Response :" + convertObjectToJson(response));
    System.out.println("Transfer Failure Message Response as Text : \n" + getResponseMessageAsText(
        response.getResponseEnvelope().getMessages()));
  }

  private void getHealthStatus() throws Exception {

    //1. Create JwtBearer Auth Mechanism.
    AuthMechanism oAuth = JwtBearerOAuth.with()
        .privateKeyFilePath(privateKeyFilePath)//todo
        .loginEndpoint(loginEndpoint)
        .connectedAppId(connectedAppId)
        .connectedAppSecret(secret)
        .userId(userId)
        .build();

    //2. Create Chatbot Client
    BasicChatbotClient client = ChatbotClients.basic()
        .basePath(basePath)
        .authMechanism(oAuth)
        .build();

    Status status = client.getHealthStatus();
    boolean isRuntimeUp = status.equals(StatusEnum.UP);
    System.out.println("Health Status: " + convertObjectToJson(client.getHealthStatus()));
  }

  private String getResponseMessageAsText(List<AnyResponseMessage> messages) {
    StringBuilder sb = new StringBuilder();
    for (AnyResponseMessage message : messages) {
      if (message instanceof TextResponseMessage) {
        sb.append(((TextResponseMessage) message).getText())
            .append("\n");
      } else if (message instanceof ChoicesResponseMessage) {
        List<ChoicesResponseMessageChoicesInner> choices = ((ChoicesResponseMessage) message)
            .getChoices();
        for (ChoicesResponseMessageChoicesInner choice : choices) {
          sb.append(choice.getAlias())
              .append(".")
              .append(choice.getLabel())
              .append("\n");
        }
      } else if (message instanceof EscalateResponseMessage) {
        List<EscalateResponseMessageTargetsInner> targets = ((EscalateResponseMessage) message)
            .getTargets();
        //Implement your code to actually do the transfer.
        sb.append("Transferring you to Agent in Target : " + targets);

      } else if (message instanceof SessionEndedResponseMessage) {
        ReasonEnum reason = ((SessionEndedResponseMessage) message)
            .getReason();
        //Implement logic to do any clean on the channel.
        sb.append("The Chat session ended with Reason : " + reason);

      }
    }
    return sb.toString();
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
        .type(TransferSucceededRequestMessage.TypeEnum.TRANSFER_SUCCEEDED)
        .sequenceId(System.currentTimeMillis());
  }

  public static AnyRequestMessage buildTransferFailedMessage() {
    return new TransferFailedRequestMessage()
        .type(TransferFailedRequestMessage.TypeEnum.TRANSFER_FAILED)
        .reason(TransferFailedRequestMessage.ReasonEnum.NO_AGENT_AVAILABLE)
        .sequenceId(System.currentTimeMillis());
  }
}