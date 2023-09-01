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

import org.apache.uniffle.common.ShuffleDataSegment;
import org.apache.uniffle.common.ShuffleIndexResult;

import static org.apache.uniffle.common.segment.LocalOrderSegmentSplitterTest.generateData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class FixedSizeSegmentSplitterTest {

  @ParameterizedTest
  @ValueSource(ints = {48, 49, 57})
  public void testAvoidEOFException(int dataLength) {
    SegmentSplitter splitter = new FixedSizeSegmentSplitter(1000);
    byte[] data = generateData(
        Pair.of(32, 0),
        Pair.of(16, 0),
        Pair.of(10, 0)
    );

    List<ShuffleDataSegment> shuffleDataSegments = splitter.split(new ShuffleIndexResult(data, dataLength));
    assertEquals(1, shuffleDataSegments.size());
    assertEquals(0, shuffleDataSegments.get(0).getOffset());
    assertEquals(48, shuffleDataSegments.get(0).getLength());
  }

  @Test
  public void testSplit() {
    SegmentSplitter splitter = new FixedSizeSegmentSplitter(100);
    ShuffleIndexResult shuffleIndexResult = new ShuffleIndexResult();
    List<ShuffleDataSegment> shuffleDataSegments = splitter.split(shuffleIndexResult);
    assertTrue(shuffleDataSegments.isEmpty());

    int readBufferSize = 32;
    splitter = new FixedSizeSegmentSplitter(32);

    // those 5 segment's data length are [32, 16, 10, 32, 6] so the index should be
    // split into 3 ShuffleDataSegment, which are [32, 16 + 10 + 32, 6]
    byte[] data = generateData(
        Pair.of(32, 0),
        Pair.of(16, 0),
        Pair.of(10, 0),
        Pair.of(32, 6),
        Pair.of(6, 0)
    );
    shuffleDataSegments = splitter.split(new ShuffleIndexResult(data, -1));
    assertEquals(3, shuffleDataSegments.size());

    assertEquals(0, shuffleDataSegments.get(0).getOffset());
    assertEquals(32, shuffleDataSegments.get(0).getLength());
    assertEquals(1, shuffleDataSegments.get(0).getBufferSegments().size());

    assertEquals(32, shuffleDataSegments.get(1).getOffset());
    assertEquals(58, shuffleDataSegments.get(1).getLength());
    assertEquals(3,shuffleDataSegments.get(1).getBufferSegments().size());

    assertEquals(90, shuffleDataSegments.get(2).getOffset());
    assertEquals(6, shuffleDataSegments.get(2).getLength());
    assertEquals(1, shuffleDataSegments.get(2).getBufferSegments().size());

    ByteBuffer incompleteByteBuffer = ByteBuffer.allocate(12);
    incompleteByteBuffer.putLong(1L);
    incompleteByteBuffer.putInt(2);
    data = incompleteByteBuffer.array();
    // It should throw exception
    try {
      splitter.split(new ShuffleIndexResult(data, -1));
      fail();
    } catch (Exception e) {
      // ignore
      assertTrue(e.getMessage().contains("Read index data under flow"));
    }
  }
}
