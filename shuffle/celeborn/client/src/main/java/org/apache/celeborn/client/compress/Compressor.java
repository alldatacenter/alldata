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

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.protocol.CompressionCodec;
import org.apache.celeborn.common.protocol.CompressionCodec.*;

public interface Compressor {

  void initCompressBuffer(int maxDestLength);

  void compress(byte[] data, int offset, int length);

  int getCompressedTotalSize();

  byte[] getCompressedBuffer();

  default void writeIntLE(int i, byte[] buf, int off) {
    buf[off++] = (byte) i;
    buf[off++] = (byte) (i >>> 8);
    buf[off++] = (byte) (i >>> 16);
    buf[off++] = (byte) (i >>> 24);
  }

  static Compressor getCompressor(CelebornConf conf) {
    CompressionCodec codec = conf.shuffleCompressionCodec();
    int blockSize = conf.pushBufferMaxSize();
    switch (codec) {
      case LZ4:
        return new RssLz4Compressor(blockSize);
      case ZSTD:
        int zstdLevel = conf.shuffleCompressionZstdCompressLevel();
        return new RssZstdCompressor(blockSize, zstdLevel);
      default:
        throw new IllegalArgumentException("Unknown compression codec: " + codec);
    }
  }
}
