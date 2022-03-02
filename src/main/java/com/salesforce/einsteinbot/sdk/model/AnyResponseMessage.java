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

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextResponseMessage.class, name = "text"),
    @JsonSubTypes.Type(value = ChoicesResponseMessage.class, name = "choices"),
    @JsonSubTypes.Type(value = EscalateResponseMessage.class, name = "escalate"),
    @JsonSubTypes.Type(value = SessionEndedResponseMessage.class, name = "sessionEnded")
})
public interface AnyResponseMessage {

  Schedule getSchedule();
}
