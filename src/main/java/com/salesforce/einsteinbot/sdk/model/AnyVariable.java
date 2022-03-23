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
 * AnyVariable - Base type to support polymorphic anyOf for Variables. Uses Jackson annotations
 * to resolve subclass type based on value of 'type' field.
 *
 * @author relango
 * @since 234
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextVariable.class, name = "text"),
    @JsonSubTypes.Type(value = BooleanVariable.class, name = "boolean"),
    @JsonSubTypes.Type(value = DateVariable.class, name = "date"),
    @JsonSubTypes.Type(value = DateTimeVariable.class, name = "dateTime"),
    @JsonSubTypes.Type(value = MoneyVariable.class, name = "money"),
    @JsonSubTypes.Type(value = NumberVariable.class, name = "number"),
    @JsonSubTypes.Type(value = ObjectVariable.class, name = "object"),
    @JsonSubTypes.Type(value = ListVariable.class, name = "list"),
    @JsonSubTypes.Type(value = RefVariable.class, name = "ref")
})
public interface AnyVariable {
  String getName();
}
