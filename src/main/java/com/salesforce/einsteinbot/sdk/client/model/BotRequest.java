/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client.model;

import static com.salesforce.einsteinbot.sdk.util.UtilFunctions.newRandomUUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.salesforce.einsteinbot.sdk.client.util.RequestEnvelopeInterceptor;
import com.salesforce.einsteinbot.sdk.model.AnyRequestMessage;
import com.salesforce.einsteinbot.sdk.model.AnyVariable;
import com.salesforce.einsteinbot.sdk.model.EndSessionReason;
import com.salesforce.einsteinbot.sdk.model.Referrer;
import com.salesforce.einsteinbot.sdk.model.ResponseOptions;
import com.salesforce.einsteinbot.sdk.model.RichContentCapability;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * BotRequest - BotRequest class to pass information required to make a request to Bot API.
 *
 * @author relango
 */
public class BotRequest {

  public enum Type {
    Message, EndSession
  }
  private Optional<String> requestId;
  private Optional<String> runtimeCRC;
  private RequestEnvelopeInterceptor requestEnvelopeInterceptor;

  BotRequest(Optional<String> requestId, Optional<String> runtimeCRC,
      RequestEnvelopeInterceptor requestEnvelopeInterceptor) {
    Objects.requireNonNull(requestId);
    Objects.requireNonNull(runtimeCRC);
    Objects.requireNonNull(requestEnvelopeInterceptor);
    this.requestId = requestId;
    this.runtimeCRC = runtimeCRC;
    this.requestEnvelopeInterceptor = requestEnvelopeInterceptor;
  }

  public Optional<String> getRequestId() {
    return this.requestId;
  }

  public Optional<String> getRuntimeCRC() {
    return runtimeCRC;
  }

  public RequestEnvelopeInterceptor getRequestEnvelopeInterceptor() {
    return requestEnvelopeInterceptor;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ")
        .add("requestId=" + requestId)
        .add("runtimeCRC='" + runtimeCRC + "'")
        .toString();
  }

  public static InitMessageOptionalFieldsBuilder<BotSendMessageRequest> withMessage(AnyRequestMessage message) {
    return new FluentBuilder<>(message);
  }

  public static FinalBuilder<BotEndSessionRequest> withEndSession(
      EndSessionReason endSessionReason) {
    return new FluentBuilder<>(endSessionReason);
  }

