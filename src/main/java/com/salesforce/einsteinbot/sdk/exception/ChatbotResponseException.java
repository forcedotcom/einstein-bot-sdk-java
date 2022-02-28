/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.exception;

import com.salesforce.einsteinbot.sdk.model.Error;
import java.util.StringJoiner;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientException;

/**
 * ChatbotResponseException - Exception Class to wrap Runtime Error Response payload.
 * <p>
 * The Spring Webclient uses Throwable to report all errors. So we extended WebClient's
 * ChatbotResponseException and wrap the Error Response Inside it.
 *
 * @author relango
 * @since 234
 */
public class ChatbotResponseException extends WebClientException {

  private final HttpStatus status;
  private final Error errorResponse;

  public ChatbotResponseException(HttpStatus status, Error errorResponse) {
    super(status.getReasonPhrase());
    this.status = status;
    this.errorResponse = errorResponse;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public Error getErrorResponse() {
    return errorResponse;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ChatbotResponseException.class.getSimpleName() + "[", "]")
        .add("status = " + status)
        .add("errorResponse = " + errorResponse)
        .add("exception = " + super.toString())
        .toString();
  }
}