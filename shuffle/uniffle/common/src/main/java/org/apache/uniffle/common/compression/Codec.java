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

package org.apache.uniffle.common.compression;

import java.nio.ByteBuffer;

import org.apache.uniffle.common.config.RssConf;

import static org.apache.uniffle.common.config.RssClientConf.COMPRESSION_TYPE;
import static org.apache.uniffle.common.config.RssClientConf.ZSTD_COMPRESSION_LEVEL;

public abstract class Codec {

  public static Codec newInstance(RssConf rssConf) {
    Type type = rssConf.get(COMPRESSION_TYPE);
    switch (type) {
      case ZSTD:
        return new ZstdCodec(rssConf.get(ZSTD_COMPRESSION_LEVEL));
      case SNAPPY:
        return new SnappyCodec();
      case NOOP:
        return new NoOpCodec();
      case LZ4:
      default:
        return new Lz4Codec();
    }
  }

  /**
   *
   * @param src
   * @param uncompressedLen
   * @param dest
   * @param destOffset
   */
  public abstract void decompress(ByteBuffer src, int uncompressedLen, ByteBuffer dest, int destOffset);

  /**
   *  Compress bytes into a byte array.
   */
  public abstract byte[] compress(byte[] src);

  public enum Type {
    LZ4,
    ZSTD,
    NOOP,
    SNAPPY,
  }
}
