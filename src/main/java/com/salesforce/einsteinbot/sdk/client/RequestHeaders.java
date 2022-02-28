/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import java.util.Optional;
import java.util.StringJoiner;

/**
 * RequestHeaders - Class to hold HTTP header values that are sent as part of Runtime API Request.
 *
 * @author relango
 */
public class RequestHeaders {

  private String orgId;
  private Optional<String> requestId;
  private String runtimeCRC;

  private RequestHeaders(String orgId, Optional<String> requestId, String runtimeCRC) {
    this.orgId = orgId;
    this.requestId = requestId;
    this.runtimeCRC = runtimeCRC;
  }

  public String getOrgId() {
    return orgId;
  }

  public String getRequestIdOr(String defaultValue) {
    return requestId.orElse(defaultValue);
  }

  public String getRuntimeCRC() {
    return runtimeCRC;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", RequestHeaders.class.getSimpleName() + "[", "]")
        .add("orgId='" + orgId + "'")
        .add("requestId=" + requestId)
        .add("runtimeCRC='" + runtimeCRC + "'")
        .toString();
  }

  /**
   * @return OrgIdBuilder to ensure required orgId value is set.
   */
  public static OrgIdBuilder builder() {
    return new FluentBuilder();
  }

  /**
   * FluentBuilder provides Fluent API to create RequestHeaders.
   */
  public static class FluentBuilder implements OrgIdBuilder, Builder {

    private String orgId;
    private Optional<String> requestId = Optional.empty();
    private String runtimeCRC;

    @Override
    public Builder orgId(String orgId) {
      this.orgId = orgId;
      return this;
    }

    @Override
    public Builder requestId(String requestId) {
      this.requestId = Optional.of(requestId);
      return this;
    }

    @Override
    public Builder runtimeCRC(String runtimeCRC) {
      this.runtimeCRC = runtimeCRC;
      return this;
    }

    @Override
    public RequestHeaders build() {
      return new RequestHeaders(orgId, requestId, runtimeCRC);
    }
  }

  /**
   * Builder interface for setting required OrgId header value
   */
  public interface OrgIdBuilder {

    Builder orgId(String orgId);
  }

  /**
   * Builder interface for setting optional header values and build method to create instance.
   */
  public interface Builder {

    Builder requestId(String requestId);

    Builder runtimeCRC(String runtimeCRC);

    RequestHeaders build();
  }
}
