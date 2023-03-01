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

package org.apache.spark.shuffle.celeborn;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.LongAdder;

import javax.annotation.Nullable;

import scala.Option;
import scala.Product2;
import scala.reflect.ClassTag;
import scala.reflect.ClassTag$;

import com.google.common.annotations.VisibleForTesting;
import org.apache.spark.Partitioner;
import org.apache.spark.ShuffleDependency;
import org.apache.spark.SparkEnv;
import org.apache.spark.TaskContext;
import org.apache.spark.annotation.Private;
import org.apache.spark.scheduler.MapStatus;
import org.apache.spark.serializer.SerializationStream;
import org.apache.spark.serializer.SerializerInstance;
import org.apache.spark.shuffle.ShuffleWriteMetricsReporter;
import org.apache.spark.shuffle.ShuffleWriter;
import org.apache.spark.sql.catalyst.expressions.UnsafeRow;
import org.apache.spark.sql.execution.PartitionIdPassthrough;
import org.apache.spark.sql.execution.UnsafeRowSerializer;
import org.apache.spark.sql.execution.metric.SQLMetric;
import org.apache.spark.storage.BlockManagerId;
import org.apache.spark.unsafe.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.client.ShuffleClient;
import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.exception.CelebornIOException;

@Private
public class SortBasedShuffleWriter<K, V, C> extends ShuffleWriter<K, V> {

  private static final Logger logger = LoggerFactory.getLogger(SortBasedShuffleWriter.class);

  private static final ClassTag<Object> OBJECT_CLASS_TAG = ClassTag$.MODULE$.Object();
  private static final int DEFAULT_INITIAL_SER_BUFFER_SIZE = 1024 * 1024;

  private final ShuffleDependency<K, V, C> dep;
  private final Partitioner partitioner;
  private final ShuffleWriteMetricsReporter writeMetrics;
  private final String appId;
  private final int shuffleId;
  private final int mapId;
  private final TaskContext taskContext;
  private final ShuffleClient rssShuffleClient;
  private final int numMappers;
  private final int numPartitions;

  private final long pushBufferMaxSize;
  // this lock is shared between different SortBasedPushers to synchronize pushData
  private final Object sharedPushLock = new Object();
  private final boolean pipelined;
  private SortBasedPusher[] pushers = new SortBasedPusher[2];
  private SortBasedPusher currentPusher;

  @Nullable private long peakMemoryUsedBytes = 0;

  private final OpenByteArrayOutputStream serBuffer;
  private final SerializationStream serOutputStream;

  private final LongAdder[] mapStatusLengths;
  private final long[] mapStatusRecords;
  private final long[] tmpRecords;

  /**
   * Are we in the process of stopping? Because map tasks can call stop() with success = true and
   * then call stop() with success = false if they get an exception, we want to make sure we don't
   * try deleting files, etc twice.
   */
  private volatile boolean stopping = false;

  // In order to facilitate the writing of unit test code, ShuffleClient needs to be passed in as
  // parameters. By the way, simplify the passed parameters.
  public SortBasedShuffleWriter(
      ShuffleDependency<K, V, C> dep,
      String appId,
      int numMappers,
      TaskContext taskContext,
      CelebornConf conf,
      ShuffleClient client,
      ShuffleWriteMetricsReporter metrics,
      ExecutorService executorService)
      throws IOException {
    this.mapId = taskContext.partitionId();
    this.dep = dep;
    this.appId = appId;
    this.shuffleId = dep.shuffleId();
    SerializerInstance serializer = dep.serializer().newInstance();
    this.partitioner = dep.partitioner();
    this.writeMetrics = metrics;
    this.taskContext = taskContext;
    this.numMappers = numMappers;
    this.numPartitions = dep.partitioner().numPartitions();
    this.rssShuffleClient = client;

    serBuffer = new OpenByteArrayOutputStream(DEFAULT_INITIAL_SER_BUFFER_SIZE);
    serOutputStream = serializer.serializeStream(serBuffer);

    this.mapStatusLengths = new LongAdder[numPartitions];
    this.mapStatusRecords = new long[numPartitions];
    for (int i = 0; i < numPartitions; i++) {
      this.mapStatusLengths[i] = new LongAdder();
    }
    tmpRecords = new long[numPartitions];

    pushBufferMaxSize = conf.pushBufferMaxSize();
    pipelined = conf.pushSortPipelineEnabled();

    if (pipelined) {
      for (int i = 0; i < pushers.length; i++) {
        pushers[i] =
            new SortBasedPusher(
                taskContext.taskMemoryManager(),
                rssShuffleClient,
                appId,
                shuffleId,
                mapId,
                taskContext.attemptNumber(),
                taskContext.taskAttemptId(),
                numMappers,
                numPartitions,
                conf,
                writeMetrics::incBytesWritten,
                mapStatusLengths,
                conf.pushSortMemoryThreshold() / 2,
                sharedPushLock,
                executorService);
      }
      currentPusher = pushers[0];
    } else {
      currentPusher =
          new SortBasedPusher(
              taskContext.taskMemoryManager(),
              rssShuffleClient,
              appId,
              shuffleId,
              mapId,
              taskContext.attemptNumber(),
              taskContext.taskAttemptId(),
              numMappers,
              numPartitions,
              conf,
              writeMetrics::incBytesWritten,
              mapStatusLengths,
              conf.pushSortMemoryThreshold(),
              sharedPushLock,
              null);
    }
  }

