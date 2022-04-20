/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.util;

import static com.salesforce.einsteinbot.sdk.util.Constants.CONTEXT_VARIABLE_NAME_INTEGRATION_NAME;
import static com.salesforce.einsteinbot.sdk.util.Constants.CONTEXT_VARIABLE_NAME_INTEGRATION_TYPE;
import static com.salesforce.einsteinbot.sdk.util.Constants.CONTEXT_VARIABLE_VALUE_API;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.salesforce.einsteinbot.sdk.handler.RFC3339DateFormat;
import com.salesforce.einsteinbot.sdk.model.AnyVariable;
import com.salesforce.einsteinbot.sdk.model.TextVariable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullableModule;

/**
 * UtilFunctions - Contains static utility functions used in Application
 *
 * @author relango
 */
public class UtilFunctions {

  private static final ObjectMapper mapper = getMapper();
  private static final DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();


  static {
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
  }

  public static String convertObjectToJson(Object data) throws JsonProcessingException {
      return mapper.writer(prettyPrinter).writeValueAsString(data);
  }

  public static TextVariable createTextVariable(String name, String value) {
    return new TextVariable()
        .name(name)
        .type(TextVariable.TypeEnum.TEXT)
        .value(value);
  }

  /**
   * Adds IntegrationType and IntegrationName context variables to given ContextVariables only if
   * integrationNameOptional is present and IntegrationName Context Variable doesn't already exist
   * in given currentContextVariables.
   *
   * @param currentContextVariables
   * @param integrationNameOptional
   * @return
   */
  public static List<AnyVariable> addIntegrationTypeAndNameToContextVariables(
      List<AnyVariable> currentContextVariables,
      Optional<String> integrationNameOptional) {

    List<AnyVariable> contextVariables = currentContextVariables == null ? new ArrayList<>()
        : new ArrayList<>(currentContextVariables);

    if (integrationNameOptional.isPresent() && !isIntegrationNameFoundInContextVariables(
        contextVariables)) {
      contextVariables.add(
          createTextVariable(CONTEXT_VARIABLE_NAME_INTEGRATION_TYPE, CONTEXT_VARIABLE_VALUE_API));
      contextVariables.add(createTextVariable(CONTEXT_VARIABLE_NAME_INTEGRATION_NAME,
          integrationNameOptional.get()));
    }

    return contextVariables;
  }

  private static boolean isIntegrationNameFoundInContextVariables(
      List<AnyVariable> contextVariables) {
    return contextVariables
        .stream()
        .filter(UtilFunctions::isIntegrationNameVariable)
        .findFirst()
        .isPresent();
  }

  private static boolean isIntegrationNameVariable(AnyVariable contextVariable) {
    return contextVariable.getName()
        .equals(CONTEXT_VARIABLE_NAME_INTEGRATION_NAME);
  }

  public static String newRandomUUID() {
    return UUID.randomUUID().toString();
  }

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
