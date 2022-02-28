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

  private final String loginEndpoint = "https://login.test1.pc-rnd.salesforce.com/";
  private final String connectedAppId = "3MVG9l3R9F9mHOGZUZs8TSRIINrHRklsp6OjPsKLQTUznlbLRyH_KMLfPG8SdPJugUtFa2UArLzpvtS74qDQ.";
  private final String secret = "1B57EFD4F6D22302A6D4FA9077430191CFFDFAEA22C6ABDA6FCB45993A8AD421";
  private final String userId = "admin1@esw5.sdb3";

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
