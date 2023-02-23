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
 * AnyResponseMessage - Base type to support polymorphic anyOf for ResponseMessage. Uses Jackson
 * annotations to resolve subclass type based on value of 'type' field.
 *
 * @author relango
 * @since 234
 */

@JsonSubTypes({
        @JsonSubTypes.Type(value = TextResponseMessage.class, names = {"text"}),
        @JsonSubTypes.Type(value = ChoicesResponseMessage.class, names = {"choices"}),
        @JsonSubTypes.Type(value = EscalateResponseMessage.class, names = {"escalate"}),
        @JsonSubTypes.Type(value = SessionEndedResponseMessage.class, names = {"sessionEnded"}),
 })//TODO more comment
public interface AnyResponseMessage {

  Schedule getSchedule();
}
