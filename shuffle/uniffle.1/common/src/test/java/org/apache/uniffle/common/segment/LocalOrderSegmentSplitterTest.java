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

package org.apache.uniffle.common.segment;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.ShuffleDataSegment;
import org.apache.uniffle.common.ShuffleIndexResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalOrderSegmentSplitterTest {

  @Test
  public void testDiscontinuousMapTaskIds() {
    // case1
    Roaring64NavigableMap taskIds = Roaring64NavigableMap.bitmapOf(6, 7, 9);
    byte[] data = generateData(
        Pair.of(8, 4),
        Pair.of(8, 5),
        Pair.of(8, 6),
        Pair.of(8, 7),
        Pair.of(8, 8),
        Pair.of(8, 9),
        Pair.of(8, 6),
        Pair.of(8, 9)
    );
    List<ShuffleDataSegment> dataSegments1 = new LocalOrderSegmentSplitter(taskIds, 1000)
        .split(new ShuffleIndexResult(data, -1));

    assertEquals(2, dataSegments1.size());
    assertEquals(16, dataSegments1.get(0).getOffset());
    assertEquals(16, dataSegments1.get(0).getLength());
    assertEquals(40, dataSegments1.get(1).getOffset());
    assertEquals(24, dataSegments1.get(1).getLength());

    assertEquals(2, dataSegments1.get(0).getBufferSegments().size());
    assertEquals(3, dataSegments1.get(1).getBufferSegments().size());

    BufferSegment bufferSegment = dataSegments1.get(0).getBufferSegments().get(0);
    assertEquals(0, bufferSegment.getOffset());
    assertEquals(8, bufferSegment.getLength());
    bufferSegment = dataSegments1.get(0).getBufferSegments().get(1);
    assertEquals(8, bufferSegment.getOffset());
    assertEquals(8, bufferSegment.getLength());

    bufferSegment = dataSegments1.get(1).getBufferSegments().get(0);
    assertEquals(0, bufferSegment.getOffset());
    assertEquals(8, bufferSegment.getLength());
    bufferSegment = dataSegments1.get(1).getBufferSegments().get(1);
    assertEquals(8, bufferSegment.getOffset());
    assertEquals(8, bufferSegment.getLength());
    bufferSegment = dataSegments1.get(1).getBufferSegments().get(2);
    assertEquals(16, bufferSegment.getOffset());
    assertEquals(8, bufferSegment.getLength());

    // case2
    taskIds = Roaring64NavigableMap.bitmapOf(1, 2, 4);
    data = generateData(
        Pair.of(1, 1),
        Pair.of(1, 2),
        Pair.of(1, 3),
        Pair.of(1, 4)
    );
    List<ShuffleDataSegment> dataSegments2 =
        new LocalOrderSegmentSplitter(taskIds, 32).split(new ShuffleIndexResult(data, -1));
    assertEquals(2, dataSegments2.size());
    assertEquals(0, dataSegments2.get(0).getOffset());
    assertEquals(2, dataSegments2.get(0).getLength());
    assertEquals(2, dataSegments2.get(0).getBufferSegments().size());

    assertEquals(3, dataSegments2.get(1).getOffset());
    assertEquals(1, dataSegments2.get(1).getLength());
    assertEquals(1, dataSegments2.get(1).getBufferSegments().size());

    // case3
    taskIds = Roaring64NavigableMap.bitmapOf(1, 2, 4);
    data = generateData(
        Pair.of(1, 1),
        Pair.of(1, 2),
        Pair.of(1, 1),
        Pair.of(1, 1),
        Pair.of(1, 3),
        Pair.of(1, 4),
        Pair.of(1, 1)
    );
    List<ShuffleDataSegment> dataSegments3 =
        new LocalOrderSegmentSplitter(taskIds, 3).split(new ShuffleIndexResult(data, -1));
    assertEquals(3, dataSegments3.size());
    assertEquals(0, dataSegments3.get(0).getOffset());
    assertEquals(3, dataSegments3.get(0).getLength());
    assertEquals(3, dataSegments3.get(0).getBufferSegments().size());

    assertEquals(3, dataSegments3.get(1).getOffset());
    assertEquals(1, dataSegments3.get(1).getLength());
    assertEquals(1, dataSegments3.get(1).getBufferSegments().size());

    assertEquals(5, dataSegments3.get(2).getOffset());
    assertEquals(2, dataSegments3.get(2).getLength());
    assertEquals(2, dataSegments3.get(2).getBufferSegments().size());

    // case4
    taskIds = Roaring64NavigableMap.bitmapOf(1, 3);
    data = generateData(
        Pair.of(1, 1),
        Pair.of(1, 2),
        Pair.of(1, 3),
        Pair.of(1, 2),
        Pair.of(1, 4)
    );
    List<ShuffleDataSegment> dataSegments4 =
        new LocalOrderSegmentSplitter(taskIds, 3).split(new ShuffleIndexResult(data, -1));
    assertEquals(2, dataSegments4.size());
  }

  /**
   * When no spark skew optimization and LOCAL_ORDER is enabled,
   * the LOCAL_ORDER split segments should be consistent with the
   * NORMAL
   */
  @ParameterizedTest()
  @ValueSource(ints = {-1, 32, 33, 48, 49, 57})
  public void testConsistentWithFixSizeSplitterWhenNoSkew(int realDataLength) {
    Roaring64NavigableMap taskIds = Roaring64NavigableMap.bitmapOf(1, 2, 3, 4, 5, 6);
    byte[] data = generateData(
        Pair.of(8, 6),
        Pair.of(8, 5),
        Pair.of(8, 4),
        Pair.of(8, 3),
        Pair.of(8, 1),
        Pair.of(8, 2),
        Pair.of(8, 3),
        Pair.of(8, 2),
        Pair.of(8, 3),
        Pair.of(8, 3),
        Pair.of(8, 4),
        Pair.of(8, 5)
    );
    List<ShuffleDataSegment> dataSegments1 = new LocalOrderSegmentSplitter(taskIds, 32)
        .split(new ShuffleIndexResult(data, realDataLength));

    List<ShuffleDataSegment> dataSegments2 = new FixedSizeSegmentSplitter(32)
        .split(new ShuffleIndexResult(data, realDataLength));

    checkConsistency(dataSegments1, dataSegments2);
  }

  private void checkConsistency(List<ShuffleDataSegment> dataSegments1, List<ShuffleDataSegment> dataSegments2) {
    assertEquals(dataSegments1.size(), dataSegments2.size());

    for (int i = 0; i < dataSegments1.size(); i++) {
      ShuffleDataSegment segment1 = dataSegments1.get(i);
      ShuffleDataSegment segment2 = dataSegments2.get(i);

      assertEquals(segment1.getLength(), segment2.getLength());
      assertEquals(segment1.getOffset(), segment2.getOffset());

      List<BufferSegment> bufferSegments1 = segment1.getBufferSegments();
      List<BufferSegment> bufferSegments2 = segment2.getBufferSegments();

      assertEquals(bufferSegments1.size(), bufferSegments2.size());

      for (int j = 0; j < bufferSegments1.size(); j++) {
        BufferSegment bs1 = bufferSegments1.get(j);
        BufferSegment bs2 = bufferSegments2.get(j);
        assertEquals(bs1.getLength(), bs2.getLength());
        assertEquals(bs1.getOffset(), bs2.getOffset());
        assertEquals(bs1.getBlockId(), bs2.getBlockId());
        assertEquals(bs1.getCrc(), bs2.getCrc());
        assertEquals(bs1.getUncompressLength(), bs2.getUncompressLength());
        assertEquals(bs1.getTaskAttemptId(), bs2.getTaskAttemptId());
      }
    }
  }

  @Test
  public void testSplitForMergeContinuousSegments() {
    /**
     * case1: (32, 5) (16, 1) (10, 1) (16, 2) (6, 1) (8, 1) (10, 3) (9, 1)
     *
     * It will skip the (32, 5) and merge others into one dataSegment when no exceeding the
     * read buffer size.
     */
    Roaring64NavigableMap taskIds = Roaring64NavigableMap.bitmapOf(1, 2);
    LocalOrderSegmentSplitter splitter = new LocalOrderSegmentSplitter(taskIds, 1000);
    byte[] data = generateData(
        Pair.of(32, 5),
        Pair.of(16, 1),
        Pair.of(10, 1),
        Pair.of(16, 2),
        Pair.of(6, 1),
        Pair.of(8, 1),
        Pair.of(10, 3),
        Pair.of(9, 1)
    );
    List<ShuffleDataSegment> dataSegments = splitter.split(new ShuffleIndexResult(data, -1));
    assertEquals(2, dataSegments.size());
    assertEquals(32, dataSegments.get(0).getOffset());
    assertEquals(56, dataSegments.get(0).getLength());

    List<BufferSegment> bufferSegments = dataSegments.get(0).getBufferSegments();
    assertEquals(0, bufferSegments.get(0).getOffset());
    assertEquals(16, bufferSegments.get(0).getLength());

    assertEquals(16, bufferSegments.get(1).getOffset());
    assertEquals(10, bufferSegments.get(1).getLength());

    assertEquals(26, bufferSegments.get(2).getOffset());
    assertEquals(16, bufferSegments.get(2).getLength());

    assertEquals(42, bufferSegments.get(3).getOffset());
    assertEquals(6, bufferSegments.get(3).getLength());

    assertEquals(48, bufferSegments.get(4).getOffset());
    assertEquals(8, bufferSegments.get(4).getLength());

    assertEquals(98, dataSegments.get(1).getOffset());
    assertEquals(9, dataSegments.get(1).getLength());
    bufferSegments = dataSegments.get(1).getBufferSegments();
    assertEquals(1, bufferSegments.size());
    assertEquals(0, bufferSegments.get(0).getOffset());
    assertEquals(9, bufferSegments.get(0).getLength());

    /**
     * case2: (16, 1) (16, 2) (6, 1)
     *
     * It will skip merging into one dataSegment when exceeding the
     * read buffer size.
     */
    data = generateData(
        Pair.of(16, 1),
        Pair.of(15, 2),
        Pair.of(1, 1),
        Pair.of(6, 1)
    );
    dataSegments = new LocalOrderSegmentSplitter(taskIds, 32).split(new ShuffleIndexResult(data, -1));
    assertEquals(2, dataSegments.size());
    assertEquals(0, dataSegments.get(0).getOffset());
    assertEquals(32, dataSegments.get(0).getLength());
    assertEquals(32, dataSegments.get(1).getOffset());
    assertEquals(6, dataSegments.get(1).getLength());
  }

  @Test
  public void testSplit() {
    Roaring64NavigableMap taskIds = Roaring64NavigableMap.bitmapOf(1);
    LocalOrderSegmentSplitter splitter = new LocalOrderSegmentSplitter(taskIds, 1000);
    assertTrue(splitter.split(new ShuffleIndexResult()).isEmpty());

    splitter = new LocalOrderSegmentSplitter(taskIds, 32);

    /**
     * (length, taskId)
     * case1: (32, 1) (16, 1) (10, 2) (16, 1) (6, 1)
     *
     *        (10, 2) will be dropped
     */
    byte[] data = generateData(
        Pair.of(32, 1),
        Pair.of(16, 1),
        Pair.of(10, 2),
        Pair.of(16, 1),
        Pair.of(6, 1)
    );
    List<ShuffleDataSegment> dataSegments = splitter.split(new ShuffleIndexResult(data, -1));
    assertEquals(3, dataSegments.size());

    assertEquals(0, dataSegments.get(0).getOffset());
    assertEquals(32, dataSegments.get(0).getLength());

    assertEquals(32, dataSegments.get(1).getOffset());
    assertEquals(16, dataSegments.get(1).getLength());

    assertEquals(58, dataSegments.get(2).getOffset());
    assertEquals(22, dataSegments.get(2).getLength());

    /**
     * case2: (32, 2) (16, 1) (10, 1) (16, 2) (6, 1)
     *
     *        (32, 2) (16, 2) will be dropped
     */
    data = generateData(
        Pair.of(32, 2),
        Pair.of(16, 1),
        Pair.of(10, 1),
        Pair.of(16, 2),
        Pair.of(6, 1)
    );
    dataSegments = splitter.split(new ShuffleIndexResult(data, -1));
    assertEquals(2, dataSegments.size());

    assertEquals(32, dataSegments.get(0).getOffset());
    assertEquals(26, dataSegments.get(0).getLength());

    assertEquals(74, dataSegments.get(1).getOffset());
    assertEquals(6, dataSegments.get(1).getLength());

    /**
     * case3: (32, 5) (16, 1) (10, 3) (16, 4) (6, 1)
     *
     *        (32, 5) will be dropped
     */
    taskIds = Roaring64NavigableMap.bitmapOf(1, 2, 3, 4);
    splitter = new LocalOrderSegmentSplitter(taskIds, 32);
    data = generateData(
        Pair.of(32, 5),
        Pair.of(16, 1),
        Pair.of(10, 3),
        Pair.of(16, 4),
        Pair.of(6, 1)
    );
    dataSegments = splitter.split(new ShuffleIndexResult(data, -1));
    assertEquals(2, dataSegments.size());

    assertEquals(32, dataSegments.get(0).getOffset());
    assertEquals(42, dataSegments.get(0).getLength());

    assertEquals(74, dataSegments.get(1).getOffset());
    assertEquals(6, dataSegments.get(1).getLength());

    /**
     * case4
     */
    data = generateData(
        Pair.of(16, 229),
        Pair.of(16, 230),
        Pair.of(16, 221),
        Pair.of(16, 229),
        Pair.of(16, 230)
    );
    taskIds = Roaring64NavigableMap.bitmapOf(230);
    dataSegments = new LocalOrderSegmentSplitter(taskIds, 10000).split(new ShuffleIndexResult(data, -1));
    assertEquals(2, dataSegments.size());
    assertEquals(16, dataSegments.get(0).getOffset());
    assertEquals(16, dataSegments.get(0).getLength());
    assertEquals(64, dataSegments.get(1).getOffset());
    assertEquals(16, dataSegments.get(1).getLength());

    /**
     * case5
     */
    data = generateData(
        Pair.of(1, 2),
        Pair.of(1, 3),
        Pair.of(1, 4),
        Pair.of(1, 5),
        Pair.of(1, 6),
        Pair.of(1, 4),
        Pair.of(1, 5),
        Pair.of(1, 6)
    );
    taskIds = Roaring64NavigableMap.bitmapOf(2, 3, 4);
    dataSegments = new LocalOrderSegmentSplitter(taskIds, 10000).split(new ShuffleIndexResult(data, -1));
    assertEquals(2, dataSegments.size());
    assertEquals(0, dataSegments.get(0).getOffset());
    assertEquals(3, dataSegments.get(0).getLength());
    assertEquals(5, dataSegments.get(1).getOffset());
    assertEquals(1, dataSegments.get(1).getLength());
  }

  public static byte[] generateData(Pair<Integer, Integer>... configEntries) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(configEntries.length * 40);
    int total = 0;
    for (Pair<Integer, Integer> entry : configEntries) {
      byteBuffer.putLong(total);
      byteBuffer.putInt(entry.getLeft());
      byteBuffer.putInt(1);
      byteBuffer.putLong(1);
      byteBuffer.putLong(1);
      byteBuffer.putLong(entry.getRight());

      total += entry.getLeft();
    }
    return byteBuffer.array();
  }
}
