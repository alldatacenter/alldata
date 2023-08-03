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

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;

import org.apache.celeborn.common.CelebornConf;

public class CodecSuiteJ {

  @Test
  public void testLz4Codec() {
    int blockSize = (new CelebornConf()).clientPushBufferMaxSize();
    Lz4Compressor lz4Compressor = new Lz4Compressor(blockSize);
    byte[] data = RandomStringUtils.random(1024).getBytes(StandardCharsets.UTF_8);
    int oriLength = data.length;
    lz4Compressor.compress(data, 0, oriLength);

    Lz4Decompressor lz4Decompressor = new Lz4Decompressor();
    byte[] dst = new byte[oriLength];
    int decompressLength = lz4Decompressor.decompress(lz4Compressor.getCompressedBuffer(), dst, 0);

    Assert.assertNotEquals(-1, decompressLength);
    Assert.assertEquals(oriLength, decompressLength);
    Assert.assertArrayEquals(data, dst);
  }

  @Test
  public void testZstdCodec() {
    for (int level = -5; level <= 22; level++) {
      int blockSize = (new CelebornConf()).clientPushBufferMaxSize();
      ZstdCompressor zstdCompressor = new ZstdCompressor(blockSize, level);
      byte[] data = RandomStringUtils.random(1024).getBytes(StandardCharsets.UTF_8);
      int oriLength = data.length;
      zstdCompressor.compress(data, 0, oriLength);

      ZstdDecompressor zstdDecompressor = new ZstdDecompressor();
      byte[] dst = new byte[oriLength];
      int decompressLength =
          zstdDecompressor.decompress(zstdCompressor.getCompressedBuffer(), dst, 0);

      Assert.assertNotEquals(-1, decompressLength);
      Assert.assertEquals(oriLength, decompressLength);
      Assert.assertArrayEquals(data, dst);
    }
  }
}
