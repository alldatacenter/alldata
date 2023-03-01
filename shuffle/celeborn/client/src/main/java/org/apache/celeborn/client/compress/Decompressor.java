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

public interface Decompressor {

  int decompress(byte[] src, byte[] dst, int dstOff);

  int getOriginalLen(byte[] src);

  default int readIntLE(byte[] buf, int i) {
    return (buf[i] & 0xFF)
        | ((buf[i + 1] & 0xFF) << 8)
        | ((buf[i + 2] & 0xFF) << 16)
        | ((buf[i + 3] & 0xFF) << 24);
  }

  static Decompressor getDecompressor(CelebornConf conf) {
    CompressionCodec codec = conf.shuffleCompressionCodec();
    switch (codec) {
      case LZ4:
        return new RssLz4Decompressor();
      case ZSTD:
        return new RssZstdDecompressor();
      default:
        throw new IllegalArgumentException("Unknown compression codec: " + codec);
    }
  }

  static int getCompressionHeaderLength(CelebornConf conf) {
    CompressionCodec codec = conf.shuffleCompressionCodec();
    switch (codec) {
      case LZ4:
        return RssLz4Trait.HEADER_LENGTH;
      case ZSTD:
        return RssZstdTrait.HEADER_LENGTH;
      default:
        throw new IllegalArgumentException("Unknown compression codec: " + codec);
    }
  }
}
