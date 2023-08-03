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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShufflePartitionedBlock;
import org.apache.uniffle.common.util.ChecksumUtils;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.storage.HdfsShuffleHandlerTestBase;
import org.apache.uniffle.storage.HdfsTestBase;
import org.apache.uniffle.storage.common.FileBasedShuffleSegment;
import org.apache.uniffle.storage.util.ShuffleStorageUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

public class HdfsShuffleReadHandlerTest extends HdfsTestBase {

  public static void createAndRunCases(String clusterPathPrefix, Configuration conf, String user) {
    try {
      String basePath = clusterPathPrefix + "HdfsShuffleFileReadHandlerTest";
      HdfsShuffleWriteHandler writeHandler =
          new HdfsShuffleWriteHandler(
              "appId",
              0,
              1,
              1,
              basePath,
              "test",
              conf,
              user);

      Map<Long, byte[]> expectedData = Maps.newHashMap();

      int readBufferSize = 13;
      int totalBlockNum = 0;
      int expectTotalBlockNum = new Random().nextInt(37);
      int blockSize = new Random().nextInt(7) + 1;
      HdfsShuffleHandlerTestBase.writeTestData(writeHandler, expectTotalBlockNum, blockSize, 0, expectedData);
      int total = HdfsShuffleHandlerTestBase.calcExpectedSegmentNum(expectTotalBlockNum, blockSize, readBufferSize);
      Roaring64NavigableMap expectBlockIds = Roaring64NavigableMap.bitmapOf();
      Roaring64NavigableMap processBlockIds =  Roaring64NavigableMap.bitmapOf();
      expectedData.forEach((id, block) -> expectBlockIds.addLong(id));
      String fileNamePrefix = ShuffleStorageUtils.getFullShuffleDataFolder(basePath,
          ShuffleStorageUtils.getShuffleDataPathWithRange("appId",
              0, 1, 1, 10)) + "/test_0";
      HdfsShuffleReadHandler handler =
          new HdfsShuffleReadHandler("appId", 0, 1, fileNamePrefix,
              readBufferSize, expectBlockIds, processBlockIds, conf);

      Set<Long> actualBlockIds = Sets.newHashSet();
      for (int i = 0; i < total; ++i) {
        ShuffleDataResult shuffleDataResult = handler.readShuffleData();
        totalBlockNum += shuffleDataResult.getBufferSegments().size();
        HdfsShuffleHandlerTestBase.checkData(shuffleDataResult, expectedData);
        for (BufferSegment bufferSegment : shuffleDataResult.getBufferSegments()) {
          actualBlockIds.add(bufferSegment.getBlockId());
        }
      }

      assertNull(handler.readShuffleData());
      assertEquals(
          total,
          handler.getShuffleDataSegments().size());
      assertEquals(expectTotalBlockNum, totalBlockNum);
      assertEquals(expectedData.keySet(), actualBlockIds);

    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void test() {
    createAndRunCases(HDFS_URI, conf, StringUtils.EMPTY);
  }

  @Test
  public void testDataInconsistent() {

    try {
      String basePath = HDFS_URI + "HdfsShuffleFileReadHandlerTest#testDataInconsistent";
      TestHdfsShuffleWriteHandler writeHandler =
          new TestHdfsShuffleWriteHandler(
              "appId",
              0,
              1,
              1,
              basePath,
              "test",
              conf,
              StringUtils.EMPTY);

      Map<Long, byte[]> expectedData = Maps.newHashMap();
      int totalBlockNum = 0;
      int expectTotalBlockNum = 6;
      int blockSize = 7;
      int taskAttemptId = 0;

      // write expectTotalBlockNum - 1 complete block
      HdfsShuffleHandlerTestBase.writeTestData(writeHandler, expectTotalBlockNum - 1,
          blockSize, taskAttemptId, expectedData);

      // write 1 incomplete block , which only write index file
      List<ShufflePartitionedBlock> blocks = Lists.newArrayList();
      byte[] buf = new byte[blockSize];
      new Random().nextBytes(buf);
      long blockId = (expectTotalBlockNum
                          << (Constants.PARTITION_ID_MAX_LENGTH + Constants.TASK_ATTEMPT_ID_MAX_LENGTH))
                              + taskAttemptId;
      blocks.add(new ShufflePartitionedBlock(blockSize, blockSize, ChecksumUtils.getCrc32(buf), blockId,
          taskAttemptId, buf));
      writeHandler.writeIndex(blocks);

      int readBufferSize = 13;
      int total = HdfsShuffleHandlerTestBase.calcExpectedSegmentNum(expectTotalBlockNum, blockSize, readBufferSize);
      Roaring64NavigableMap expectBlockIds = Roaring64NavigableMap.bitmapOf();
      Roaring64NavigableMap processBlockIds =  Roaring64NavigableMap.bitmapOf();
      expectedData.forEach((id, block) -> expectBlockIds.addLong(id));
      String fileNamePrefix = ShuffleStorageUtils.getFullShuffleDataFolder(basePath,
          ShuffleStorageUtils.getShuffleDataPathWithRange("appId",
              0, 1, 1, 10)) + "/test_0";
      HdfsShuffleReadHandler handler =
          new HdfsShuffleReadHandler("appId", 0, 1, fileNamePrefix,
              readBufferSize, expectBlockIds, processBlockIds, conf);

      Set<Long> actualBlockIds = Sets.newHashSet();
      for (int i = 0; i < total; ++i) {
        ShuffleDataResult shuffleDataResult = handler.readShuffleData();
        totalBlockNum += shuffleDataResult.getBufferSegments().size();
        HdfsShuffleHandlerTestBase.checkData(shuffleDataResult, expectedData);
        for (BufferSegment bufferSegment : shuffleDataResult.getBufferSegments()) {
          actualBlockIds.add(bufferSegment.getBlockId());
        }
      }

      assertNull(handler.readShuffleData());
      assertEquals(
          total,
          handler.getShuffleDataSegments().size());
      // The last block cannot be read, only the index is generated
      assertEquals(expectTotalBlockNum - 1, totalBlockNum);
      assertEquals(expectedData.keySet(), actualBlockIds);

    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  static class TestHdfsShuffleWriteHandler extends HdfsShuffleWriteHandler {

    private Configuration hadoopConf;
    private Lock writeLock = new ReentrantLock();
    private String basePath;
    private String fileNamePrefix;
    private int failTimes = 0;

    TestHdfsShuffleWriteHandler(
        String appId,
        int shuffleId,
        int startPartition,
        int endPartition,
        String storageBasePath,
        String fileNamePrefix,
        Configuration hadoopConf,
        String user) throws Exception {
      super(appId, shuffleId, startPartition, endPartition, storageBasePath, fileNamePrefix, hadoopConf, user);
      this.hadoopConf = hadoopConf;
      this.fileNamePrefix = fileNamePrefix;
      this.basePath = ShuffleStorageUtils.getFullShuffleDataFolder(storageBasePath,
          ShuffleStorageUtils.getShuffleDataPath(appId, shuffleId, startPartition, endPartition));
    }


    // only write index file
    public void writeIndex(
        List<ShufflePartitionedBlock> shuffleBlocks) throws IOException, IllegalStateException {
      HdfsFileWriter indexWriter = null;
      writeLock.lock();
      try {
        try {
          String indexFileName = ShuffleStorageUtils.generateIndexFileName(fileNamePrefix + "_" + failTimes);
          indexWriter = createWriter(indexFileName);
          for (ShufflePartitionedBlock block : shuffleBlocks) {
            long blockId = block.getBlockId();
            long crc = block.getCrc();
            long startOffset = indexWriter.nextOffset();

            FileBasedShuffleSegment segment = new FileBasedShuffleSegment(
                blockId, startOffset, block.getLength(), block.getUncompressLength(), crc, block.getTaskAttemptId());
            indexWriter.writeIndex(segment);
          }
        } catch (Exception e) {
          failTimes++;
          throw new RuntimeException(e);
        } finally {
          if (indexWriter != null) {
            indexWriter.close();
          }
        }
      } finally {
        writeLock.unlock();
      }
    }
  }
}
