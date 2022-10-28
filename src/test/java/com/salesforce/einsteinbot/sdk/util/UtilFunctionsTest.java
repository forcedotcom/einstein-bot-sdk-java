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
import static com.salesforce.einsteinbot.sdk.util.UtilFunctions.AUTHORIZATION_HEADER_MASKED;
import static com.salesforce.einsteinbot.sdk.util.UtilFunctions.addIntegrationTypeAndNameToContextVariables;
import static com.salesforce.einsteinbot.sdk.util.UtilFunctions.createTextVariable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import com.salesforce.einsteinbot.sdk.model.AnyVariable;
import com.salesforce.einsteinbot.sdk.model.TextVariable;
import com.salesforce.einsteinbot.sdk.model.TextVariable.TypeEnum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;


/**
 * UtilFunctionsTest - Unit Tests for methods in UtilFunctions
 *
 * @author relango
 */
public class UtilFunctionsTest {

  private String testIntegrationName = "TestIntegrationName";
  private TextVariable expectedIntegrationType = createTextVariable(
      CONTEXT_VARIABLE_NAME_INTEGRATION_TYPE, CONTEXT_VARIABLE_VALUE_API);
  private TextVariable expectedIntegrationNameVar = createTextVariable(
      CONTEXT_VARIABLE_NAME_INTEGRATION_NAME, testIntegrationName);

  @Test
  public void testCreateTextVariable() {
    String variableName = "TestName";
    String variableVal = "TestVal";
    TextVariable variable = createTextVariable(variableName, variableVal);
    assertEquals(variableName, variable.getName());
    assertEquals(variableVal, variable.getValue());
    assertEquals(TypeEnum.TEXT, variable.getType());
  }

  @Test
  public void testAddIntegrationTypeAndName() {
    List<AnyVariable> currentContextVariables = new ArrayList<>();
    List<AnyVariable> newContextVariables = addIntegrationTypeAndNameToContextVariables(
        currentContextVariables, Optional.of(testIntegrationName));

    assertThat(newContextVariables, contains(expectedIntegrationType, expectedIntegrationNameVar));
  }

  @Test
  public void testDoNotAddIntegrationNameIfPresent() {

    AnyVariable myIntegrationNameVariable = createTextVariable(
        CONTEXT_VARIABLE_NAME_INTEGRATION_NAME, "MyIntegrationName");

    List<AnyVariable> currentContextVariables = Arrays.asList(myIntegrationNameVariable);
    List<AnyVariable> newContextVariables = addIntegrationTypeAndNameToContextVariables(
        currentContextVariables, Optional.of(testIntegrationName));

    assertThat(newContextVariables, contains(myIntegrationNameVariable));
  }

  @Test
  public void testDoNotAddIntegrationNameIfGivenIsEmpty() {

    List<AnyVariable> currentContextVariables = new ArrayList<>();
    List<AnyVariable> newContextVariables = addIntegrationTypeAndNameToContextVariables(
        currentContextVariables, Optional.empty());

    assertTrue(newContextVariables.isEmpty());
  }

  @Test
  public void testMaskAuthorizationHeader(){
    HttpHeaders headers = new HttpHeaders();
    String requestId1 = "request-id1";
    String requestId2 = "request-id2";
    headers.add("X-Request-Id", requestId1);
    headers.add("X-Request-Id", requestId2);
    headers.add(HttpHeaders.AUTHORIZATION, "auth");
    headers.add("X-SCRT-AUTHORIZATION", "scrt-auth");
    Map<String, List<String>> masked = UtilFunctions.maskAuthorizationHeader(headers);
    assertEquals(Lists.newArrayList(AUTHORIZATION_HEADER_MASKED),masked.get(HttpHeaders.AUTHORIZATION));
    assertEquals(Lists.newArrayList(AUTHORIZATION_HEADER_MASKED), masked.get("X-SCRT-AUTHORIZATION"));
    assertEquals(Lists.newArrayList(requestId1, requestId2), masked.get("X-Request-Id"));
  }
}
