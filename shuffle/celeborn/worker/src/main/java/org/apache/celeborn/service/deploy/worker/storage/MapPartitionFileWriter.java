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

package org.apache.celeborn.service.deploy.worker.storage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.meta.FileInfo;
import org.apache.celeborn.common.metrics.source.AbstractSource;
import org.apache.celeborn.common.protocol.PartitionSplitMode;
import org.apache.celeborn.common.protocol.PartitionType;
import org.apache.celeborn.common.util.Utils;
import org.apache.celeborn.service.deploy.worker.WorkerSource;

/*
 * map partition file writer, it will create index for each partition
 */
public final class MapPartitionFileWriter extends FileWriter {
  private static final Logger logger = LoggerFactory.getLogger(MapPartitionFileWriter.class);

  private int numReducerPartitions;
  private int currentDataRegionIndex;
  private boolean isBroadcastRegion;
  private long[] numReducerPartitionBytes;
  private ByteBuffer indexBuffer;
  private int currentReducePartition;
  private long totalBytes;
  private long regionStartingOffset;
  private long numDataRegions;
  private FileChannel channelIndex;

  public MapPartitionFileWriter(
      FileInfo fileInfo,
      Flusher flusher,
      AbstractSource workerSource,
      CelebornConf conf,
      DeviceMonitor deviceMonitor,
      long splitThreshold,
      PartitionSplitMode splitMode,
      boolean rangeReadFilter)
      throws IOException {
    super(
        fileInfo,
        flusher,
        workerSource,
        conf,
        deviceMonitor,
        splitThreshold,
        splitMode,
        PartitionType.MAP,
        rangeReadFilter);
    if (!fileInfo.isHdfs()) {
      channelIndex = new FileOutputStream(fileInfo.getIndexPath()).getChannel();
    } else {
      try {
        StorageManager.hadoopFs().create(fileInfo.getHdfsIndexPath(), true).close();
      } catch (IOException e) {
        try {
          // If create file failed, wait 10 ms and retry
          Thread.sleep(10);
        } catch (InterruptedException ex) {
          throw new RuntimeException(ex);
        }
        StorageManager.hadoopFs().create(fileInfo.getHdfsIndexPath(), true).close();
      }
    }
    takeBufferIndex();
  }

  private void takeBufferIndex() {
    // metrics start
    String metricsName = null;
    String fileAbsPath = null;
    if (source.metricsCollectCriticalEnabled()) {
      metricsName = WorkerSource.TakeBufferTimeIndex();
      fileAbsPath = fileInfo.getIndexPath();
      source.startTimer(metricsName, fileAbsPath);
    }

    // metrics end
    if (source.metricsCollectCriticalEnabled()) {
      source.stopTimer(metricsName, fileAbsPath);
    }
  }

  public void write(ByteBuf data) throws IOException {
    data.markReaderIndex();
    int partitionId = data.readInt();
    int attemptId = data.readInt();
    int batchId = data.readInt();
    int size = data.readInt();

    data.resetReaderIndex();
    logger.debug(
        "mappartition filename:{} write partition:{} attemptId:{} batchId:{} size:{}",
        fileInfo.getFilePath(),
        partitionId,
        attemptId,
        batchId,
        size);

    collectPartitionDataLength(partitionId, data);

    super.write(data);
  }

  private void collectPartitionDataLength(int partitionId, ByteBuf data) throws IOException {
    if (numReducerPartitionBytes == null) {
      numReducerPartitionBytes = new long[numReducerPartitions];
    }
    if (partitionId < currentReducePartition) {
      throw new IOException(
          "Must writing data in reduce partition index order, but now partitionId is "
              + partitionId
              + " and pre partitionId is "
              + currentReducePartition);
    }

    if (partitionId > currentReducePartition) {
      currentReducePartition = partitionId;
    }
    long length = data.readableBytes();
    totalBytes += length;
    numReducerPartitionBytes[partitionId] += length;
  }

  @Override
  public synchronized long close() throws IOException {
    return super.close(
        () -> {
          flushIndex();
        },
        () -> {
          if (StorageManager.hadoopFs().exists(fileInfo.getHdfsPeerWriterSuccessPath())) {
            StorageManager.hadoopFs().delete(fileInfo.getHdfsPath(), false);
            deleted = true;
          } else {
            StorageManager.hadoopFs().create(fileInfo.getHdfsWriterSuccessPath()).close();
          }
        },
        () -> {
          if (channelIndex != null) {
            channelIndex.close();
          }
          if (fileInfo.isHdfs()) {
            if (StorageManager.hadoopFs()
                .exists(
                    new Path(
                        Utils.getWriteSuccessFilePath(
                            Utils.getPeerPath(fileInfo.getIndexPath()))))) {
              StorageManager.hadoopFs().delete(fileInfo.getHdfsIndexPath(), false);
              deleted = true;
            } else {
              StorageManager.hadoopFs()
                  .create(new Path(Utils.getWriteSuccessFilePath((fileInfo.getIndexPath()))))
                  .close();
            }
          }
        });
  }

