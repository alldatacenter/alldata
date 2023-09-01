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

package org.apache.uniffle.common;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ShufflePartitionedBlockTest {

  @Test
  public void shufflePartitionedBlockTest() {
    byte[] buf = new byte[3];
    new Random().nextBytes(buf);

    ShufflePartitionedBlock b1 = new ShufflePartitionedBlock(1, 1, 2, 3, 1, buf);
    assertEquals(1, b1.getLength());
    assertEquals(2, b1.getCrc());
    assertEquals(3, b1.getBlockId());

    ShufflePartitionedBlock b3 = new ShufflePartitionedBlock(1, 1, 2, 3, 3, buf);
    assertArrayEquals(buf, b3.getData());
  }

  @Test
  public void testEquals() {
    ShufflePartitionedBlock b1 = new ShufflePartitionedBlock(1, 2, 3, 4, 5, new byte[6]);
    ShufflePartitionedBlock b2 = new ShufflePartitionedBlock(1, 6, 3, 4, 7, new byte[6]);
    assertEquals(b1, b1);
    assertEquals(b1.hashCode(), b1.hashCode());
    assertEquals(b1, b2);
    assertEquals(b1.hashCode(), b2.hashCode());
    assertNotEquals(b1, null);
    assertNotEquals(b1, new Object());
  }

  @ParameterizedTest
  @CsvSource({
      "5, 2, 3, 4",
      "1, 5, 3, 4",
      "1, 2, 5, 4",
      "1, 2, 3, 5"
  })
  public void testNotEquals(int length, long crc, long blockId, int dataSize) {
    ShufflePartitionedBlock b1 = new ShufflePartitionedBlock(1, 0, 2, 3, 0, new byte[4]);
    ShufflePartitionedBlock b2 = new ShufflePartitionedBlock(length, 0, crc, blockId, 0, new byte[dataSize]);
    assertNotEquals(b1, b2);
  }

  @Test
  public void testToString() {
    ShufflePartitionedBlock b1 = new ShufflePartitionedBlock(1, 2, 3, 4, 5, new byte[6]);
    assertEquals("ShufflePartitionedBlock{blockId[" + b1.getBlockId()
        + "], length[" + b1.getLength()
        + "], uncompressLength[" + b1.getUncompressLength()
        + "], crc[" + b1.getCrc()
        + "], taskAttemptId[" + b1.getTaskAttemptId() + "]}",
        b1.toString());
  }

  @Test
  public void testSize() {
    ShufflePartitionedBlock b1 = new ShufflePartitionedBlock(1, 2, 3, 4, 5, new byte[6]);
    assertEquals(b1.getSize(), b1.getLength() + 3 * Long.BYTES + 2 * Integer.BYTES);
  }

}
