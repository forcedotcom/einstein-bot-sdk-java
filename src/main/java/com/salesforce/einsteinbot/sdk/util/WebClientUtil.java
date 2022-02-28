/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.util;

import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

/**
 * WebClientUtil - Provides utility methods to work with Spring WebClient
 *
 * @author relango
 */

public class WebClientUtil {

  private static final Logger logger = LoggerFactory.getLogger(WebClientUtil.class);

  public static Mono<ClientRequest> createLoggingRequestProcessor(ClientRequest clientRequest) {
    logger.info("Making {} Request to URI {} with Headers : {}", clientRequest.method(),
        clientRequest.url(), clientRequest.headers());
    return Mono.just(clientRequest);
  }

  public static Mono<ClientResponse> createErrorResponseProcessor(ClientResponse clientResponse,
      Function<ClientResponse, Mono<ClientResponse>> errorResponseMapper) {
    if (clientResponse.statusCode().isError()) {
      return errorResponseMapper.apply(clientResponse);
    } else {
      return Mono.just(clientResponse);
    }
  }

  public static ExchangeFilterFunction createFilter(
      Function<ClientRequest, Mono<ClientRequest>> requestProcessor,
      Function<ClientResponse, Mono<ClientResponse>> responseProcessor) {
    return ExchangeFilterFunction
        .ofRequestProcessor(requestProcessor)
        .andThen(
            ExchangeFilterFunction.ofResponseProcessor(responseProcessor)
        );
  }
}
