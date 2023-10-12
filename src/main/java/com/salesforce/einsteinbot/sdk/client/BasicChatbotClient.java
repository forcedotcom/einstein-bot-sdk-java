/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import com.salesforce.einsteinbot.sdk.auth.AuthMechanism;
import com.salesforce.einsteinbot.sdk.client.model.BotResponse;
import com.salesforce.einsteinbot.sdk.client.model.BotSendMessageRequest;
import com.salesforce.einsteinbot.sdk.client.model.ExternalSessionId;
import com.salesforce.einsteinbot.sdk.client.model.RequestConfig;
import com.salesforce.einsteinbot.sdk.client.model.RuntimeSessionId;
import com.salesforce.einsteinbot.sdk.model.Status;
import java.util.Objects;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * BasicChatbotClient - Interface BasicChatbotClient which provides methods to start chat session,
 * send message , end chat session and get health status.
 *
 * @author relango
 */
public interface BasicChatbotClient extends ChatbotClient<RuntimeSessionId> {

  BotResponse startChatSession(RequestConfig config,
      ExternalSessionId sessionId,
      BotSendMessageRequest requestEnvelope);

  Status getHealthStatus();

  /**
   * BasicClientFluentBuilder provides Fluent API to create Basic Chatbot Client.
   */
  class BasicClientFluentBuilder implements BasePathBuilder, AuthMechanismBuilder,
      BasicClientFinalBuilder {

    protected String basePath;
    protected AuthMechanism authMechanism;
    protected WebClient.Builder webClientBuilder = WebClient.builder();

    protected BasicClientFluentBuilder() {
    }

    public BasicClientFluentBuilder basePath(String basePath) {
      this.basePath = basePath;
      return this;
    }

    public BasicClientFluentBuilder authMechanism(AuthMechanism authMechanism) {
      this.authMechanism = authMechanism;
      return this;
    }

    public BasicClientFluentBuilder webClientBuilder(WebClient.Builder webClientBuilder) {
      this.webClientBuilder = webClientBuilder;
      return this;
    }

    public BasicChatbotClient build() {
      validate();
      return new BasicChatbotClientImpl(this.basePath, this.authMechanism, this.webClientBuilder);
    }

    protected void validate() {
      String errorMessageTemplate = "Please provide non-null value for %s";
      Objects.requireNonNull(basePath, () -> String.format(errorMessageTemplate, "basePath"));
      Objects.requireNonNull(authMechanism,
          () -> String.format(errorMessageTemplate, "authMechanism"));
    }
  }

  interface BasePathBuilder {

    AuthMechanismBuilder basePath(String basePath);
  }

  interface AuthMechanismBuilder {

    BasicClientFinalBuilder authMechanism(AuthMechanism authMechanism);
  }

  interface BasicClientFinalBuilder {

    BasicClientFinalBuilder webClientBuilder(WebClient.Builder webClientBuilder);

    BasicChatbotClient build();
  }
}
