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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import com.clearspring.analytics.util.Lists;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.apache.spark.executor.ShuffleWriteMetrics;
import org.apache.spark.memory.MemoryConsumer;
import org.apache.spark.memory.MemoryMode;
import org.apache.spark.memory.TaskMemoryManager;
import org.apache.spark.serializer.SerializationStream;
import org.apache.spark.serializer.Serializer;
import org.apache.spark.serializer.SerializerInstance;
import org.apache.spark.shuffle.RssSparkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.reflect.ClassTag$;
import scala.reflect.ManifestFactory$;

import org.apache.uniffle.client.util.ClientUtils;
import org.apache.uniffle.common.ShuffleBlockInfo;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.compression.Codec;
import org.apache.uniffle.common.config.RssConf;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.common.util.ChecksumUtils;

public class WriteBufferManager extends MemoryConsumer {

  private static final Logger LOG = LoggerFactory.getLogger(WriteBufferManager.class);
  private int bufferSize;
  private long spillSize;
  // allocated bytes from executor memory
  private AtomicLong allocatedBytes = new AtomicLong(0);
  // bytes of shuffle data in memory
  private AtomicLong usedBytes = new AtomicLong(0);
  // bytes of shuffle data which is in send list
  private AtomicLong inSendListBytes = new AtomicLong(0);
  // it's part of blockId
  private Map<Integer, Integer> partitionToSeqNo = Maps.newHashMap();
  private long askExecutorMemory;
  private int shuffleId;
  private long taskAttemptId;
  private SerializerInstance instance;
  private ShuffleWriteMetrics shuffleWriteMetrics;
  // cache partition -> records
  private Map<Integer, WriterBuffer> buffers;
  private Map<Integer, List<ShuffleServerInfo>> partitionToServers;
  private int serializerBufferSize;
  private int bufferSegmentSize;
  private long copyTime = 0;
  private long serializeTime = 0;
  private long compressTime = 0;
  private long writeTime = 0;
  private long estimateTime = 0;
  private long requireMemoryTime = 0;
  private SerializationStream serializeStream;
  private WrappedByteArrayOutputStream arrayOutputStream;
  private long uncompressedDataLen = 0;
  private long requireMemoryInterval;
  private int requireMemoryRetryMax;
  private Codec codec;

  public WriteBufferManager(
      int shuffleId,
      long taskAttemptId,
      BufferManagerOptions bufferManagerOptions,
      Serializer serializer,
      Map<Integer, List<ShuffleServerInfo>> partitionToServers,
      TaskMemoryManager taskMemoryManager,
      ShuffleWriteMetrics shuffleWriteMetrics,
      RssConf rssConf) {
    super(taskMemoryManager, taskMemoryManager.pageSizeBytes(), MemoryMode.ON_HEAP);
    this.bufferSize = bufferManagerOptions.getBufferSize();
    this.spillSize = bufferManagerOptions.getBufferSpillThreshold();
    this.instance = serializer.newInstance();
    this.buffers = Maps.newHashMap();
    this.shuffleId = shuffleId;
    this.taskAttemptId = taskAttemptId;
    this.partitionToServers = partitionToServers;
    this.shuffleWriteMetrics = shuffleWriteMetrics;
    this.serializerBufferSize = bufferManagerOptions.getSerializerBufferSize();
    this.bufferSegmentSize = bufferManagerOptions.getBufferSegmentSize();
    this.askExecutorMemory = bufferManagerOptions.getPreAllocatedBufferSize();
    this.requireMemoryInterval = bufferManagerOptions.getRequireMemoryInterval();
    this.requireMemoryRetryMax = bufferManagerOptions.getRequireMemoryRetryMax();
    this.arrayOutputStream = new WrappedByteArrayOutputStream(serializerBufferSize);
    this.serializeStream = instance.serializeStream(arrayOutputStream);
    boolean compress = rssConf.getBoolean(RssSparkConfig.SPARK_SHUFFLE_COMPRESS_KEY
            .substring(RssSparkConfig.SPARK_RSS_CONFIG_PREFIX.length()),
        RssSparkConfig.SPARK_SHUFFLE_COMPRESS_DEFAULT);
    this.codec = compress ? Codec.newInstance(rssConf) : null;
  }

