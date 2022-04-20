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
 *
 * @author relango
 * @since 234
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)

@JsonSubTypes({
    @JsonSubTypes.Type(value = TextMessage.class, name = "text"),
    @JsonSubTypes.Type(value = ChoiceMessage.class, name = "choice"),
    @JsonSubTypes.Type(value = RedirectMessage.class, name = "redirect"),
    @JsonSubTypes.Type(value = TransferSucceededRequestMessage.class, name = "transferSucceeded"),
    @JsonSubTypes.Type(value = TransferFailedRequestMessage.class, name = "transferFailed"),
    @JsonSubTypes.Type(value = EndSessionMessage.class, name = "endSession"),
})
public interface AnyRequestMessage {
  void setSequenceId(Long sequenceId);
}