  public synchronized void destroy(IOException ioException) {
    destroyIndex();
    super.destroy(ioException);
  }

  public void pushDataHandShake(int numReducerPartitions, int bufferSize) {
    logger.debug(
        "FileWriter:{} pushDataHandShake numReducePartitions:{} bufferSize:{}",
        fileInfo.getFilePath(),
        numReducerPartitions,
        bufferSize);

    this.numReducerPartitions = numReducerPartitions;
    fileInfo.setBufferSize(bufferSize);
    fileInfo.setNumReducerPartitions(numReducerPartitions);
  }

  public void regionStart(int currentDataRegionIndex, boolean isBroadcastRegion) {
    logger.debug(
        "FileWriter:{} regionStart currentDataRegionIndex:{} isBroadcastRegion:{}",
        fileInfo.getFilePath(),
        currentDataRegionIndex,
        isBroadcastRegion);

    this.currentReducePartition = 0;
    this.currentDataRegionIndex = currentDataRegionIndex;
    this.isBroadcastRegion = isBroadcastRegion;
  }

  public void regionFinish() throws IOException {
    logger.debug("FileWriter:{} regionFinish", fileInfo.getFilePath());
    if (regionStartingOffset == totalBytes) {
      return;
    }

    long fileOffset = regionStartingOffset;
    if (indexBuffer == null) {
      indexBuffer = allocateIndexBuffer(numReducerPartitions);
    }

    // write the index information of the current data region
    for (int partitionIndex = 0; partitionIndex < numReducerPartitions; ++partitionIndex) {
      indexBuffer.putLong(fileOffset);
      if (!isBroadcastRegion) {
        logger.debug(
            "flush index filename:{} region:{} partitionid:{} flush index fileOffset:{}, size:{} ",
            fileInfo.getFilePath(),
            currentDataRegionIndex,
            partitionIndex,
            fileOffset,
            numReducerPartitionBytes[partitionIndex]);

        indexBuffer.putLong(numReducerPartitionBytes[partitionIndex]);
        fileOffset += numReducerPartitionBytes[partitionIndex];
      } else {
        logger.debug(
            "flush index broadcast filename:{} region:{} partitionid:{}  fileOffset:{}, size:{} ",
            fileInfo.getFilePath(),
            currentDataRegionIndex,
            partitionIndex,
            fileOffset,
            numReducerPartitionBytes[0]);

        indexBuffer.putLong(numReducerPartitionBytes[0]);
      }
    }

    if (!indexBuffer.hasRemaining()) {
      flushIndex();
      takeBufferIndex();
    }

    ++numDataRegions;
    regionStartingOffset = totalBytes;
    Arrays.fill(numReducerPartitionBytes, 0);
  }

  private synchronized void destroyIndex() {
    try {
      if (channelIndex != null) {
        channelIndex.close();
      }
    } catch (IOException e) {
      logger.warn(
          "Close channel failed for file {} caused by {}.",
          fileInfo.getIndexPath(),
          e.getMessage());
    }
  }

  private void flushIndex() throws IOException {
    if (indexBuffer != null) {
      logger.debug("flushIndex start:{}", fileInfo.getIndexPath());
      long startTime = System.currentTimeMillis();
      indexBuffer.flip();
      notifier.checkException();
      try {
        notifier.numPendingFlushes.incrementAndGet();
        if (indexBuffer.hasRemaining()) {
          // mappartition synchronously writes file index
          if (channelIndex != null) {
            while (indexBuffer.hasRemaining()) {
              channelIndex.write(indexBuffer);
            }

          } else if (fileInfo.isHdfs()) {
            FSDataOutputStream hdfsStream =
                StorageManager.hadoopFs().append(fileInfo.getHdfsIndexPath());
            hdfsStream.write(indexBuffer.array());
            hdfsStream.close();
          }
        }
        indexBuffer.clear();

      } finally {
        logger.info(
            "flushIndex end:{}, cost:{}",
            fileInfo.getIndexPath(),
            System.currentTimeMillis() - startTime);
        notifier.numPendingFlushes.decrementAndGet();
      }
    }
  }

  private ByteBuffer allocateIndexBuffer(int numPartitions) {

    // the returned buffer size is no smaller than 4096 bytes to improve disk IO performance
    int minBufferSize = 4096;

    int indexRegionSize = numPartitions * (8 + 8);
    if (indexRegionSize >= minBufferSize) {
      ByteBuffer buffer = ByteBuffer.allocateDirect(indexRegionSize);
      buffer.order(ByteOrder.BIG_ENDIAN);
      return buffer;
    }

    int numRegions = minBufferSize / indexRegionSize;
    if (minBufferSize % indexRegionSize != 0) {
      ++numRegions;
    }
    ByteBuffer buffer = ByteBuffer.allocateDirect(numRegions * indexRegionSize);
    buffer.order(ByteOrder.BIG_ENDIAN);

    return buffer;
  }
}
