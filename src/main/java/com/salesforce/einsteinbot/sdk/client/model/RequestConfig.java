/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client.model;

import java.util.Objects;

/**
 * RequestConfig - RequestConfig used to store configurations used to make Bot's API request.
 *
 * <p>
 * The config values stored in this class should not change often. So typically one config instance
 * can be for a Bot and reused for API requests with that bot.
 * </p>
 *
 * @author relango
 */
public class RequestConfig {

  private String botId;
  private String orgId;
  private String forceConfigEndpoint;

  private RequestConfig(String botId, String orgId, String forceConfigEndpoint) {
    Objects.requireNonNull(botId);
    Objects.requireNonNull(orgId);
    Objects.requireNonNull(forceConfigEndpoint);
    this.botId = botId;
    this.orgId = orgId;
    this.forceConfigEndpoint = forceConfigEndpoint;
  }

  public String getBotId() {
    return botId;
  }

  public String getOrgId() {
    return orgId;
  }

  public String getForceConfigEndpoint() {
    return forceConfigEndpoint;
  }

  public static BotIdBuilder with() {
    return new FluentBuilder();
  }

  public static RequestConfig with(String botId, String orgId, String forceConfigEndpoint) {
    return new RequestConfig(botId, orgId, forceConfigEndpoint);
  }

  /**
   * FluentBuilder provides Fluent API to create Request Config.
   */
  public static class FluentBuilder implements BotIdBuilder, OrgIdBuilder,
      ForceConfigEndpointBuilder, FinalBuilder {

    private String botId;
    private String orgId;
    private String forceConfigEndpoint;

    @Override
    public OrgIdBuilder botId(String botId) {
      this.botId = botId;
      return this;
    }

    @Override
    public ForceConfigEndpointBuilder orgId(String orgId) {
      this.orgId = orgId;
      return this;
    }

    @Override
    public FinalBuilder forceConfigEndpoint(String forceConfigEndpoint) {
      this.forceConfigEndpoint = forceConfigEndpoint;
      return this;
    }

    @Override
    public RequestConfig build() {
      return new RequestConfig(botId, orgId, forceConfigEndpoint);
    }
  }

  public interface BotIdBuilder {

    OrgIdBuilder botId(String botId);
  }

  public interface OrgIdBuilder {

    ForceConfigEndpointBuilder orgId(String orgId);
  }

  public interface ForceConfigEndpointBuilder {

    FinalBuilder forceConfigEndpoint(String forceConfigEndpoint);
  }

  public interface FinalBuilder {

    RequestConfig build();
  }
}
