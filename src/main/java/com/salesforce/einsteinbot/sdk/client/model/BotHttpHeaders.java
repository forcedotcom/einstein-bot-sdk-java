/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * BotHttpHeaders - Class to store/retrieve Http Headers used by Bot API Requests.
 *
 * @author relango
 */
public class BotHttpHeaders {

  public static final String HEADER_NAME_REQUEST_ID = "X-Request-ID";
  public static final String HEADER_NAME_RUNTIME_CRC = "X-Runtime-CRC";

  private Multimap<String, String> headerValues = ArrayListMultimap.create();

  private BotHttpHeaders(Multimap<String, String> headerValues) {
    Objects.requireNonNull(headerValues);
    this.headerValues = headerValues;
  }

  private BotHttpHeaders(Set<Entry<String, List<String>>> entries) {
    entries
        .stream()
        .forEach(this::addToHeader);

  }

  private void addToHeader(Entry<String, List<String>> entry) {
    headerValues.putAll(entry.getKey(), entry.getValue());
  }

  public static Builder with() {
    return new Builder();
  }

  public static BotHttpHeaders fromSpringHttpHeaders(HttpHeaders httpHeaders) {
    return new BotHttpHeaders(httpHeaders.entrySet());
  }

  public Optional<String> getRequestIdHeader() {
    return getFirst(HEADER_NAME_REQUEST_ID);
  }

  public Optional<String> getRuntimeCRCHeader() {
    return getFirst(HEADER_NAME_RUNTIME_CRC);
  }

  public Optional<String> getFirst(String headerName) {
    return Optional
        .ofNullable(headerValues.get(headerName))
        .flatMap(this::findFirstItem);
  }

  private Optional<String> findFirstItem(Collection<String> collection) {
    return collection.stream().findFirst();
  }

  public Map<String, Collection<String>> getAll() {
    return Collections.unmodifiableMap(headerValues.asMap());
  }

  public Collection<String> get(String headerName) {
    return headerValues.get(headerName);
  }

  public Multimap<String, String> getHeaderValues() {
    return Multimaps.unmodifiableMultimap(headerValues);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BotHttpHeaders that = (BotHttpHeaders) o;
    return headerValues.equals(that.headerValues);
  }

  @Override
  public int hashCode() {
    return Objects.hash(headerValues);
  }

  @Override
  public String toString() {
    return headerValues.toString();
  }

  public static class Builder {

    private Multimap<String, String> headerValues = ArrayListMultimap.create();

    public Builder requestId(String requestId) {
      headerValues.put(HEADER_NAME_REQUEST_ID, requestId);
      return this;
    }

    public Builder runtimeCRC(String runtimeCRC) {
      headerValues.put(HEADER_NAME_RUNTIME_CRC, runtimeCRC);
      return this;
    }

    public Builder header(String headerName, String headerValue) {
      headerValues.put(headerName, headerValue);
      return this;
    }

    public BotHttpHeaders build() {
      return new BotHttpHeaders(headerValues);
    }
  }
}
