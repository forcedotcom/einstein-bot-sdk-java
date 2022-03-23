/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client;

import com.salesforce.einsteinbot.sdk.auth.AuthMechanism;

/**
 * TestAuthMechanism - Used for testing
 */
public class TestAuthMechanism implements AuthMechanism {

  private static final String TEST_TOKEN = "TOKEN";

  @Override
  public String getToken() {
    return TEST_TOKEN;
  }

  @Override
  public String getAuthorizationHeader() {
    return "Bearer " + TEST_TOKEN;
  }
}
