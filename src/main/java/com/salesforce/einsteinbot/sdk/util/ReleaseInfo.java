/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ReleaseInfo - Holds SDK Release info.
 * @author relango
 */
public class ReleaseInfo {

  private static final Logger logger = LoggerFactory.getLogger(ReleaseInfo.class);

  private static final String RELEASE_INFO_PROPERTIES_FILE = "release-info.properties";
  private static final String SDK_NAME_PROPERTY = "sdk.name";
  private static final String SDK_VERSION_PROPERTY = "sdk.version";
  private static final String DEFAULT_VALUE = "UNKNOWN";

  //Use static nested class for thread-safe Singleton.
  private static class InstanceHolder {
    private static ReleaseInfo instance = new ReleaseInfo();
  }

  private String sdkName;
  private String sdkVersion;

  public static ReleaseInfo getInstance() {
    return InstanceHolder.instance;
  }

  private ReleaseInfo() {
    initializeValues(loadReleaseInfoProperties());
  }

  private Properties loadReleaseInfoProperties() {
    Properties props = new Properties();

    try (InputStream inputStream = getReleaseInfoPropertiesAsInputStream()) {
      props.load(inputStream);
    } catch (IOException e) {
      logger.error("Exception in loading Release Info Properties file : {} ",
          RELEASE_INFO_PROPERTIES_FILE, e);
    }
    return props;
  }

  private InputStream getReleaseInfoPropertiesAsInputStream() throws FileNotFoundException {
    InputStream inputStream = this.getClass()
        .getClassLoader()
        .getResourceAsStream(RELEASE_INFO_PROPERTIES_FILE);

    if (inputStream == null) {
      throw new FileNotFoundException(
          "Missing Release Info Properties file : " + RELEASE_INFO_PROPERTIES_FILE);
    }

    return inputStream;
  }

  private void initializeValues(Properties props) {
    this.sdkName = props.getProperty(SDK_NAME_PROPERTY, DEFAULT_VALUE);
    this.sdkVersion = props.getProperty(SDK_VERSION_PROPERTY, DEFAULT_VALUE);
  }

  public String getSdkName() {
    return sdkName;
  }

  public String getSdkVersion() {
    return sdkVersion;
  }
}
