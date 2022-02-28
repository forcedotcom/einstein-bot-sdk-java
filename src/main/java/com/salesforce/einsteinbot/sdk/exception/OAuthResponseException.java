/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.exception;

import java.util.StringJoiner;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientException;

/**
 * OAuthResponseException - Exception Class to wrap OAuth Error Response payload.
 *
 * @author relango
 * @since 238
 */
public class OAuthResponseException extends WebClientException {

  private final HttpStatus status;
  private final String errorResponse;

  public OAuthResponseException(HttpStatus status, String errorResponse) {
    super(status.getReasonPhrase());
    this.status = status;
    this.errorResponse = errorResponse;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getErrorResponse() {
    return errorResponse;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", OAuthResponseException.class.getSimpleName() + "[", "]")
        .add("status = " + status)
        .add("errorResponse = " + errorResponse)
        .add("exception = " + super.toString())
        .toString();
  }
}