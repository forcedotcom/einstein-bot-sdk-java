/*
 * Copyright (c) 2022, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.einsteinbot.sdk.util;

import org.springframework.core.io.buffer.DataBuffer;

/**
 * BytesUtil - BytesUtil contains utility methods to work with Bytes
 *
 * @author relango
 */
public class BytesUtil {

  static byte[] extractBytesAndReset(final DataBuffer data) {
    final byte[] bytes = new byte[data.readableByteCount()];
    data.read(bytes);
    data.readPosition(0);
    return bytes;
  }
}
