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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BufferSegmentTest {

  @Test
  public void testEquals() {
    BufferSegment segment1 = new BufferSegment(0, 1, 2, 3, 4, 5);
    BufferSegment segment2 = new BufferSegment(0, 1, 2, 3, 4, 5);
    assertEquals(segment1, segment2);
    assertEquals(segment1.hashCode(), segment2.hashCode());
    assertNotEquals(segment1, null);
    assertNotEquals(segment1, new Object());
  }

  @ParameterizedTest
  @CsvSource({
      "6, 1, 2, 3, 4, 5",
      "0, 6, 2, 3, 4, 5",
      "0, 1, 6, 3, 4, 5",
      "0, 1, 2, 6, 4, 5",
      "0, 1, 2, 3, 6, 5",
      "0, 1, 2, 3, 4, 6",
  })
  public void testNotEquals(long blockId, long offset, int length, int uncompressLength, long crc, long taskAttemptId) {
    BufferSegment segment1 = new BufferSegment(0, 1, 2, 3, 4, 5);
    BufferSegment segment2 = new BufferSegment(blockId, offset, length, uncompressLength, crc, taskAttemptId);
    assertNotEquals(segment1, segment2);
  }

  @Test
  public void testToString() {
    BufferSegment segment = new BufferSegment(0, 1, 2, 3, 4, 5);
    assertEquals("BufferSegment{blockId[0], taskAttemptId[5], offset[1], length[2], crc[4], uncompressLength[3]}",
        segment.toString());
  }

  @Test
  public void testGetOffset() {
    BufferSegment segment1 = new BufferSegment(0, Integer.MAX_VALUE, 2, 3, 4, 5);
    assertEquals(Integer.MAX_VALUE, segment1.getOffset());
    BufferSegment segment2 = new BufferSegment(0, (long) Integer.MAX_VALUE + 1, 2, 3, 4, 5);
    assertThrows(RuntimeException.class, segment2::getOffset);
  }

}
