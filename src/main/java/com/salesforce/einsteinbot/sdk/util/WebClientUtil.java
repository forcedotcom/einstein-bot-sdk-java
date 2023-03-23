/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.util;

import static com.salesforce.einsteinbot.sdk.util.UtilFunctions.maskAuthorizationHeader;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.salesforce.einsteinbot.sdk.model.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
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
        clientRequest.url(), maskAuthorizationHeader(clientRequest.headers()));
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

  public static BodyExtractor<Mono<Error>, ReactiveHttpInputMessage> errorBodyExtractor() {
    BodyExtractor<Mono<Error>, ReactiveHttpInputMessage> extractor = (inputMessage, context) -> {
      String contentType = inputMessage.getHeaders().getContentType().toString();
      if (contentType.contains("application/json")) {
        return BodyExtractors.toMono(Error.class)
                .extract(inputMessage, context);
      } else {
        return buildErrorFromClientResponse(inputMessage, context);
      }
    };
    return extractor;
  }

  private static Mono<Error> buildErrorFromClientResponse(ReactiveHttpInputMessage clientResponse, BodyExtractor.Context context) {
    ClientHttpResponse response = (ClientHttpResponse) clientResponse;
    Mono<String> bodyString = BodyExtractors.toMono(String.class).
            extract(clientResponse, context);
    return bodyString.map(errorMessage -> new Error()
            .status(response.getRawStatusCode())
            .message("This Error Response is returned before hitting Runtime Service, " +
                    "See the 'error' field for details")
            .error(errorMessage));
  }
}
