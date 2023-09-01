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

package org.apache.uniffle.storage.handler.impl;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShuffleDataSegment;
import org.apache.uniffle.common.ShuffleIndexResult;
import org.apache.uniffle.common.ShufflePartitionedBlock;
import org.apache.uniffle.common.segment.FixedSizeSegmentSplitter;
import org.apache.uniffle.common.util.ChecksumUtils;
import org.apache.uniffle.storage.common.FileBasedShuffleSegment;
import org.apache.uniffle.storage.handler.api.ServerReadHandler;
import org.apache.uniffle.storage.handler.api.ShuffleWriteHandler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocalFileHandlerTestBase {
  private static AtomicLong ATOMIC_LONG = new AtomicLong(0L);

  public static List<ShufflePartitionedBlock> generateBlocks(int num, int length) {
    List<ShufflePartitionedBlock> blocks = Lists.newArrayList();
    for (int i = 0; i < num; i++) {
      byte[] buf = new byte[length];
      new Random().nextBytes(buf);
      long blockId = ATOMIC_LONG.incrementAndGet();
      blocks.add(new ShufflePartitionedBlock(length, length, ChecksumUtils.getCrc32(buf), blockId, 100,
          buf));
    }
    return blocks;
  }

  public static void writeTestData(List<ShufflePartitionedBlock> blocks, ShuffleWriteHandler handler,
      Map<Long, byte[]> expectedData, Set<Long> expectedBlockIds) throws Exception {
    handler.write(blocks);
    blocks.forEach(block -> expectedBlockIds.add(block.getBlockId()));
    blocks.forEach(block -> expectedData.put(block.getBlockId(), block.getData()));
  }

  public static void validateResult(ServerReadHandler readHandler, Set<Long> expectedBlockIds,
      Map<Long, byte[]> expectedData) {
    List<ShuffleDataResult> shuffleDataResults = readAll(readHandler);
    Set<Long> actualBlockIds = Sets.newHashSet();
    for (ShuffleDataResult sdr : shuffleDataResults) {
      byte[] buffer = sdr.getData();
      List<BufferSegment> bufferSegments = sdr.getBufferSegments();

      for (BufferSegment bs : bufferSegments) {
        byte[] data = new byte[bs.getLength()];
        System.arraycopy(buffer, bs.getOffset(), data, 0, bs.getLength());
        assertEquals(bs.getCrc(), ChecksumUtils.getCrc32(data));
        assertArrayEquals(expectedData.get(bs.getBlockId()), data);
        actualBlockIds.add(bs.getBlockId());
      }
    }
    assertEquals(expectedBlockIds, actualBlockIds);
  }

  public static List<ShuffleDataResult> readAll(ServerReadHandler readHandler) {
    ShuffleIndexResult shuffleIndexResult = readIndex(readHandler);
    return readData(readHandler, shuffleIndexResult);
  }

  public static ShuffleIndexResult readIndex(ServerReadHandler readHandler) {
    ShuffleIndexResult shuffleIndexResult = readHandler.getShuffleIndex();
    return shuffleIndexResult;
  }

  public static List<ShuffleDataResult> readData(ServerReadHandler readHandler, ShuffleIndexResult shuffleIndexResult) {
    List<ShuffleDataResult> shuffleDataResults = Lists.newLinkedList();
    if (shuffleIndexResult == null || shuffleIndexResult.isEmpty()) {
      return shuffleDataResults;
    }

    List<ShuffleDataSegment> shuffleDataSegments = new FixedSizeSegmentSplitter(32).split(shuffleIndexResult);

    for (ShuffleDataSegment shuffleDataSegment : shuffleDataSegments) {
      byte[] shuffleData =
          readHandler.getShuffleData(shuffleDataSegment.getOffset(), shuffleDataSegment.getLength()).getData();
      ShuffleDataResult sdr = new ShuffleDataResult(shuffleData, shuffleDataSegment.getBufferSegments());
      shuffleDataResults.add(sdr);
    }

    return shuffleDataResults;
  }

  public static void checkData(ShuffleDataResult shuffleDataResult, Map<Long, byte[]> expectedData) {
    byte[] buffer = shuffleDataResult.getData();
    List<BufferSegment> bufferSegments = shuffleDataResult.getBufferSegments();

    for (BufferSegment bs : bufferSegments) {
      byte[] data = new byte[bs.getLength()];
      System.arraycopy(buffer, bs.getOffset(), data, 0, bs.getLength());
      assertEquals(bs.getCrc(), ChecksumUtils.getCrc32(data));
      assertArrayEquals(expectedData.get(bs.getBlockId()), data);
    }
  }

  public static void writeIndex(ByteBuffer byteBuffer, FileBasedShuffleSegment segment) {
    byteBuffer.putLong(segment.getOffset());
    byteBuffer.putInt(segment.getLength());
    byteBuffer.putInt(segment.getUncompressLength());
    byteBuffer.putLong(segment.getCrc());
    byteBuffer.putLong(segment.getBlockId());
    byteBuffer.putLong(segment.getTaskAttemptId());
  }

  public static List<byte[]> calcSegmentBytes(Map<Long, byte[]> blockIdToData,
      int bytesPerSegment, List<Long> blockIds) {
    List<byte[]> res = Lists.newArrayList();
    int curSize = 0;
    ByteBuffer byteBuffer = ByteBuffer.allocate(2 * bytesPerSegment);
    for (long i : blockIds) {
      byte[] data = blockIdToData.get(i);
      byteBuffer.put(data, 0, data.length);
      curSize += data.length;
      if (curSize >= bytesPerSegment) {
        byte[] newByte = new byte[curSize];
        System.arraycopy(byteBuffer.array(), 0, newByte, 0, curSize);
        res.add(newByte);
        byteBuffer.clear();
        curSize = 0;
      }
    }
    if (curSize > 0) {
      byte[] newByte = new byte[curSize];
      System.arraycopy(byteBuffer.array(), 0, newByte, 0, curSize);
      res.add(newByte);
    }
    return res;
  }
}
