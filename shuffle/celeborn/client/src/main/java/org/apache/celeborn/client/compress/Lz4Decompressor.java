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

import java.util.zip.Checksum;

import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import net.jpountz.xxhash.XXHashFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Lz4Decompressor extends Lz4Trait implements Decompressor {
  private static final Logger logger = LoggerFactory.getLogger(Lz4Decompressor.class);
  private final LZ4FastDecompressor decompressor;
  private final Checksum checksum;

  public Lz4Decompressor() {
    decompressor = LZ4Factory.fastestInstance().fastDecompressor();
    checksum = XXHashFactory.fastestInstance().newStreamingHash32(DEFAULT_SEED).asChecksum();
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
      case COMPRESSION_METHOD_LZ4:
        int compressedLen2 = decompressor.decompress(src, HEADER_LENGTH, dst, dstOff, originalLen);
        if (compressedLen != compressedLen2) {
          logger.error(
              "Compressed length corrupted! expected: {}, actual: {}.",
              compressedLen,
              compressedLen2);
          return -1;
        }
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
