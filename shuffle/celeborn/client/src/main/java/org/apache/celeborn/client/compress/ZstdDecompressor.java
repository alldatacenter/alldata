/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.client.compress;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

import com.github.luben.zstd.Zstd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZstdDecompressor extends ZstdTrait implements Decompressor {
  private static final Logger logger = LoggerFactory.getLogger(ZstdDecompressor.class);
  private final Checksum checksum;

  public ZstdDecompressor() {
    checksum = new CRC32();
  }

  @Override
  public int getOriginalLen(byte[] src) {
    return readIntLE(src, MAGIC_LENGTH + 5);
  }

  @Override
  public int decompress(byte[] src, byte[] dst, int dstOff) {
    int compressionMethod = src[MAGIC_LENGTH] & 0xFF;
    int compressedLen = readIntLE(src, MAGIC_LENGTH + 1);
    int originalLen = readIntLE(src, MAGIC_LENGTH + 5);
    int check = readIntLE(src, MAGIC_LENGTH + 9);

    switch (compressionMethod) {
      case COMPRESSION_METHOD_RAW:
        System.arraycopy(src, HEADER_LENGTH, dst, dstOff, originalLen);
        break;
      case COMPRESSION_METHOD_ZSTD:
        int originalLen2 =
            (int)
                Zstd.decompressByteArray(
                    dst, dstOff, originalLen, src, HEADER_LENGTH, compressedLen);
        if (originalLen != originalLen2) {
          logger.error(
              "Original length corrupted! expected: {}, actual: {}.", originalLen, originalLen2);
          return -1;
        }
        break;
      default:
        logger.error("Unknown compression method whose decimal number is {} .", compressionMethod);
        return -1;
    }

    checksum.reset();
    checksum.update(dst, dstOff, originalLen);
    if ((int) checksum.getValue() != check) {
      logger.error("Checksum not equal! expected: {}, actual: {}.", check, checksum.getValue());
      return -1;
    }
    return originalLen;
  }
}