  @Override
  public void write(scala.collection.Iterator<Product2<K, V>> records) throws IOException {
    if (canUseFastWrite()) {
      fastWrite0(records);
    } else if (dep.mapSideCombine()) {
      if (dep.aggregator().isEmpty()) {
        throw new UnsupportedOperationException(
            "When using map side combine, an aggregator must be specified.");
      }
      write0(dep.aggregator().get().combineValuesByKey(records, taskContext));
    } else {
      write0(records);
    }
    close();
  }

  @VisibleForTesting
  boolean canUseFastWrite() {
    return dep.serializer() instanceof UnsafeRowSerializer
        && partitioner instanceof PartitionIdPassthrough;
  }

  private void fastWrite0(scala.collection.Iterator iterator) throws IOException {
    final scala.collection.Iterator<Product2<Integer, UnsafeRow>> records = iterator;

    SQLMetric dataSize = SparkUtils.getDataSize((UnsafeRowSerializer) dep.serializer());
    while (records.hasNext()) {
      final Product2<Integer, UnsafeRow> record = records.next();
      final int partitionId = record._1();
      final UnsafeRow row = record._2();

      final int rowSize = row.getSizeInBytes();
      final int serializedRecordSize = 4 + rowSize;

      if (dataSize != null) {
        dataSize.add(serializedRecordSize);
      }

      if (serializedRecordSize > pushBufferMaxSize) {
        byte[] giantBuffer = new byte[serializedRecordSize];
        Platform.putInt(giantBuffer, Platform.BYTE_ARRAY_OFFSET, Integer.reverseBytes(rowSize));
        Platform.copyMemory(
            row.getBaseObject(),
            row.getBaseOffset(),
            giantBuffer,
            Platform.BYTE_ARRAY_OFFSET + 4,
            rowSize);
        pushGiantRecord(partitionId, giantBuffer, serializedRecordSize);
      } else {
        long insertStartTime = System.nanoTime();
        boolean success =
            currentPusher.insertRecord(
                row.getBaseObject(), row.getBaseOffset(), rowSize, partitionId, true);
        if (!success) {
          pushAndSwitch();
          success =
              currentPusher.insertRecord(
                  row.getBaseObject(), row.getBaseOffset(), rowSize, partitionId, true);
          if (!success) {
            throw new CelebornIOException("Unable to push after switching pusher!");
          }
        }
        writeMetrics.incWriteTime(System.nanoTime() - insertStartTime);
      }
      tmpRecords[partitionId] += 1;
    }
  }

  private void pushAndSwitch() throws IOException {
    if (pipelined) {
      currentPusher.triggerPush();
      currentPusher = (currentPusher == pushers[0] ? pushers[1] : pushers[0]);
      currentPusher.waitPushFinish();
    } else {
      currentPusher.pushData();
    }
  }

