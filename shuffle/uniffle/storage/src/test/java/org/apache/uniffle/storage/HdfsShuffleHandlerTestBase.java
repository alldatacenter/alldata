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

package org.apache.uniffle.storage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Lists;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShufflePartitionedBlock;
import org.apache.uniffle.common.util.ChecksumUtils;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.storage.common.FileBasedShuffleSegment;
import org.apache.uniffle.storage.handler.impl.HdfsFileReader;
import org.apache.uniffle.storage.handler.impl.HdfsFileWriter;
import org.apache.uniffle.storage.handler.impl.HdfsShuffleWriteHandler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HdfsShuffleHandlerTestBase {

  private static final AtomicLong ATOMIC_LONG = new AtomicLong(0);

  public static void writeTestData(
      HdfsShuffleWriteHandler writeHandler,
      int num, int length, long taskAttemptId,
      Map<Long, byte[]> expectedData) throws Exception {
    List<ShufflePartitionedBlock> blocks = Lists.newArrayList();
    for (int i = 0; i < num; i++) {
      byte[] buf = new byte[length];
      new Random().nextBytes(buf);
      long blockId = (ATOMIC_LONG.getAndIncrement()
          << (Constants.PARTITION_ID_MAX_LENGTH + Constants.TASK_ATTEMPT_ID_MAX_LENGTH)) + taskAttemptId;
      blocks.add(new ShufflePartitionedBlock(
          length, length, ChecksumUtils.getCrc32(buf), blockId, taskAttemptId, buf));
      expectedData.put(blockId, buf);
    }
    writeHandler.write(blocks);
  }

  public static void writeTestData(
      HdfsFileWriter writer,
      int partitionId,
      int num, int length, long taskAttemptId,
      Map<Long, byte[]> expectedData,
      Map<Integer, List<ShufflePartitionedBlock>> expectedBlocks,
      Map<Integer, List<FileBasedShuffleSegment>> expectedIndexSegments,
      boolean doWrite) throws Exception {
    List<ShufflePartitionedBlock> blocks = Lists.newArrayList();
    List<FileBasedShuffleSegment> segments = Lists.newArrayList();
    for (int i = 0; i < num; i++) {
      byte[] buf = new byte[length];
      new Random().nextBytes(buf);
      long blockId = (ATOMIC_LONG.getAndIncrement()
          << (Constants.PARTITION_ID_MAX_LENGTH + Constants.TASK_ATTEMPT_ID_MAX_LENGTH)) + taskAttemptId;
      blocks.add(new ShufflePartitionedBlock(
          length, length, ChecksumUtils.getCrc32(buf), blockId, taskAttemptId, buf));
      expectedData.put(blockId, buf);
    }
    expectedBlocks.put(partitionId, blocks);
    long offset = 0;
    for (ShufflePartitionedBlock spb : blocks) {
      FileBasedShuffleSegment segment = new FileBasedShuffleSegment(
          spb.getBlockId(), offset, spb.getLength(), spb.getUncompressLength(), spb.getCrc(), 1);
      offset += spb.getLength();
      segments.add(segment);
      if (doWrite) {
        writer.writeData(spb.getData());
      }
    }
    expectedIndexSegments.put(partitionId, segments);
  }

  public static byte[] writeData(HdfsFileWriter writer, int len) throws IOException {
    byte[] data = new byte[len];
    new Random().nextBytes(data);
    writer.writeData(data);
    return data;
  }

  public static int calcExpectedSegmentNum(int num, int size, int bufferSize) {
    int segmentNum = 0;
    int cur = 0;
    for (int i = 0; i < num; ++i) {
      cur += size;
      if (cur >= bufferSize) {
        segmentNum++;
        cur = 0;
      }
    }

    if (cur > 0) {
      ++segmentNum;
    }

    return segmentNum;
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

  public static HdfsFileReader createHdfsReader(
      String folder, String fileName, Configuration hadoopConf) throws Exception {
    Path path = new Path(folder, fileName);
    HdfsFileReader reader = new HdfsFileReader(path, hadoopConf);
    return reader;
  }

}
