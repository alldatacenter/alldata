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

import java.io.File;
import java.io.FilenameFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.ShuffleIndexResult;
import org.apache.uniffle.common.exception.FileNotFoundException;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.storage.common.FileBasedShuffleSegment;
import org.apache.uniffle.storage.handler.api.ServerReadHandler;
import org.apache.uniffle.storage.util.ShuffleStorageUtils;

public class LocalFileServerReadHandler implements ServerReadHandler {

  private static final Logger LOG = LoggerFactory.getLogger(LocalFileServerReadHandler.class);
  private String indexFileName = "";
  private String dataFileName = "";
  private String appId;
  private int shuffleId;
  private int partitionId;

  public LocalFileServerReadHandler(
      String appId,
      int shuffleId,
      int partitionId,
      int partitionNumPerRange,
      int partitionNum,
      String path) {
    this.appId = appId;
    this.shuffleId = shuffleId;
    this.partitionId = partitionId;
    init(appId, shuffleId, partitionId, partitionNumPerRange, partitionNum, path);
  }

  private void init(
      String appId,
      int shuffleId,
      int partitionId,
      int partitionNumPerRange,
      int partitionNum,
      String path) {

    long start = System.currentTimeMillis();
    prepareFilePath(appId, shuffleId, partitionId, partitionNumPerRange, partitionNum, path);
    LOG.debug("Prepare for appId[" + appId + "], shuffleId[" + shuffleId + "], partitionId[" + partitionId
        + "] cost " + (System.currentTimeMillis() - start) + " ms");
  }

  private void prepareFilePath(
      String appId,
      int shuffleId,
      int partitionId,
      int partitionNumPerRange,
      int partitionNum,
      String storageBasePath) {
    String fullShufflePath = ShuffleStorageUtils.getFullShuffleDataFolder(storageBasePath,
        ShuffleStorageUtils.getShuffleDataPathWithRange(
            appId, shuffleId, partitionId, partitionNumPerRange, partitionNum));

    File baseFolder = new File(fullShufflePath);
    if (!baseFolder.exists()) {
      // the partition doesn't exist in this base folder, skip
      throw new FileNotFoundException("Can't find folder " + fullShufflePath);
    }
    File[] indexFiles;
    String failedGetIndexFileMsg = "No index file found in  " + storageBasePath;
    try {
      // get all index files
      indexFiles = baseFolder.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          return name.endsWith(Constants.SHUFFLE_INDEX_FILE_SUFFIX);
        }
      });
    } catch (Exception e) {
      throw new FileNotFoundException(failedGetIndexFileMsg, e);
    }

    if (indexFiles != null && indexFiles.length > 0) {
      if (indexFiles.length != 1) {
        throw new RuntimeException("More index file than expected: " + indexFiles.length);
      }
      String fileNamePrefix = getFileNamePrefix(indexFiles[0].getName());
      indexFileName = fullShufflePath + "/" + ShuffleStorageUtils.generateIndexFileName(fileNamePrefix);
      dataFileName = fullShufflePath + "/" + ShuffleStorageUtils.generateDataFileName(fileNamePrefix);
    }
  }

  private String getFileNamePrefix(String fileName) {
    int point = fileName.lastIndexOf(".");
    return fileName.substring(0, point);
  }

  private LocalFileReader createFileReader(String path) throws Exception {
    return new LocalFileReader(path);
  }

  @Override
  public ShuffleDataResult getShuffleData(long offset, int length) {
    byte[] readBuffer = new byte[0];

    try {
      long start = System.currentTimeMillis();
      try (LocalFileReader reader = createFileReader(dataFileName)) {
        readBuffer = reader.read(offset, length);
      }
      LOG.debug(
          "Read File segment: {}, offset[{}], length[{}], cost: {} ms, for appId[{}], shuffleId[{}], partitionId[{}]",
          dataFileName, offset, length, System.currentTimeMillis() - start, appId, shuffleId, partitionId);
    } catch (Exception e) {
      LOG.warn("Can't read data for{}, offset[{}], length[{}]", dataFileName, offset, length);
    }

    return new ShuffleDataResult(readBuffer);
  }

  @Override
  public ShuffleIndexResult getShuffleIndex() {
    int indexNum = 0;
    int len = 0;
    try (LocalFileReader reader = createFileReader(indexFileName)) {
      long indexFileSize = new File(indexFileName).length();
      indexNum = (int)  (indexFileSize / FileBasedShuffleSegment.SEGMENT_SIZE);
      len = indexNum * FileBasedShuffleSegment.SEGMENT_SIZE;
      if (indexFileSize != len) {
        LOG.warn("Maybe the index file: {} is being written due to the shuffle-buffer flushing.", indexFileName);
      }
      byte[] indexData = reader.read(0, len);
      // get dataFileSize for read segment generation in DataSkippableReadHandler#readShuffleData
      long dataFileSize = new File(dataFileName).length();
      return new ShuffleIndexResult(indexData, dataFileSize);
    } catch (Exception e) {
      LOG.error("Fail to read index file {} indexNum {} len {}",
          indexFileName, indexNum, len);
      return new ShuffleIndexResult();
    }
  }
}