  private void write0(scala.collection.Iterator iterator) throws IOException {
    final scala.collection.Iterator<Product2<K, ?>> records = iterator;

    while (records.hasNext()) {
      final Product2<K, ?> record = records.next();
      final K key = record._1();
      final int partitionId = partitioner.getPartition(key);
      serBuffer.reset();
      serOutputStream.writeKey(key, OBJECT_CLASS_TAG);
      serOutputStream.writeValue(record._2(), OBJECT_CLASS_TAG);
      serOutputStream.flush();

      final int serializedRecordSize = serBuffer.size();
      assert (serializedRecordSize > 0);

      if (serializedRecordSize > pushBufferMaxSize) {
        pushGiantRecord(partitionId, serBuffer.getBuf(), serializedRecordSize);
      } else {
        long insertStartTime = System.nanoTime();
        boolean success =
            currentPusher.insertRecord(
                serBuffer.getBuf(),
                Platform.BYTE_ARRAY_OFFSET,
                serializedRecordSize,
                partitionId,
                false);
        if (!success) {
          pushAndSwitch();
          success =
              currentPusher.insertRecord(
                  serBuffer.getBuf(),
                  Platform.BYTE_ARRAY_OFFSET,
                  serializedRecordSize,
                  partitionId,
                  false);
          if (!success) {
            throw new IOException("Unable to push after switching pusher!");
          }
        }
        writeMetrics.incWriteTime(System.nanoTime() - insertStartTime);
      }
      tmpRecords[partitionId] += 1;
    }
  }

  private void pushGiantRecord(int partitionId, byte[] buffer, int numBytes) throws IOException {
    logger.debug("Push giant record, size {}.", numBytes);
    long pushStartTime = System.nanoTime();
    int bytesWritten =
        rssShuffleClient.pushData(
            appId,
            shuffleId,
            mapId,
            taskContext.attemptNumber(),
            partitionId,
            buffer,
            0,
            numBytes,
            numMappers,
            numPartitions);
    mapStatusLengths[partitionId].add(bytesWritten);
    writeMetrics.incBytesWritten(bytesWritten);
    writeMetrics.incWriteTime(System.nanoTime() - pushStartTime);
  }

  private void close() throws IOException {
    if (pipelined) {
      logger.info("Memory used {}", (pushers[0].getUsed() + pushers[1].getUsed()));
    } else {
      logger.info("Memory used {}", currentPusher.getUsed());
    }
    long pushStartTime = System.nanoTime();
    if (pipelined) {
      for (int i = 0; i < pushers.length; i++) {
        pushers[i].waitPushFinish();
        pushers[i].pushData();
        pushers[i].close();
      }
    } else {
      currentPusher.pushData();
      currentPusher.close();
    }
    writeMetrics.incWriteTime(System.nanoTime() - pushStartTime);

    long pushMergedDataTime = System.nanoTime();
    rssShuffleClient.pushMergedData(appId, shuffleId, mapId, taskContext.attemptNumber());
    writeMetrics.incWriteTime(System.nanoTime() - pushMergedDataTime);

    updateMapStatus();

    long waitStartTime = System.nanoTime();
    rssShuffleClient.mapperEnd(appId, shuffleId, mapId, taskContext.attemptNumber(), numMappers);
    writeMetrics.incWriteTime(System.nanoTime() - waitStartTime);
  }

  private void updateMapStatus() {
    long recordsWritten = 0;
    for (int i = 0; i < partitioner.numPartitions(); i++) {
      mapStatusRecords[i] += tmpRecords[i];
      recordsWritten += tmpRecords[i];
      tmpRecords[i] = 0;
    }
    writeMetrics.incRecordsWritten(recordsWritten);
  }

  @Override
  public Option<MapStatus> stop(boolean success) {
    try {
      taskContext.taskMetrics().incPeakExecutionMemory(peakMemoryUsedBytes);

      if (stopping) {
        return Option.empty();
      } else {
        stopping = true;
        if (success) {
          BlockManagerId bmId = SparkEnv.get().blockManager().shuffleServerId();
          MapStatus mapStatus =
              SparkUtils.createMapStatus(
                  bmId, SparkUtils.unwrap(mapStatusLengths), taskContext.taskAttemptId());
          if (mapStatus == null) {
            throw new IllegalStateException("Cannot call stop(true) without having called write()");
          }
          return Option.apply(mapStatus);
        } else {
          return Option.empty();
        }
      }
    } finally {
      rssShuffleClient.cleanup(appId, shuffleId, mapId, taskContext.attemptNumber());
    }
  }

  // Added in SPARK-32917, for Spark 3.2 and above
  public long[] getPartitionLengths() {
    throw new UnsupportedOperationException(
        "RSS is not compatible with Spark push mode, please set spark.shuffle.push.enabled to false");
  }
}
