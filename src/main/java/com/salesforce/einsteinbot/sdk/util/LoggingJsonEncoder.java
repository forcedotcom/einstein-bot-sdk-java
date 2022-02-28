/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.MimeType;

/**
 * LoggingJsonEncoder - LoggingJsonEncoder is a wrapper over Jackson2JsonEncoder to log encoded Json
 * Payload for debugging purpose.
 *
 * @author relango
 */
public class LoggingJsonEncoder extends Jackson2JsonEncoder {

  private static final Logger logger = LoggerFactory.getLogger(LoggingJsonEncoder.class);

  private final boolean isEnabled;

  public LoggingJsonEncoder(ObjectMapper mapper, MediaType mediaType, boolean isEnabled) {
    super(mapper, mediaType);
    this.isEnabled = isEnabled;
  }

  @Override
  public DataBuffer encodeValue(final Object value, final DataBufferFactory bufferFactory,
      final ResolvableType valueType, final MimeType mimeType, final Map<String, Object> hints) {

    final DataBuffer data = super.encodeValue(value, bufferFactory, valueType, mimeType, hints);
    if (isEnabled) {
      logger.info("Request Payload = {} ",
          new String(BytesUtil.extractBytesAndReset(data), StandardCharsets.UTF_8));
    }
    return data;
  }

}

