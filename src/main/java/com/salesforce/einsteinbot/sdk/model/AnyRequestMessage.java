/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.einsteinbot.sdk.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * AnyRequestMessage - Base type to support polymorphic anyOf for RequestMessage. Uses Jackson
 * annotations to resolve subclass type based on value of 'type' field.

 * We could not use build-in JsonTypeInfo because for some response message types we need to look up
 * two fields 'type' and 'messageType' to map the right subclass.
 *
 * So we implemented a custom JsonDeserializer AnyResponseMessageDeserializer to handle this case.
 * Also, we could not annotate this interface to use the custom AnyResponseMessageDeserializer
 * Because if we do, we also need to annotate all the subclasses to use 'JsonDeserialer.None' to avoid infinite loop.
 *
 * Since the subclasses are generated by swagger-codegen, we can not annotate them.
 *
 * So we registered custom AnyResponseAndRequestMessageDeserializer in the ObjectMapperFactory in UtilFunctions.getMapper().
 * So get object mapper from UtilFunctions.getMapper().
 *
 * @author relango
 * @since 234
 */


@JsonSubTypes({
      @JsonSubTypes.Type(value = TextMessage.class, names = { "text" }),
      @JsonSubTypes.Type(value = ChoiceMessage.class, names = { "choice" }),
      @JsonSubTypes.Type(value = RedirectMessage.class, names = { "redirect" }),
      @JsonSubTypes.Type(value = TransferSucceededRequestMessage.class, names = { "transferSucceeded" }),
      @JsonSubTypes.Type(value = TransferFailedRequestMessage.class, names = { "transferFailed" }),
      @JsonSubTypes.Type(value = EndSessionMessage.class, names = { "endSession" }),
      @JsonSubTypes.Type(value = SetVariablesMessage.class, names = { "setVariables"})
})
public interface AnyRequestMessage {

  void setSequenceId(Long sequenceId);
}
