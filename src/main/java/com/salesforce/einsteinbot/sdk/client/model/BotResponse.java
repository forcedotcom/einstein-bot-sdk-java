/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client.model;

import com.salesforce.einsteinbot.sdk.model.ResponseEnvelope;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * BotResponse - BotResponse holds to Response and Http Info returned by Bot's API
 *
 * @author relango
 */
public class BotResponse {

  private ResponseEnvelope responseEnvelope;
  private int httpStatusCode;
  private BotHttpHeaders httpHeaders;

  private BotResponse(ResponseEnvelope responseEnvelope, int httpStatusCode,
      BotHttpHeaders httpHeaders) {
    Objects.requireNonNull(responseEnvelope);
    Objects.requireNonNull(httpHeaders);
    this.responseEnvelope = responseEnvelope;
    this.httpStatusCode = httpStatusCode;
    this.httpHeaders = httpHeaders;
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }

  public ResponseEnvelope getResponseEnvelope() {
    return responseEnvelope;
  }

  public BotHttpHeaders getHttpHeaders() {
    return httpHeaders;
  }

  public static BotResponse with(ResponseEnvelope responseEnvelope, int httpStatusCode,
      BotHttpHeaders httpHeaders) {
    return new BotResponse(responseEnvelope, httpStatusCode, httpHeaders);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BotResponse that = (BotResponse) o;
    return httpStatusCode == that.httpStatusCode && responseEnvelope.equals(that.responseEnvelope)
        && httpHeaders.equals(that.httpHeaders);
  }

  @Override
  public int hashCode() {
    return Objects.hash(responseEnvelope, httpStatusCode, httpHeaders);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", BotResponse.class.getSimpleName() + "[", "]")
        .add("responseEnvelope=" + responseEnvelope)
        .add("httpStatusCode=" + httpStatusCode)
        .add("httpHeaders=" + httpHeaders)
        .toString();
  }
}
