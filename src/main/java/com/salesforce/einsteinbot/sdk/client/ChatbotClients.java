/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;


import com.salesforce.einsteinbot.sdk.client.BasicChatbotClient.BasePathBuilder;
import com.salesforce.einsteinbot.sdk.client.BasicChatbotClient.BasicClientFluentBuilder;
import com.salesforce.einsteinbot.sdk.client.SessionManagedChatbotClient.BasicClientBuilder;
import com.salesforce.einsteinbot.sdk.client.SessionManagedChatbotClient.SessionManagedClientFluentBuilder;

/**
 * ChatbotClients - Provide factory methods for creating different implementations of {@link ChatbotClient}
 *
 * @author relango
 */
public class ChatbotClients {

  /**
   * Return's builder to build {@link SessionManagedChatbotClient} implementation.
   * @return SessionManagedClient FluentBuilder
   */
  public static BasicClientBuilder sessionManaged() {
    return new SessionManagedClientFluentBuilder();
  }

  /**
   * Return's builder to build {@link BasicChatbotClient} implementation.
   * @return BasicClient FluentBuilder
   */
  public static BasePathBuilder basic() {
    return new BasicClientFluentBuilder();
  }
}
