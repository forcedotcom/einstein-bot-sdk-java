/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.examples;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.salesforce.einsteinbot.sdk.api.MessagesApi;
import com.salesforce.einsteinbot.sdk.handler.ApiClient;
import com.salesforce.einsteinbot.sdk.model.AnyVariable;
import com.salesforce.einsteinbot.sdk.model.InitMessage;
import com.salesforce.einsteinbot.sdk.model.Referrer;
import com.salesforce.einsteinbot.sdk.model.RequestEnvelope;
import com.salesforce.einsteinbot.sdk.model.AnyRequestMessage;
import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;
import com.salesforce.einsteinbot.sdk.model.TextVariable;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import reactor.core.publisher.Mono;

/**
 * SdkExamples - Contains example code showing how to use SDK. This is not supposed to be run as
 * unit test. This class can be using main method but uses Runtime API service running on
 * localhost:3000.
 *
 * @author relango
 */
public class SdkExamples {

  private static final String BASE_PATH = "http://localhost:3000";
  //private HealthApi healthApi;
  private MessagesApi messagesApi;

  public SdkExamples() {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(BASE_PATH);
    // healthApi = new HealthApi(apiClient);
    messagesApi = new MessagesApi(apiClient);
  }

    /*
    public void pingExample() throws Exception {

        Mono<String> response = healthApi.pingGet();

        //Reactive Example using Spring Reactor
        response
          .doOnNext(v -> System.out.println("Response from pingExampleUsingReactor " + v))
          .doOnError(Throwable::printStackTrace)
          .subscribe();

        //Using CompletableFuture
        CompletableFuture<String> futureResponse = healthApi.pingGet().toFuture();

        futureResponse
          .thenAccept(v -> System.out.println("Response from pingExampleUsingFuture " + v))
          .exceptionally(t -> {
              t.printStackTrace();
              return null;
          });

        // Just to give time to execute before main terminates.
        // Don't use it in production code.
        Thread.sleep(5000);
    }

    public void statusExample() throws Exception {

        CompletableFuture<Status> response = healthApi.statusGet().toFuture();

        response.thenAccept( v ->
          System.out.println("Response from statusExample " + v))
          .exceptionally(t -> {
              t.printStackTrace();
              return null;
          });
        Thread.sleep(2000);
    }*/

  public void sendMessagesExample() throws Exception {
    String xOrgId = "00Dxx0000006GprEAE";
    String xRequestID = "08c38dcf-af09-4d96-899f-6247052d6f00";
    RequestEnvelope requestEnvelope = createRequestEnvelope();
    String xBotMode = null;
    String xRuntimeCRC = null;
    String X_SCRT_AUTHORIZATION = "\"\"";
    String X_SCRT_VERSION = "\"\"";
    String authorization = "\"\"";

    //Reactive Example using Spring Reactor
    Mono<ResponseEnvelope> response = messagesApi
        .sendMessages(xOrgId, xRequestID, requestEnvelope, xRuntimeCRC);

    response
        .doOnNext(v -> System.out.println("Response from sendMessagesExampleUsingReactor " + v))
        .doOnError(Throwable::printStackTrace)
        .subscribe();

    //Using CompletableFuture
    CompletableFuture<ResponseEnvelope> futureResponse = messagesApi
        .sendMessages(xOrgId, xRequestID, requestEnvelope, xRuntimeCRC).toFuture();

    futureResponse
        .thenAccept(v -> System.out.println("Response from sendMessagesExampleUsingFuture " + v))
        .exceptionally(t -> {
          t.printStackTrace();
          return null;
        });

    // Just to give time to execute before main terminates.
    // Don't use it in production code.
    Thread.sleep(5000);
  }

  private RequestEnvelope createRequestEnvelope() throws JsonProcessingException {

    AnyVariable variable = new TextVariable().type(TextVariable.TypeEnum.TEXT).name("textVar")
        .value("textVal");
    Referrer referrer = new Referrer().type(Referrer.TypeEnum.BOTRUNTIME_SESSION_ID)
        .value("session-id");
    AnyRequestMessage message = new InitMessage()
        .type(InitMessage.TypeEnum.INIT)
        .text("Hi")
        .sequenceId(1L)
        .variables(Arrays.asList(variable))
        .referrers(Arrays.asList(referrer));

    RequestEnvelope requestEnvelope = new RequestEnvelope()
        .sessionId("175e31d1-3ba8-4f07-bf25-924ebe3a7ce8")
        .externalSessionKey("175e31d1-3ba8-4f07-bf25-924ebe3a7ce8")
        .botId("0Xxxx0000000001CAA")
        .messages(Arrays.asList(message));

    return requestEnvelope;
  }

  public static void main(String[] args) throws Exception {
    //You need to have chatbot runtime running on locahost:3000 to run these examples.
    new SdkExamples().run();
  }

  private void run() throws Exception {
    //  pingExample();
    sendMessagesExample();
    // statusExample();
  }
}
