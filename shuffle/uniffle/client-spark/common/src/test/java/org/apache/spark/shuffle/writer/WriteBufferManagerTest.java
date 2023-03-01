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

package org.apache.spark.shuffle.writer;

import java.util.List;

import com.google.common.collect.Maps;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.executor.ShuffleWriteMetrics;
import org.apache.spark.memory.TaskMemoryManager;
import org.apache.spark.serializer.KryoSerializer;
import org.apache.spark.serializer.Serializer;
import org.apache.spark.shuffle.RssSparkConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.ShuffleBlockInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class WriteBufferManagerTest {

  private WriteBufferManager createManager(SparkConf conf) {
    Serializer kryoSerializer = new KryoSerializer(conf);
    TaskMemoryManager mockTaskMemoryManager = mock(TaskMemoryManager.class);

    BufferManagerOptions bufferOptions = new BufferManagerOptions(conf);
    WriteBufferManager wbm = new WriteBufferManager(
        0, 0, bufferOptions, kryoSerializer,
        Maps.newHashMap(), mockTaskMemoryManager, new ShuffleWriteMetrics(), RssSparkConfig.toRssConf(conf));
    WriteBufferManager spyManager = spy(wbm);
    doReturn(512L).when(spyManager).acquireMemory(anyLong());
    return spyManager;
  }

  private SparkConf getConf() {
    SparkConf conf = new SparkConf(false);
    conf.set(RssSparkConfig.RSS_WRITER_BUFFER_SIZE.key(), "64")
        .set(RssSparkConfig.RSS_WRITER_BUFFER_SEGMENT_SIZE.key(), "32")
        .set(RssSparkConfig.RSS_WRITER_SERIALIZER_BUFFER_SIZE.key(), "128")
        .set(RssSparkConfig.RSS_WRITER_PRE_ALLOCATED_BUFFER_SIZE.key(), "512")
        .set(RssSparkConfig.RSS_WRITER_BUFFER_SPILL_SIZE.key(), "190");
    return conf;
  }

  @Test
  public void addRecordCompressedTest() throws Exception {
    addRecord(true);
  }

  @Test
  public void addRecordUnCompressedTest() throws Exception {
    addRecord(false);
  }

  private void addRecord(boolean compress) throws IllegalAccessException {
    SparkConf conf = getConf();
    if (!compress) {
      conf.set(RssSparkConfig.SPARK_SHUFFLE_COMPRESS_KEY, String.valueOf(false));
    }
    WriteBufferManager wbm = createManager(conf);
    Object codec = FieldUtils.readField(wbm, "codec", true);
    if (compress) {
      Assertions.assertNotNull(codec);
    } else {
      Assertions.assertNull(codec);
    }
    wbm.setShuffleWriteMetrics(new ShuffleWriteMetrics());
    String testKey = "Key";
    String testValue = "Value";
    List<ShuffleBlockInfo> result = wbm.addRecord(0, testKey, testValue);
    // single buffer is not full, there is no data return
    assertEquals(0, result.size());
    assertEquals(512, wbm.getAllocatedBytes());
    assertEquals(32, wbm.getUsedBytes());
    assertEquals(0, wbm.getInSendListBytes());
    assertEquals(1, wbm.getBuffers().size());
    wbm.addRecord(0, testKey, testValue);
    wbm.addRecord(0, testKey, testValue);
    wbm.addRecord(0, testKey, testValue);
    result = wbm.addRecord(0, testKey, testValue);
    // single buffer is full
    assertEquals(1, result.size());
    assertEquals(512, wbm.getAllocatedBytes());
    assertEquals(96, wbm.getUsedBytes());
    assertEquals(96, wbm.getInSendListBytes());
    assertEquals(0, wbm.getBuffers().size());
    wbm.addRecord(0, testKey, testValue);
    wbm.addRecord(1, testKey, testValue);
    wbm.addRecord(2, testKey, testValue);
    // single buffer is not full, and less than spill size
    assertEquals(512, wbm.getAllocatedBytes());
    assertEquals(192, wbm.getUsedBytes());
    assertEquals(96, wbm.getInSendListBytes());
    assertEquals(3, wbm.getBuffers().size());
    // all buffer size > spill size
    wbm.addRecord(3, testKey, testValue);
    wbm.addRecord(4, testKey, testValue);
    result = wbm.addRecord(5, testKey, testValue);
    assertEquals(6, result.size());
    assertEquals(512, wbm.getAllocatedBytes());
    assertEquals(288, wbm.getUsedBytes());
    assertEquals(288, wbm.getInSendListBytes());
    assertEquals(0, wbm.getBuffers().size());
    // free memory
    wbm.freeAllocatedMemory(96);
    assertEquals(416, wbm.getAllocatedBytes());
    assertEquals(192, wbm.getUsedBytes());
    assertEquals(192, wbm.getInSendListBytes());

    assertEquals(11, wbm.getShuffleWriteMetrics().recordsWritten());
    assertTrue(wbm.getShuffleWriteMetrics().bytesWritten() > 0);

    wbm.freeAllocatedMemory(192);
    wbm.addRecord(0, testKey, testValue);
    wbm.addRecord(1, testKey, testValue);
    wbm.addRecord(2, testKey, testValue);
    result = wbm.clear();
    assertEquals(3, result.size());
    assertEquals(224, wbm.getAllocatedBytes());
    assertEquals(96, wbm.getUsedBytes());
    assertEquals(96, wbm.getInSendListBytes());
  }

  @Test
  public void addHugeRecordTest() {
    SparkConf conf = getConf();
    WriteBufferManager wbm = createManager(conf);
    String testKey = "len_more_than_32";
    String testValue = "len_more_than_32";
    List<ShuffleBlockInfo> result = wbm.addRecord(0, testKey, testValue);
    assertEquals(0, result.size());
    assertEquals(512, wbm.getAllocatedBytes());
    assertEquals(36, wbm.getUsedBytes());
    assertEquals(0, wbm.getInSendListBytes());
    assertEquals(1, wbm.getBuffers().size());
  }

  @Test
  public void addNullValueRecordTest() {
    SparkConf conf = getConf();
    WriteBufferManager wbm = createManager(conf);
    String testKey = "key";
    String testValue = null;
    List<ShuffleBlockInfo> result = wbm.addRecord(0, testKey, testValue);
    assertEquals(0, result.size());
    assertEquals(512, wbm.getAllocatedBytes());
    assertEquals(32, wbm.getUsedBytes());
    assertEquals(0, wbm.getInSendListBytes());
    assertEquals(1, wbm.getBuffers().size());
  }

  @Test
  public void createBlockIdTest() {
    SparkConf conf = getConf();
    WriteBufferManager wbm = createManager(conf);
    WriterBuffer mockWriterBuffer = mock(WriterBuffer.class);
    when(mockWriterBuffer.getData()).thenReturn(new byte[]{});
    when(mockWriterBuffer.getMemoryUsed()).thenReturn(0);
    ShuffleBlockInfo sbi = wbm.createShuffleBlock(0, mockWriterBuffer);
    // seqNo = 0, partitionId = 0, taskId = 0
    assertEquals(0L, sbi.getBlockId());

    // seqNo = 1, partitionId = 0, taskId = 0
    sbi = wbm.createShuffleBlock(0, mockWriterBuffer);
    assertEquals(35184372088832L, sbi.getBlockId());

    // seqNo = 0, partitionId = 1, taskId = 0
    sbi = wbm.createShuffleBlock(1, mockWriterBuffer);
    assertEquals(2097152L, sbi.getBlockId());

    // seqNo = 1, partitionId = 1, taskId = 0
    sbi = wbm.createShuffleBlock(1, mockWriterBuffer);
    assertEquals(35184374185984L, sbi.getBlockId());
  }
}
