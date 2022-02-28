/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.einsteinbot.sdk.auth;

/**
 * Interface to provide token used to authenticate with Einstein Bots.
 */
public interface AuthMechanism {

  /**
   * @return Authentication token raw value
   */
  String getToken();

  /**
   * @return Authentication value as expected by Authorization header ( eg. HTTP header containing
   * prefix like 'Bearer' )
   */
  String getAuthorizationHeader();
}
