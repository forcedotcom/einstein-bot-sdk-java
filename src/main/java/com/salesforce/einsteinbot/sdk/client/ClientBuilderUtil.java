/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.salesforce.einsteinbot.sdk.handler.RFC3339DateFormat;
import java.text.DateFormat;
import java.util.TimeZone;
import org.openapitools.jackson.nullable.JsonNullableModule;

public class ClientBuilderUtil {

  public static ObjectMapper getMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setDateFormat(createDefaultDateFormat());
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    JsonNullableModule jnm = new JsonNullableModule();
    mapper.registerModule(jnm);
    mapper.setSerializationInclusion(Include.NON_NULL);
    return mapper;
  }

  public static DateFormat createDefaultDateFormat() {
    DateFormat dateFormat = new RFC3339DateFormat();
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

    return dateFormat;
  }
}