  public List<ShuffleBlockInfo> addRecord(int partitionId, Object key, Object value) {
    final long start = System.currentTimeMillis();
    arrayOutputStream.reset();
    if (key != null) {
      serializeStream.writeKey(key, ClassTag$.MODULE$.apply(key.getClass()));
    } else {
      serializeStream.writeKey(null, ManifestFactory$.MODULE$.Null());
    }
    if (value != null) {
      serializeStream.writeValue(value, ClassTag$.MODULE$.apply(value.getClass()));
    } else {
      serializeStream.writeValue(null, ManifestFactory$.MODULE$.Null());
    }
    serializeStream.flush();
    serializeTime += System.currentTimeMillis() - start;
    byte[] serializedData = arrayOutputStream.getBuf();
    int serializedDataLength = arrayOutputStream.size();
    if (serializedDataLength == 0) {
      return null;
    }
    List<ShuffleBlockInfo> result = Lists.newArrayList();
    if (buffers.containsKey(partitionId)) {
      WriterBuffer wb = buffers.get(partitionId);
      if (wb.askForMemory(serializedDataLength)) {
        requestMemory(Math.max(bufferSegmentSize, serializedDataLength));
      }
      wb.addRecord(serializedData, serializedDataLength);
      if (wb.getMemoryUsed() > bufferSize) {
        result.add(createShuffleBlock(partitionId, wb));
        copyTime += wb.getCopyTime();
        buffers.remove(partitionId);
        LOG.debug("Single buffer is full for shuffleId[" + shuffleId
            + "] partition[" + partitionId + "] with memoryUsed[" + wb.getMemoryUsed()
            + "], dataLength[" + wb.getDataLength() + "]");
      }
    } else {
      requestMemory(Math.max(bufferSegmentSize, serializedDataLength));
      WriterBuffer wb = new WriterBuffer(bufferSegmentSize);
      wb.addRecord(serializedData, serializedDataLength);
      buffers.put(partitionId, wb);
    }
    shuffleWriteMetrics.incRecordsWritten(1L);

    // check buffer size > spill threshold
    if (usedBytes.get() - inSendListBytes.get() > spillSize) {
      result.addAll(clear());
    }
    writeTime += System.currentTimeMillis() - start;
    return result;
  }

  // transform all [partition, records] to [partition, ShuffleBlockInfo] and clear cache
  public List<ShuffleBlockInfo> clear() {
    List<ShuffleBlockInfo> result = Lists.newArrayList();
    long dataSize = 0;
    long memoryUsed = 0;
    for (Entry<Integer, WriterBuffer> entry : buffers.entrySet()) {
      WriterBuffer wb = entry.getValue();
      dataSize += wb.getDataLength();
      memoryUsed += wb.getMemoryUsed();
      result.add(createShuffleBlock(entry.getKey(), wb));
      copyTime += wb.getCopyTime();
    }
    LOG.info("Flush total buffer for shuffleId[" + shuffleId + "] with allocated["
        + allocatedBytes + "], dataSize[" + dataSize + "], memoryUsed[" + memoryUsed + "]");
    buffers.clear();
    return result;
  }

  // transform records to shuffleBlock
  protected ShuffleBlockInfo createShuffleBlock(int partitionId, WriterBuffer wb) {
    byte[] data = wb.getData();
    final int uncompressLength = data.length;
    byte[] compressed = data;
    if (codec != null) {
      long start = System.currentTimeMillis();
      compressed = codec.compress(data);
      compressTime += System.currentTimeMillis() - start;
    }
    final long crc32 = ChecksumUtils.getCrc32(compressed);
    final long blockId = ClientUtils.getBlockId(partitionId, taskAttemptId, getNextSeqNo(partitionId));
    uncompressedDataLen += data.length;
    shuffleWriteMetrics.incBytesWritten(compressed.length);
    // add memory to indicate bytes which will be sent to shuffle server
    inSendListBytes.addAndGet(wb.getMemoryUsed());
    return new ShuffleBlockInfo(shuffleId, partitionId, blockId, compressed.length, crc32,
        compressed, partitionToServers.get(partitionId), uncompressLength, wb.getMemoryUsed(), taskAttemptId);
  }

