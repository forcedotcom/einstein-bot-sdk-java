/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.client.model;

import java.util.Objects;

/**
 * RuntimeSessionId - RuntimeSessionId is a class to track Runtime's session id.
 *
 * <p>
 * An Runtime session Id is the unique ID used by Bot's runtime to track sessions.
 * A Runtime session Id need to be provided continue existing session or end session.
 * </p>
 *
 * @author relango
 */
public class RuntimeSessionId implements BotSessionId{

  private String value;

  public RuntimeSessionId(String value) {
    Objects.requireNonNull(value);
    this.value = value;
  }

  public String getValue(){
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RuntimeSessionId that = (RuntimeSessionId) o;
    return value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
