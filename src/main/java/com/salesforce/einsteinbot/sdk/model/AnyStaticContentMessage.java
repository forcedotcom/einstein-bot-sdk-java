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
 * AnyStaticContentMessage - Base type to support polymorphic anyOf for StaticContentMessage. Uses Jackson
 * annotations to resolve subclass type based on value of 'type' field.
 *
 * @author relango
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = StaticContentAttachments.class, name = "Attachments"),
    @JsonSubTypes.Type(value = StaticContentText.class, name = "Text"),
})
public interface AnyStaticContentMessage {
}
