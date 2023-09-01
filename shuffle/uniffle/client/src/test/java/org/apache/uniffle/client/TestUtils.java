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

package org.apache.uniffle.client;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.apache.uniffle.client.api.ShuffleReadClient;
import org.apache.uniffle.client.response.CompressedShuffleBlock;
import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.ShuffleDataResult;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUtils {

  private TestUtils() {

  }

  public static void validateResult(ShuffleReadClient readClient,
                                Map<Long, byte[]> expectedData) {
    ByteBuffer data = readClient.readShuffleBlockData().getByteBuffer();
    int blockNum = 0;
    while (data != null) {
      blockNum++;
      boolean match = false;
      for (byte[] expected : expectedData.values()) {
        if (compareByte(expected, data)) {
          match = true;
        }
      }
      assertTrue(match);
      CompressedShuffleBlock csb = readClient.readShuffleBlockData();
      if (csb == null) {
        data = null;
      } else {
        data = csb.getByteBuffer();
      }
    }
    assertEquals(expectedData.size(), blockNum);
  }

  public static void validateResult(
      Map<Long, byte[]> expectedData,
      ShuffleDataResult sdr) {
    byte[] buffer = sdr.getData();
    List<BufferSegment> bufferSegments = sdr.getBufferSegments();
    assertEquals(expectedData.size(), bufferSegments.size());
    for (Map.Entry<Long, byte[]> entry : expectedData.entrySet()) {
      BufferSegment bs = findBufferSegment(entry.getKey(), bufferSegments);
      assertNotNull(bs);
      byte[] data = new byte[bs.getLength()];
      System.arraycopy(buffer, bs.getOffset(), data, 0, bs.getLength());
      assertArrayEquals(entry.getValue(), data);
    }
  }

  private static BufferSegment findBufferSegment(long blockId, List<BufferSegment> bufferSegments) {
    for (BufferSegment bs : bufferSegments) {
      if (bs.getBlockId() == blockId) {
        return bs;
      }
    }
    return null;
  }

  public static boolean compareByte(byte[] expected, ByteBuffer buffer) {
    int start = buffer.position();
    for (int i = 0; i < expected.length; i++) {
      if (expected[i] != buffer.get(start + i)) {
        return false;
      }
    }
    return true;
  }
}
