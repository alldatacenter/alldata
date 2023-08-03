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

public class ZstdCompressor extends ZstdTrait implements Compressor {
  private final int compressionLevel;
  private final Checksum checksum;
  private byte[] compressedBuffer;
  private int compressedTotalSize;

  public ZstdCompressor(int blockSize, int level) {
    compressionLevel = level;
    checksum = new CRC32();
    initCompressBuffer(blockSize);
  }

  @Override
  public void initCompressBuffer(int maxDestLength) {
    int compressedBlockSize = HEADER_LENGTH + maxDestLength;
    compressedBuffer = new byte[compressedBlockSize];
    System.arraycopy(MAGIC, 0, compressedBuffer, 0, MAGIC_LENGTH);
  }

  @Override
  public void compress(byte[] data, int offset, int length) {
    checksum.reset();
    checksum.update(data, offset, length);
    final int check = (int) checksum.getValue();
    int maxDestLength = (int) Zstd.compressBound(length);
    if (compressedBuffer.length - HEADER_LENGTH < maxDestLength) {
      initCompressBuffer(maxDestLength);
    }
    int compressedLength =
        (int)
            Zstd.compressByteArray(
                compressedBuffer,
                HEADER_LENGTH,
                maxDestLength - HEADER_LENGTH,
                data,
                offset,
                length,
                compressionLevel);
    final int compressMethod;
    if (compressedLength >= length) {
      compressMethod = COMPRESSION_METHOD_RAW;
      compressedLength = length;
      System.arraycopy(data, offset, compressedBuffer, HEADER_LENGTH, length);
    } else {
      compressMethod = COMPRESSION_METHOD_ZSTD;
    }

    compressedBuffer[MAGIC_LENGTH] = (byte) compressMethod;
    writeIntLE(compressedLength, compressedBuffer, MAGIC_LENGTH + 1);
    writeIntLE(length, compressedBuffer, MAGIC_LENGTH + 5);
    writeIntLE(check, compressedBuffer, MAGIC_LENGTH + 9);

    compressedTotalSize = HEADER_LENGTH + compressedLength;
  }

  @Override
  public int getCompressedTotalSize() {
    return compressedTotalSize;
  }

  @Override
  public byte[] getCompressedBuffer() {
    return compressedBuffer;
  }
}