  // it's run in single thread, and is not thread safe
  private int getNextSeqNo(int partitionId) {
    partitionToSeqNo.putIfAbsent(partitionId, 0);
    int seqNo = partitionToSeqNo.get(partitionId);
    partitionToSeqNo.put(partitionId, seqNo + 1);
    return seqNo;
  }

  private void requestMemory(long requiredMem) {
    final long start = System.currentTimeMillis();
    if (allocatedBytes.get() - usedBytes.get() < requiredMem) {
      requestExecutorMemory(requiredMem);
    }
    usedBytes.addAndGet(requiredMem);
    requireMemoryTime += System.currentTimeMillis() - start;
  }

  private void requestExecutorMemory(long leastMem) {
    long gotMem = acquireMemory(askExecutorMemory);
    allocatedBytes.addAndGet(gotMem);
    int retry = 0;
    while (allocatedBytes.get() - usedBytes.get() < leastMem) {
      LOG.info("Can't get memory for now, sleep and try[" + retry
          + "] again, request[" + askExecutorMemory + "], got[" + gotMem + "] less than "
          + leastMem);
      try {
        Thread.sleep(requireMemoryInterval);
      } catch (InterruptedException ie) {
        LOG.warn("Exception happened when waiting for memory.", ie);
      }
      gotMem = acquireMemory(askExecutorMemory);
      allocatedBytes.addAndGet(gotMem);
      retry++;
      if (retry > requireMemoryRetryMax) {
        String message = "Can't get memory to cache shuffle data, request[" + askExecutorMemory
            + "], got[" + gotMem + "]," + " WriteBufferManager allocated[" + allocatedBytes
            + "] task used[" + used + "]. It may be caused by shuffle server is full of data"
            + " or consider to optimize 'spark.executor.memory',"
            + " 'spark.rss.writer.buffer.spill.size'.";
        LOG.error(message);
        throw new RssException(message);
      }
    }
  }

  @Override
  public long spill(long size, MemoryConsumer trigger) {
    // there is no spill for such situation
    return 0;
  }

  @VisibleForTesting
  protected long getAllocatedBytes() {
    return allocatedBytes.get();
  }

  @VisibleForTesting
  protected long getUsedBytes() {
    return usedBytes.get();
  }

  @VisibleForTesting
  protected long getInSendListBytes() {
    return inSendListBytes.get();
  }

  public void freeAllocatedMemory(long freeMemory) {
    freeMemory(freeMemory);
    allocatedBytes.addAndGet(-freeMemory);
    usedBytes.addAndGet(-freeMemory);
    inSendListBytes.addAndGet(-freeMemory);
  }

  public void freeAllMemory() {
    long memory = allocatedBytes.get();
    if (memory > 0) {
      freeMemory(memory);
    }
  }

  @VisibleForTesting
  protected Map<Integer, WriterBuffer> getBuffers() {
    return buffers;
  }

  @VisibleForTesting
  protected ShuffleWriteMetrics getShuffleWriteMetrics() {
    return shuffleWriteMetrics;
  }

  @VisibleForTesting
  protected void setShuffleWriteMetrics(ShuffleWriteMetrics shuffleWriteMetrics) {
    this.shuffleWriteMetrics = shuffleWriteMetrics;
  }

  public long getWriteTime() {
    return writeTime;
  }

  public String getManagerCostInfo() {
    return "WriteBufferManager cost copyTime[" + copyTime + "], writeTime[" + writeTime + "], serializeTime["
        + serializeTime + "], compressTime[" + compressTime + "], estimateTime["
        + estimateTime + "], requireMemoryTime[" + requireMemoryTime
        + "], uncompressedDataLen[" + uncompressedDataLen + "]";
  }
}
