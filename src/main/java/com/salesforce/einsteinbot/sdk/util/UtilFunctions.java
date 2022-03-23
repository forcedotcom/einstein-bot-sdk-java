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

import com.salesforce.einsteinbot.sdk.model.AnyVariable;
import com.salesforce.einsteinbot.sdk.model.TextVariable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * UtilFunctions - Contains static utility functions used in Application
 * @author relango
 */
public class UtilFunctions {

  public static TextVariable createTextVariable(String name, String value) {
    return new TextVariable()
        .name(name)
        .type(TextVariable.TypeEnum.TEXT)
        .value(value);
  }

  /* todo: Add unit test
   */
  public static List<AnyVariable> addIntegrationTypeAndNameToContextVariables(List<AnyVariable> currentContextVariables,
      Optional<String> integrationNameOptional) {

    List<AnyVariable> contextVariables = currentContextVariables == null ? new ArrayList<>()
        : new ArrayList<>(currentContextVariables);

    integrationNameOptional.ifPresent( integrationName -> {
      boolean integrationTypeFound = false;
      boolean integrationNameFound = false;
      boolean integrationTypeAndNameFound = integrationTypeFound && integrationNameFound;

      Iterator<AnyVariable> iterator = contextVariables.iterator();

      while (iterator.hasNext() && !integrationTypeAndNameFound) {

        AnyVariable contextVariable = iterator.next();

        integrationTypeFound = integrationTypeFound || isIntegrationTypeVariable(contextVariable);
        integrationNameFound = integrationNameFound || isIntegrationNameVariable(contextVariable);

        integrationTypeAndNameFound = integrationTypeFound && integrationNameFound;
      }

      if (!integrationTypeFound){
        contextVariables.add(createTextVariable(CONTEXT_VARIABLE_NAME_INTEGRATION_TYPE, CONTEXT_VARIABLE_VALUE_API));
      }

      if (!integrationNameFound) {
        contextVariables.add(createTextVariable(CONTEXT_VARIABLE_NAME_INTEGRATION_NAME, integrationName));
      }
    });
    return contextVariables;
  }

  private static boolean isIntegrationTypeVariable(AnyVariable contextVariable) {
    return contextVariable.getName()
        .equals(CONTEXT_VARIABLE_NAME_INTEGRATION_TYPE);
  }

  private static boolean isIntegrationNameVariable(AnyVariable contextVariable) {
    return contextVariable.getName()
        .equals(CONTEXT_VARIABLE_NAME_INTEGRATION_NAME);
  }
}
