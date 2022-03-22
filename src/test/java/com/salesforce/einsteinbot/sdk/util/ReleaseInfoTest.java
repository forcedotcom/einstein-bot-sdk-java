/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * ReleaseInfoTest : Unit Tests for ReleaseInfo
 * @author relango
 */
public class ReleaseInfoTest {

  private static final String EXPECTED_SDK_NAME = "Einstein-Bot-Sdk-Java";

  @Test
  public void testReleaseInfoProperties(){
    ReleaseInfo releaseInfo = ReleaseInfo.getInstance();
    Assert.assertEquals(EXPECTED_SDK_NAME, releaseInfo.getSdkName());
    Assert.assertNotNull(releaseInfo.getSdkVersion());
  }

  @Test
  public void testUserAgent(){
    ReleaseInfo releaseInfo = ReleaseInfo.getInstance();
    String version = releaseInfo.getSdkVersion();
    Assert.assertEquals(EXPECTED_SDK_NAME + "/" + version, releaseInfo.getAsUserAgent());
  }
}
