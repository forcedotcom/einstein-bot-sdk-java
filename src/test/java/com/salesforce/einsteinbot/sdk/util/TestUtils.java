/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.util;

import com.salesforce.einsteinbot.sdk.client.model.BotHttpHeaders;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * TestUtils - Contains utility methods used by tests
 *
 * @author relango
 */
public class TestUtils {

  private static final String TEST_FILES_DIR = "__files/";
  public static final String TEST_MOCK_DIR = "mocks/";
  public static final String TEST_FILES_MOCK_DIR = "/" + TEST_FILES_DIR + TEST_MOCK_DIR;
  public static final String EXPECTED_SDK_NAME = "einstein-bot-sdk-java";

  public static String readTestFileAsString(String fileName)
      throws URISyntaxException, IOException {
    return readFileAsString(
        Paths.get(TestUtils.class.getResource(TEST_FILES_MOCK_DIR + fileName).toURI()));
  }

  public static String readFileAsString(Path path) throws IOException {
    return new String(Files.readAllBytes(path));
  }

  public static <T> ResponseEntity<T> createResponseEntity(T responseEnvelope,
      BotHttpHeaders httpHeaders, HttpStatus httpStatus) {
    return new ResponseEntity(responseEnvelope, httpHeaders.toMultiValueMap(), httpStatus);
  }
}