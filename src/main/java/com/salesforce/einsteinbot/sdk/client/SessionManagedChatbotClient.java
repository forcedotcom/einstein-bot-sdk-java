/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import com.salesforce.einsteinbot.sdk.cache.Cache;
import java.util.Objects;
import java.util.Optional;

/**
 */
public interface SessionManagedChatbotClient extends ChatbotClient<ExternalSessionId> {

  /**
   * SessionManagedClientFluentBuilder provides Fluent API to create Session Managed Chatbot Client.
   */
  class SessionManagedClientFluentBuilder implements BasicClientBuilder,
      CacheBuilder, SessionManagedClientFinalBuilder {

    private BasicChatbotClient basicClient;
    private Optional<String> integrationName = Optional.empty();
    private Cache cache;

    SessionManagedClientFluentBuilder() {
    }

    public SessionManagedClientFluentBuilder basicClient(BasicChatbotClient basicClient) {
      this.basicClient = basicClient;
      return this;
    }

    public SessionManagedClientFluentBuilder integrationName(String integrationName) {
      IntegrationNameValidator.validateIntegrationName(integrationName);
      this.integrationName = Optional.ofNullable(integrationName);
      return this;
    }

    public SessionManagedClientFluentBuilder cache(Cache cache) {
      this.cache = cache;
      return this;
    }

    public SessionManagedChatbotClient build() {
      String errorMessageTemplate = "Please provide non-null value for %s ";
      Objects.requireNonNull(basicClient, () -> String.format(errorMessageTemplate, "basicClient"));
      Objects.requireNonNull(cache, () -> String.format(errorMessageTemplate, "cache"));
      return new SessionManagedChatbotClientImpl(this.basicClient, this.integrationName, this.cache);
    }

  }

  interface BasicClientBuilder {
    SessionManagedChatbotClientImpl.CacheBuilder basicClient(BasicChatbotClient basicClient);
  }

  interface CacheBuilder {
    SessionManagedClientFinalBuilder cache(Cache cache);
  }

  interface SessionManagedClientFinalBuilder {
    SessionManagedClientFinalBuilder integrationName(String integrationName);
    SessionManagedChatbotClient build();
  }
}
