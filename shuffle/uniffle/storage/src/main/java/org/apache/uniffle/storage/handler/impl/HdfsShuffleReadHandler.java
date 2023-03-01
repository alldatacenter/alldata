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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.roaringbitmap.longlong.Roaring64NavigableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.ShuffleDataDistributionType;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShuffleDataSegment;
import org.apache.uniffle.common.ShuffleIndexResult;
import org.apache.uniffle.storage.common.FileBasedShuffleSegment;
import org.apache.uniffle.storage.util.ShuffleStorageUtils;

/**
 * HdfsShuffleFileReadHandler is a shuffle-specific file read handler, it contains two HdfsFileReader
 * instances created by using the index file and its indexed data file.
 */
public class HdfsShuffleReadHandler extends DataSkippableReadHandler {
  private static final Logger LOG = LoggerFactory.getLogger(HdfsShuffleReadHandler.class);

  protected final String filePrefix;
  protected final HdfsFileReader indexReader;
  protected final HdfsFileReader dataReader;

  public HdfsShuffleReadHandler(
      String appId,
      int shuffleId,
      int partitionId,
      String filePrefix,
      int readBufferSize,
      Roaring64NavigableMap expectBlockIds,
      Roaring64NavigableMap processBlockIds,
      Configuration conf,
      ShuffleDataDistributionType distributionType,
      Roaring64NavigableMap expectTaskIds) throws Exception {
    super(appId, shuffleId, partitionId, readBufferSize, expectBlockIds, processBlockIds,
        distributionType, expectTaskIds);
    this.filePrefix = filePrefix;
    this.indexReader = createHdfsReader(ShuffleStorageUtils.generateIndexFileName(filePrefix), conf);
    this.dataReader = createHdfsReader(ShuffleStorageUtils.generateDataFileName(filePrefix), conf);
  }

  // Only for test
  public HdfsShuffleReadHandler(
      String appId,
      int shuffleId,
      int partitionId,
      String filePrefix,
      int readBufferSize,
      Roaring64NavigableMap expectBlockIds,
      Roaring64NavigableMap processBlockIds,
      Configuration conf) throws Exception {
    this(appId, shuffleId, partitionId, filePrefix, readBufferSize, expectBlockIds,
        processBlockIds, conf, ShuffleDataDistributionType.NORMAL, Roaring64NavigableMap.bitmapOf());
  }

  @Override
  protected ShuffleIndexResult readShuffleIndex() {
    long start = System.currentTimeMillis();
    try {
      byte[] indexData = indexReader.read();
      int segmentNumber = indexData.length / FileBasedShuffleSegment.SEGMENT_SIZE;
      int expectedLen = segmentNumber * FileBasedShuffleSegment.SEGMENT_SIZE;
      if (indexData.length != expectedLen) {
        LOG.warn("Maybe the index file: {} is being written due to the shuffle-buffer flushing.", filePrefix);
        byte[] indexNewData = new byte[expectedLen];
        System.arraycopy(indexData, 0, indexNewData, 0, expectedLen);
        indexData = indexNewData;
      }
      long dateFileLen = getDataFileLen();
      LOG.info("Read index files {}.index for {} ms", filePrefix, System.currentTimeMillis() - start);
      return new ShuffleIndexResult(indexData, dateFileLen);
    } catch (Exception e) {
      LOG.info("Fail to read index files {}.index", filePrefix, e);
    }
    return new ShuffleIndexResult();
  }

  protected ShuffleDataResult readShuffleData(ShuffleDataSegment shuffleDataSegment) {
    // Here we make an assumption that the rest of the file is corrupted, if an unexpected data is read.
    int expectedLength = shuffleDataSegment.getLength();
    if (expectedLength <= 0) {
      LOG.warn("Invalid data segment is {} from file {}.data", shuffleDataSegment, filePrefix);
      return null;
    }

    byte[] data = readShuffleData(shuffleDataSegment.getOffset(), expectedLength);
    if (data.length == 0) {
      LOG.warn("Fail to read expected[{}] data, actual[{}] and segment is {} from file {}.data",
          expectedLength, data.length, shuffleDataSegment, filePrefix);
      return null;
    }

    ShuffleDataResult shuffleDataResult = new ShuffleDataResult(data, shuffleDataSegment.getBufferSegments());
    if (shuffleDataResult.isEmpty()) {
      LOG.warn("Shuffle data is empty, expected length {}, data length {}, segment {} in file {}.data",
          expectedLength, data.length, shuffleDataSegment, filePrefix);
      return null;
    }

    return shuffleDataResult;
  }

  protected byte[] readShuffleData(long offset, int expectedLength) {
    byte[] data = dataReader.read(offset, expectedLength);
    if (data.length != expectedLength) {
      LOG.warn("Fail to read expected[{}] data, actual[{}] from file {}.data",
          expectedLength, data.length, filePrefix);
      return new byte[0];
    }
    return data;
  }

  private long getDataFileLen() {
    try {
      return dataReader.getFileLen();
    } catch (IOException ioException) {
      LOG.error("getDataFileLen failed for " +  ShuffleStorageUtils.generateDataFileName(filePrefix), ioException);
      return -1;
    }
  }

  public synchronized void close() {
    try {
      dataReader.close();
    } catch (IOException ioe) {
      String message = "Error happened when close index filer reader for " + filePrefix + ".data";
      LOG.warn(message, ioe);
    }

    try {
      indexReader.close();
    } catch (IOException ioe) {
      String message = "Error happened when close data file reader for " + filePrefix + ".index";
      LOG.warn(message, ioe);
    }
  }

  protected HdfsFileReader createHdfsReader(
      String fileName, Configuration hadoopConf) throws Exception {
    Path path = new Path(fileName);
    return new HdfsFileReader(path, hadoopConf);
  }

  public List<ShuffleDataSegment> getShuffleDataSegments() {
    return shuffleDataSegments;
  }

  public String getFilePrefix() {
    return filePrefix;
  }
}
