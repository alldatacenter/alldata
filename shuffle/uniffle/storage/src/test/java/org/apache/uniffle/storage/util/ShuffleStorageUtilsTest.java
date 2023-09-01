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

package org.apache.uniffle.storage.util;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.storage.common.FileBasedShuffleSegment;
import org.apache.uniffle.storage.handler.impl.DataFileSegment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ShuffleStorageUtilsTest {

  @Test
  public void mergeSegmentsTest() {
    List<FileBasedShuffleSegment> segments = Lists.newArrayList(
        new FileBasedShuffleSegment(1, 0, 40, 0, 0, 0));
    List<DataFileSegment> fileSegments = ShuffleStorageUtils.mergeSegments("path", segments, 100);
    assertEquals(1, fileSegments.size());
    for (DataFileSegment seg : fileSegments) {
      assertEquals(0, seg.getOffset());
      assertEquals(40, seg.getLength());
      assertEquals("path", seg.getPath());
      List<BufferSegment> bufferSegments = seg.getBufferSegments();
      assertEquals(1, bufferSegments.size());
      assertEquals(new BufferSegment(1, 0, 40, 0, 0, 0), bufferSegments.get(0));
    }

    segments = Lists.newArrayList(
        new FileBasedShuffleSegment(1, 0, 40, 0, 0, 0),
        new FileBasedShuffleSegment(2, 40, 40, 0, 0, 0),
        new FileBasedShuffleSegment(3, 80, 20, 0, 0, 0));
    fileSegments = ShuffleStorageUtils.mergeSegments("path", segments, 100);
    assertEquals(1, fileSegments.size());
    for (DataFileSegment seg : fileSegments) {
      assertEquals(0, seg.getOffset());
      assertEquals(100, seg.getLength());
      assertEquals("path", seg.getPath());
      List<BufferSegment> bufferSegments = seg.getBufferSegments();
      assertEquals(3, bufferSegments.size());
      Set<Long> testedBlockIds = Sets.newHashSet();
      for (BufferSegment segment : bufferSegments) {
        if (segment.getBlockId() == 1) {
          assertTrue(segment.equals(new BufferSegment(1, 0, 40, 0, 0, 0)));
          testedBlockIds.add(1L);
        } else if (segment.getBlockId() == 2) {
          assertTrue(segment.equals(new BufferSegment(2, 40, 40, 0, 0, 0)));
          testedBlockIds.add(2L);
        } else if (segment.getBlockId() == 3) {
          assertTrue(segment.equals(new BufferSegment(3, 80, 20, 0, 0, 0)));
          testedBlockIds.add(3L);
        }
      }
      assertEquals(3, testedBlockIds.size());
    }

    segments = Lists.newArrayList(
        new FileBasedShuffleSegment(1, 0, 40, 0, 0, 0),
        new FileBasedShuffleSegment(2, 40, 40, 0, 0, 0),
        new FileBasedShuffleSegment(3, 80, 20, 0, 0, 0),
        new FileBasedShuffleSegment(4, 100, 20, 0, 0, 0));
    fileSegments = ShuffleStorageUtils.mergeSegments("path", segments, 100);
    assertEquals(2, fileSegments.size());
    boolean tested = false;
    for (DataFileSegment seg : fileSegments) {
      if (seg.getOffset() == 100) {
        tested = true;
        assertEquals(20, seg.getLength());
        assertEquals("path", seg.getPath());
        List<BufferSegment> bufferSegments = seg.getBufferSegments();
        assertEquals(1, bufferSegments.size());
        assertTrue(bufferSegments.get(0).equals(new BufferSegment(4, 0, 20, 0, 0, 0)));
      }
    }
    assertTrue(tested);

    segments = Lists.newArrayList(
        new FileBasedShuffleSegment(1, 0, 40, 0, 0, 0),
        new FileBasedShuffleSegment(2, 40, 40, 0, 0, 0),
        new FileBasedShuffleSegment(3, 80, 20, 0, 0, 0),
        new FileBasedShuffleSegment(4, 100, 20, 0, 0, 0),
        new FileBasedShuffleSegment(5, 120, 100, 0, 0, 0));
    fileSegments = ShuffleStorageUtils.mergeSegments("path", segments, 100);
    assertEquals(2, fileSegments.size());
    tested = false;
    for (DataFileSegment seg : fileSegments) {
      if (seg.getOffset() == 100) {
        tested = true;
        assertEquals(120, seg.getLength());
        assertEquals("path", seg.getPath());
        List<BufferSegment> bufferSegments = seg.getBufferSegments();
        assertEquals(2, bufferSegments.size());
        Set<Long> testedBlockIds = Sets.newHashSet();
        for (BufferSegment segment : bufferSegments) {
          if (segment.getBlockId() == 4) {
            assertTrue(segment.equals(new BufferSegment(4, 0, 20, 0, 0, 0)));
            testedBlockIds.add(4L);
          } else if (segment.getBlockId() == 5) {
            assertTrue(segment.equals(new BufferSegment(5, 20, 100, 0, 0, 0)));
            testedBlockIds.add(5L);
          }
        }
        assertEquals(2, testedBlockIds.size());
      }
    }
    assertTrue(tested);

    segments = Lists.newArrayList(
        new FileBasedShuffleSegment(1, 10, 40, 0, 0, 0),
        new FileBasedShuffleSegment(2, 80, 20, 0, 0, 0),
        new FileBasedShuffleSegment(3, 500, 120, 0, 0, 0),
        new FileBasedShuffleSegment(4, 700, 20, 0, 0, 0));
    fileSegments = ShuffleStorageUtils.mergeSegments("path", segments, 100);
    assertEquals(3, fileSegments.size());
    Set<Long> expectedOffset = Sets.newHashSet(10L, 500L, 700L);
    for (DataFileSegment seg : fileSegments) {
      if (seg.getOffset() == 10) {
        validResult(seg, 90, 1, 40, 2, 70);
        expectedOffset.remove(10L);
      }
      if (seg.getOffset() == 500) {
        assertEquals(120, seg.getLength());
        List<BufferSegment> bufferSegments = seg.getBufferSegments();
        assertEquals(1, bufferSegments.size());
        assertTrue(bufferSegments.get(0).equals(new BufferSegment(3, 0, 120, 0, 0, 0)));
        expectedOffset.remove(500L);
      }
      if (seg.getOffset() == 700) {
        assertEquals(20, seg.getLength());
        List<BufferSegment> bufferSegments = seg.getBufferSegments();
        assertEquals(1, bufferSegments.size());
        assertTrue(bufferSegments.get(0).equals(new BufferSegment(4, 0, 20, 0, 0, 0)));
        expectedOffset.remove(700L);
      }
    }
    assertTrue(expectedOffset.isEmpty());

    segments = Lists.newArrayList(
        new FileBasedShuffleSegment(5, 500, 120, 0, 0, 0),
        new FileBasedShuffleSegment(3, 630, 10, 0, 0, 0),
        new FileBasedShuffleSegment(2, 80, 20, 0, 0, 0),
        new FileBasedShuffleSegment(1, 10, 40, 0, 0, 0),
        new FileBasedShuffleSegment(6, 769, 20, 0, 0, 0),
        new FileBasedShuffleSegment(4, 700, 20, 0, 0, 0));
    fileSegments = ShuffleStorageUtils.mergeSegments("path", segments, 100);
    assertEquals(4, fileSegments.size());
    expectedOffset = Sets.newHashSet(10L, 500L, 630L, 700L);
    for (DataFileSegment seg : fileSegments) {
      if (seg.getOffset() == 10) {
        validResult(seg, 90, 1, 40, 2, 70);
        expectedOffset.remove(10L);
      }
      if (seg.getOffset() == 500) {
        assertEquals(120, seg.getLength());
        List<BufferSegment> bufferSegments = seg.getBufferSegments();
        assertEquals(1, bufferSegments.size());
        assertTrue(bufferSegments.get(0).equals(new BufferSegment(5, 0, 120, 0, 0, 0)));
        expectedOffset.remove(500L);
      }
      if (seg.getOffset() == 630) {
        assertEquals(10, seg.getLength());
        List<BufferSegment> bufferSegments = seg.getBufferSegments();
        assertEquals(1, bufferSegments.size());
        assertTrue(bufferSegments.get(0).equals(new BufferSegment(3, 0, 10, 0, 0, 0)));
        expectedOffset.remove(630L);
      }
      if (seg.getOffset() == 700) {
        validResult(seg, 89, 4, 20, 6, 69);
        expectedOffset.remove(700L);
      }
    }
    assertTrue(expectedOffset.isEmpty());
  }

  private void validResult(
      DataFileSegment seg,
      int length,
      int someBlockId,
      int someLength,
      int anotherBlockId,
      int anotherOffset) {
    assertEquals(length, seg.getLength());
    List<BufferSegment> bufferSegments = seg.getBufferSegments();
    assertEquals(2, bufferSegments.size());
    Set<Long> testedBlockIds = Sets.newHashSet();
    for (BufferSegment segment : bufferSegments) {
      if (segment.getBlockId() == someBlockId) {
        assertTrue(segment.equals(new BufferSegment(someBlockId, 0, someLength, 0, 0, 0)));
        testedBlockIds.add((long)someBlockId);
      } else if (segment.getBlockId() == anotherBlockId) {
        assertTrue(segment.equals(new BufferSegment(anotherBlockId, anotherOffset, 20, 0, 0, 0)));
        testedBlockIds.add((long)anotherBlockId);
      }
    }
    assertEquals(2, testedBlockIds.size());
  }

  @Test
  public void getShuffleDataPathWithRangeTest() {
    String result = ShuffleStorageUtils.getShuffleDataPathWithRange("appId", 0, 1, 3, 6);
    assertEquals("appId/0/0-2", result);
    result = ShuffleStorageUtils.getShuffleDataPathWithRange("appId", 0, 2, 3, 6);
    assertEquals("appId/0/0-2", result);
    result = ShuffleStorageUtils.getShuffleDataPathWithRange("appId", 0, 3, 3, 6);
    assertEquals("appId/0/3-5", result);
    result = ShuffleStorageUtils.getShuffleDataPathWithRange("appId", 0, 5, 3, 6);
    assertEquals("appId/0/3-5", result);
    try {
      ShuffleStorageUtils.getShuffleDataPathWithRange("appId", 0, 6, 3, 6);
      fail("shouldn't be here");
    } catch (Exception e) {
      assertTrue(e.getMessage().startsWith("Can't generate ShuffleData Path"));
    }
    result = ShuffleStorageUtils.getShuffleDataPathWithRange("appId", 0, 6, 3, 7);
    assertEquals("appId/0/6-8", result);
  }

  @Test
  public void getStorageIndexTest() {
    int index = ShuffleStorageUtils.getStorageIndex(3, "abcde", 3, 1);
    assertEquals(2, index);
    index = ShuffleStorageUtils.getStorageIndex(3, "abcde", 3, 4);
    assertEquals(1, index);
  }

  @Test
  public void getPartitionRangeTest() {
    int[] range = ShuffleStorageUtils.getPartitionRange(0, 1, 5);
    assertEquals(0, range[0]);
    assertEquals(0, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(5, 1, 5);
    assertEquals(null, range);
    range = ShuffleStorageUtils.getPartitionRange(0, 2, 5);
    assertEquals(0, range[0]);
    assertEquals(1, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(1, 2, 5);
    assertEquals(0, range[0]);
    assertEquals(1, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(0, 3, 5);
    assertEquals(0, range[0]);
    assertEquals(2, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(1, 3, 5);
    assertEquals(0, range[0]);
    assertEquals(2, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(2, 3, 5);
    assertEquals(0, range[0]);
    assertEquals(2, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(-1, 3, 5);
    assertEquals(null, range);
    range = ShuffleStorageUtils.getPartitionRange(1, 3, 0);
    assertEquals(null, range);
    range = ShuffleStorageUtils.getPartitionRange(4, 2, 3);
    assertEquals(null, range);
    range = ShuffleStorageUtils.getPartitionRange(4, 2, 5);
    assertEquals(4, range[0]);
    assertEquals(5, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(3, 2, 2);
    assertEquals(null, range);
    range = ShuffleStorageUtils.getPartitionRange(0, 2, 2);
    assertEquals(0, range[0]);
    assertEquals(1, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(3, 2, 4);
    assertEquals(2, range[0]);
    assertEquals(3, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(4, 3, 5);
    assertEquals(3, range[0]);
    assertEquals(5, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(5, 3, 5);
    assertEquals(null, range);
    range = ShuffleStorageUtils.getPartitionRange(0, 3, 5);
    assertEquals(0, range[0]);
    assertEquals(2, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(9, 3, 11);
    assertEquals(9, range[0]);
    assertEquals(11, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(9, 3, 12);
    assertEquals(9, range[0]);
    assertEquals(11, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(0, 7, 12);
    assertEquals(0, range[0]);
    assertEquals(6, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(3, 7, 12);
    assertEquals(0, range[0]);
    assertEquals(6, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(7, 7, 12);
    assertEquals(7, range[0]);
    assertEquals(13, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(8, 7, 12);
    assertEquals(7, range[0]);
    assertEquals(13, range[1]);
    range = ShuffleStorageUtils.getPartitionRange(12, 7, 12);
    assertEquals(null, range);
  }
}
