/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.auth;

public class IntrospectionResult {

  private boolean active;
  private long exp;

  public IntrospectionResult() {
  }

  public IntrospectionResult(boolean active, long exp) {
    this.active = active;
    this.exp = exp;
  }

  public long getExp() {
    return exp;
  }

  public boolean isActive() {
    return active;
  }
}