  /**
   * FluentBuilder provides Fluent API to create BotRequest.
   */
  public static class FluentBuilder<T extends BotRequest> implements
      InitMessageOptionalFieldsBuilder<T>,
      FinalBuilder<T>, SendMessageRequestCloneBuilder<T>, FinalCloneBuilder<T> {

    protected Optional<String> requestId = Optional.empty();
    protected Optional<String> runtimeCRC = Optional.empty();
    protected EndSessionReason endSessionReason;
    protected AnyRequestMessage message;
    protected List<AnyVariable> variables = Collections.emptyList();
    protected Optional<String> tz = Optional.empty();
    protected Optional<ResponseOptions> responseOptions = Optional.empty();
    protected List<Referrer> referrers = Collections.emptyList();
    protected Optional<RichContentCapability> richContentCapabilities = Optional.empty();

    protected Type type;
    protected RequestEnvelopeInterceptor requestEnvelopeInterceptor = v -> {/*NOOP Consumer*/};

    protected FluentBuilder(AnyRequestMessage message) {
      this.type = Type.Message;
      this.message = message;
    }

    protected FluentBuilder(EndSessionReason endSessionReason) {
      this.type = Type.EndSession;
      this.endSessionReason = endSessionReason;
    }

    public FluentBuilder(BotSendMessageRequest requestEnvelope) {
      this(requestEnvelope.getMessage());
      this.requestId = requestEnvelope.getRequestId();
      this.runtimeCRC = requestEnvelope.getRuntimeCRC();
      this.variables = requestEnvelope.getVariables();
      this.requestEnvelopeInterceptor = requestEnvelope.getRequestEnvelopeInterceptor();
      this.tz = requestEnvelope.getTz();
      this.responseOptions = requestEnvelope.getResponseOptions();
      this.referrers = requestEnvelope.getReferrers();
      this.richContentCapabilities = requestEnvelope.getRichContentCapabilities();
    }

    private FluentBuilder(BotEndSessionRequest requestEnvelope) {
      this(requestEnvelope.getEndSessionReason());
      this.requestId = requestEnvelope.getRequestId();
      this.runtimeCRC = requestEnvelope.getRuntimeCRC();
      this.requestEnvelopeInterceptor = requestEnvelope.getRequestEnvelopeInterceptor();
    }

    @Override
    public FinalBuilder<T> requestId(String requestId) {
      return requestId(Optional.ofNullable(requestId));
    }

    @Override
    public FinalBuilder<T> requestId(Optional<String> requestId) {
      this.requestId = requestId;
      return this;
    }

    @Override
    public FinalBuilder<T> runtimeCRC(Optional<String> runtimeCRC) {
      this.runtimeCRC = runtimeCRC;
      return this;
    }

    @Override
    public FinalBuilder<T> runtimeCRC(String runtimeCRC) {
      this.runtimeCRC = Optional.ofNullable(runtimeCRC);
      return this;
    }

    @Override
    public FinalBuilder<T> requestEnvelopeInterceptor(
        RequestEnvelopeInterceptor requestEnvelopeInterceptor) {

      this.requestEnvelopeInterceptor = requestEnvelopeInterceptor;
      return this;
    }

    @Override
    public FinalCloneBuilder<T> setRequestId(String requestId) {
      this.requestId = Optional.ofNullable(requestId);
      return this;
    }

    @Override
    public FinalCloneBuilder<T> setRequestId(Optional<String> requestId) {
      this.requestId = requestId;
      return this;
    }

    @Override
    public FinalCloneBuilder<T> setRuntimeCRC(Optional<String> runtimeCRC) {
      this.runtimeCRC = runtimeCRC;
      return this;
    }

    @Override
    public FinalCloneBuilder<T> setRuntimeCRC(String runtimeCRC) {
      this.runtimeCRC = Optional.ofNullable(runtimeCRC);
      return this;
    }

    @Override
    public FinalCloneBuilder<T> setRequestEnvelopeInterceptor(
        RequestEnvelopeInterceptor requestEnvelopeInterceptor) {
      this.requestEnvelopeInterceptor = requestEnvelopeInterceptor;
      return this;
    }

    @Override
    public InitMessageOptionalFieldsBuilder<T> tz(String tz) {
      this.tz = Optional.ofNullable(tz);
      return this;
    }

    @Override
    public InitMessageOptionalFieldsBuilder<T> referrers(List<Referrer> referrers) {
      this.referrers = referrers;
      return this;
    }

    @Override
    public InitMessageOptionalFieldsBuilder<T> responseOptions(ResponseOptions responseOptions) {
      this.responseOptions = Optional.ofNullable(responseOptions);
      return this;
    }

    @Override
    public InitMessageOptionalFieldsBuilder<T> richContentCapabilities(RichContentCapability richContentCapabilities) {
      this.richContentCapabilities = Optional.ofNullable(richContentCapabilities);
      return this;
    }

    @Override
    public InitMessageOptionalFieldsBuilder<T> variables(List<AnyVariable> variables) {
      this.variables = variables;
      return this;
    }

    @Override
    public FinalCloneBuilder<T> setVariables(List<AnyVariable> variables) {
      this.variables = variables;
      return this;
    }

    @Override
    public T build() {
      if (type == Type.Message) {
        return (T) new BotSendMessageRequest(requestId, runtimeCRC, requestEnvelopeInterceptor,
            variables, message, tz, responseOptions, referrers, richContentCapabilities);
      } else if (type == Type.EndSession) {
        return (T) new BotEndSessionRequest(requestId, runtimeCRC, requestEnvelopeInterceptor,
            endSessionReason);
      } else {
        throw new IllegalArgumentException("Invalid type : " + type);
      }
    }
  }

  public interface InitMessageOptionalFieldsBuilder<T> extends FinalBuilder<T> {

    InitMessageOptionalFieldsBuilder<T> tz(String tz);
    InitMessageOptionalFieldsBuilder<T> referrers(List<Referrer> referrers);
    InitMessageOptionalFieldsBuilder<T> responseOptions(ResponseOptions responseOptions);
    InitMessageOptionalFieldsBuilder<T> richContentCapabilities(RichContentCapability richContentCapabilities);
    InitMessageOptionalFieldsBuilder<T> variables(List<AnyVariable> variables);
  }

  public interface SendMessageRequestCloneBuilder<T> extends FinalCloneBuilder<T> {
    FinalCloneBuilder<T> setVariables(List<AnyVariable> variables);
  }

  public interface FinalCloneBuilder<T> {

    FinalCloneBuilder<T> setRequestId(String requestId);

    FinalCloneBuilder<T> setRequestId(Optional<String> requestId);

    FinalCloneBuilder<T> setRuntimeCRC(Optional<String> runtimeCRC);

    FinalCloneBuilder<T> setRuntimeCRC(String runtimeCRC);

    FinalCloneBuilder<T> setRequestEnvelopeInterceptor(
        RequestEnvelopeInterceptor requestEnvelopeInterceptor);

    T build();
  }

  public interface FinalBuilder<T> {

    FinalBuilder<T> requestId(String requestId);

    FinalBuilder<T> requestId(Optional<String> requestId);

    FinalBuilder<T> runtimeCRC(Optional<String> runtimeCRC);

    FinalBuilder<T> runtimeCRC(String runtimeCRC);

    FinalBuilder<T> requestEnvelopeInterceptor(
        RequestEnvelopeInterceptor requestEnvelopeInterceptor);

    T build();
  }
}
