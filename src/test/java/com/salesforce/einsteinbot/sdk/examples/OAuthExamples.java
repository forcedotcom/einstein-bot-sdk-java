/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.examples;

import com.salesforce.einsteinbot.sdk.auth.IntrospectionResult;
import com.salesforce.einsteinbot.sdk.auth.Introspector;
import com.salesforce.einsteinbot.sdk.auth.JwtBearerOAuth;
import com.salesforce.einsteinbot.sdk.cache.InMemoryCache;

public class OAuthExamples {

  //Replace folowing variables with real values before running.
  private final String loginEndpoint = "SALESFORCE_LOGIN_END_POINT";
  private final String connectedAppId = "YOUR_CONNECTED_APP_ID";
  private final String secret = "YOUR_CONNECTED_APP_SECRET";
  private final String userId = "SALESFORCE_LOGIN_USER";

  public static void main(String[] args) {
    new OAuthExamples().run();
  }

  private void run() {
    /*
     * Command to convert private key to DER format
     * openssl pkcs8 -topk8 -inform PEM -outform DER -in server.key -out src/test/resources/YourPrivateKey.der -nocrypt
     * */
    JwtBearerOAuth oAuth = new JwtBearerOAuth("src/test/resources/YourPrivateKey.der",
        loginEndpoint, connectedAppId, secret, userId, new InMemoryCache(300L));
    String token = oAuth.getToken();

    Introspector is = new Introspector(connectedAppId, secret, loginEndpoint);
    IntrospectionResult result = is.introspect(token);

    System.out.println("Token : " + token + " IntrospectionResult : " + result);
  }

}
