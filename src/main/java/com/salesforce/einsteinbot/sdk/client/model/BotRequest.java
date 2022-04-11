/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client.model;

import static com.salesforce.einsteinbot.sdk.util.UtilFunctions.newRandomUUID;

import com.salesforce.einsteinbot.sdk.model.AnyRequestMessage;
import com.salesforce.einsteinbot.sdk.model.AnyVariable;
import com.salesforce.einsteinbot.sdk.model.EndSessionReason;
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

  private enum Type { Message, EndSession };

  private Optional<String> requestId;
  private Optional<String> runtimeCRC;

  BotRequest(Optional<String> requestId, Optional<String> runtimeCRC) {
    Objects.requireNonNull(requestId);
    Objects.requireNonNull(runtimeCRC);
    this.requestId = requestId;
    this.runtimeCRC = runtimeCRC;
  }

  public String getOrCreateRequestId() {
    return requestId.orElse(newRandomUUID());
  }

  public Optional getRequestId(){
    return this.requestId;
  }

  public Optional<String> getRuntimeCRC() {
    return runtimeCRC;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ")
        .add("requestId=" + requestId)
        .add("runtimeCRC='" + runtimeCRC + "'")
        .toString();
  }

  public static VariablesBuilder<BotSendMessageRequest> withMessage(AnyRequestMessage message){
    return new FluentBuilder<>(message);
  }

  public static FinalBuilder<BotEndSessionRequest> withEndSession(EndSessionReason endSessionReason){
    return new FluentBuilder<>(endSessionReason);
  }

  public static SendMessageRequestCloneBuilder<BotSendMessageRequest> from(
      BotSendMessageRequest messageRequestEnvelope){
    return new FluentBuilder(messageRequestEnvelope);
  }

  public static FinalCloneBuilder<BotEndSessionRequest> from(
      BotEndSessionRequest endSessionRequestEnvelope){
    return new FluentBuilder(endSessionRequestEnvelope);
  }

  /**
   * FluentBuilder provides Fluent API to create BotRequest.
   * TODO: Add unit tests.
   */
  public static class FluentBuilder<T extends BotRequest> implements VariablesBuilder<T>,
      FinalBuilder<T> , SendMessageRequestCloneBuilder<T>, FinalCloneBuilder<T>{

    private Optional<String> requestId = Optional.empty();
    private Optional<String> runtimeCRC = Optional.empty();
    private EndSessionReason endSessionReason;
    private AnyRequestMessage message;
    private List<AnyVariable> variables = Collections.emptyList();
    private Type type;

    private FluentBuilder(AnyRequestMessage message) {
      this.type = Type.Message;
      this.message = message;
    }

    private FluentBuilder(EndSessionReason endSessionReason) {
      this.type = Type.EndSession;
      this.endSessionReason = endSessionReason;
    }

    private FluentBuilder(BotSendMessageRequest requestEnvelope){
      this(requestEnvelope.getMessage());
      this.requestId = requestEnvelope.getRequestId();
      this.runtimeCRC = requestEnvelope.getRuntimeCRC();
      this.variables = requestEnvelope.getVariables();
    }

    private FluentBuilder(BotEndSessionRequest requestEnvelope){
      this(requestEnvelope.getEndSessionReason());
      this.requestId = requestEnvelope.getRequestId();
      this.runtimeCRC = requestEnvelope.getRuntimeCRC();
    }

    @Override
    public FinalBuilder<T> requestId(String requestId) {
      return requestId(Optional.of(requestId));
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
      this.runtimeCRC = Optional.of(runtimeCRC);
      return this;
    }

    @Override
    public FinalCloneBuilder<T> setRequestId(String requestId) {
      this.requestId = Optional.of(requestId);
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
      this.runtimeCRC = Optional.of(runtimeCRC);
      return this;
    }

    @Override
    public FinalBuilder<T> variables(List<AnyVariable> variables) {
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
      if (type == Type.Message){
        return (T) new BotSendMessageRequest(requestId, runtimeCRC, variables, message);
      }else if (type == Type.EndSession){
        return (T) new BotEndSessionRequest(requestId, runtimeCRC, endSessionReason);
      }else {
        throw new IllegalArgumentException("Invalid type : " + type);
      }
    }
  }

  public interface VariablesBuilder<T> extends FinalBuilder<T> {
    FinalBuilder<T> variables(List<AnyVariable> variables);
  }

  public interface SendMessageRequestCloneBuilder<T> extends FinalCloneBuilder<T> {
    FinalCloneBuilder<T> setVariables(List<AnyVariable> variables);
  }

  public interface FinalCloneBuilder<T> {

    FinalCloneBuilder<T> setRequestId(String requestId);

    FinalCloneBuilder<T> setRequestId(Optional<String> requestId);

    FinalCloneBuilder<T> setRuntimeCRC(Optional<String> runtimeCRC);

    FinalCloneBuilder<T> setRuntimeCRC(String runtimeCRC);

    T build();
  }

  public interface FinalBuilder<T> {

    FinalBuilder<T> requestId(String requestId);

    FinalBuilder<T> requestId(Optional<String> requestId);

    FinalBuilder<T> runtimeCRC(Optional<String> runtimeCRC);

    FinalBuilder<T> runtimeCRC(String runtimeCRC);

    T build();
  }
}
