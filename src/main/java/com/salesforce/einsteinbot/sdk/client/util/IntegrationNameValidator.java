/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client.util;

public class IntegrationNameValidator {

  private static final int MAX_LENGTH = 128;

  public static void validateIntegrationName(String name) {
    if (name != null && name.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("Integration name exceeds max length");
    }
  }
}
