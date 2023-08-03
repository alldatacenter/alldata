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

import java.io.IOException;
import java.nio.ByteBuffer;

import org.xerial.snappy.Snappy;

import org.apache.uniffle.common.exception.RssException;

public class SnappyCodec extends Codec {
  @Override
  public void decompress(ByteBuffer src, int uncompressedLen, ByteBuffer dest, int destOffset) {
    try {
      if (!src.isDirect() && !dest.isDirect()) {
        int size = Snappy.uncompress(src.array(), src.position(), src.limit() - src.position(), dest.array(),
            destOffset);
        if (size != uncompressedLen) {
          throw new RssException(
            "This should not happen that the decompressed data size is not equals to original size.");
        }
        return;
      }
      if (src.isDirect() && dest.isDirect()) {
        if (destOffset != 0) {
          throw new RssException(
            "Snappy decompression does not support non-zero offset for destination direct ByteBuffer");
        }
        int size = Snappy.uncompress(src, dest);
        if (size != uncompressedLen) {
          throw new RssException(
            "This should not happen that the decompressed data size is not equals to original size.");
        }
        return;
      }
    } catch (IOException e) {
      throw new RssException("Failed to uncompress by Snappy", e);
    }

    throw new IllegalStateException("Snappy only supports the same type of bytebuffer decompression.");
  }

  @Override
  public byte[] compress(byte[] src) {
    try {
      return Snappy.compress(src);
    } catch (IOException e) {
      throw new RssException("Failed to uncompress by Snappy", e);
    }
  }
}
