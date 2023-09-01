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

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.roaringbitmap.longlong.Roaring64NavigableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.ShuffleDataDistributionType;
import org.apache.uniffle.common.ShuffleDataResult;
import org.apache.uniffle.common.filesystem.HadoopFilesystemProvider;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.storage.util.ShuffleStorageUtils;

public class HdfsClientReadHandler extends AbstractClientReadHandler {

  private static final Logger LOG = LoggerFactory.getLogger(HdfsClientReadHandler.class);

  protected final int partitionNumPerRange;
  protected final int partitionNum;
  protected final int readBufferSize;
  private final String shuffleServerId;
  protected Roaring64NavigableMap expectBlockIds;
  protected Roaring64NavigableMap processBlockIds;
  protected final String storageBasePath;
  protected final Configuration hadoopConf;
  protected final List<HdfsShuffleReadHandler> readHandlers = Lists.newArrayList();
  private int readHandlerIndex;
  private ShuffleDataDistributionType distributionType;
  private Roaring64NavigableMap expectTaskIds;

  public HdfsClientReadHandler(
      String appId,
      int shuffleId,
      int partitionId,
      int indexReadLimit,
      int partitionNumPerRange,
      int partitionNum,
      int readBufferSize,
      Roaring64NavigableMap expectBlockIds,
      Roaring64NavigableMap processBlockIds,
      String storageBasePath,
      Configuration hadoopConf,
      ShuffleDataDistributionType distributionType,
      Roaring64NavigableMap expectTaskIds,
      String shuffleServerId) {
    this.appId = appId;
    this.shuffleId = shuffleId;
    this.partitionId = partitionId;
    this.partitionNumPerRange = partitionNumPerRange;
    this.partitionNum = partitionNum;
    this.readBufferSize = readBufferSize;
    this.expectBlockIds = expectBlockIds;
    this.processBlockIds = processBlockIds;
    this.storageBasePath = storageBasePath;
    this.hadoopConf = hadoopConf;
    this.readHandlerIndex = 0;
    this.distributionType = distributionType;
    this.expectTaskIds = expectTaskIds;
    this.shuffleServerId = shuffleServerId;
  }

  // Only for test
  public HdfsClientReadHandler(
      String appId,
      int shuffleId,
      int partitionId,
      int indexReadLimit,
      int partitionNumPerRange,
      int partitionNum,
      int readBufferSize,
      Roaring64NavigableMap expectBlockIds,
      Roaring64NavigableMap processBlockIds,
      String storageBasePath,
      Configuration hadoopConf) {
    this(appId, shuffleId, partitionId, indexReadLimit, partitionNumPerRange, partitionNum, readBufferSize,
        expectBlockIds, processBlockIds, storageBasePath, hadoopConf, ShuffleDataDistributionType.NORMAL,
        Roaring64NavigableMap.bitmapOf(), null);
  }

  protected void init(String fullShufflePath) {
    FileSystem fs;
    Path baseFolder = new Path(fullShufflePath);
    try {
      fs = HadoopFilesystemProvider.getFilesystem(baseFolder, hadoopConf);
    } catch (Exception ioe) {
      throw new RuntimeException("Can't get FileSystem for " + baseFolder);
    }

    FileStatus[] indexFiles = null;
    try {
      // get all index files
      indexFiles = fs.listStatus(baseFolder,
          file -> file.getName().endsWith(Constants.SHUFFLE_INDEX_FILE_SUFFIX)
              && (shuffleServerId == null || file.getName().startsWith(shuffleServerId)));
    } catch (Exception e) {
      if (e instanceof FileNotFoundException) {
        LOG.info("Directory[" + baseFolder
            + "] not found. The data may not be flushed to this directory. Nothing will be read.");
      } else {
        String failedGetIndexFileMsg = "Can't list index file in  " + baseFolder;
        LOG.error(failedGetIndexFileMsg, e);
      }
      return;
    }

    if (indexFiles != null && indexFiles.length != 0) {
      for (FileStatus status : indexFiles) {
        LOG.info("Find index file for shuffleId[" + shuffleId + "], partitionId["
            + partitionId + "] " + status.getPath());
        String filePrefix = getFileNamePrefix(status.getPath().toUri().toString());
        try {
          HdfsShuffleReadHandler handler = new HdfsShuffleReadHandler(
              appId, shuffleId, partitionId, filePrefix,
              readBufferSize, expectBlockIds, processBlockIds, hadoopConf,
              distributionType, expectTaskIds);
          readHandlers.add(handler);
        } catch (Exception e) {
          LOG.warn("Can't create ShuffleReaderHandler for " + filePrefix, e);
        }
      }
      Collections.shuffle(readHandlers);
      LOG.info("Reading order of HDFS files with name prefix: {}",
          readHandlers.stream().map(x -> x.filePrefix).collect(Collectors.toList())
      );
    }
  }

  @Override
  public ShuffleDataResult readShuffleData() {
    // init lazily like LocalFileClientRead
    if (readHandlers.isEmpty()) {
      String fullShufflePath = ShuffleStorageUtils.getFullShuffleDataFolder(storageBasePath,
          ShuffleStorageUtils.getShuffleDataPathWithRange(appId,
              shuffleId, partitionId, partitionNumPerRange, partitionNum));
      init(fullShufflePath);
    }

    if (readHandlerIndex >= readHandlers.size()) {
      return new ShuffleDataResult();
    }

    HdfsShuffleReadHandler hdfsShuffleFileReader = readHandlers.get(readHandlerIndex);
    ShuffleDataResult shuffleDataResult = hdfsShuffleFileReader.readShuffleData();

    while (shuffleDataResult == null) {
      ++readHandlerIndex;
      if (readHandlerIndex >= readHandlers.size()) {
        return new ShuffleDataResult();
      }
      hdfsShuffleFileReader = readHandlers.get(readHandlerIndex);
      shuffleDataResult = hdfsShuffleFileReader.readShuffleData();
    }

    return shuffleDataResult;
  }

  protected String getFileNamePrefix(String fileName) {
    int point = fileName.lastIndexOf(".");
    return fileName.substring(0, point);
  }

  @Override
  public synchronized void close() {
    for (HdfsShuffleReadHandler handler : readHandlers) {
      handler.close();
    }
  }

  protected List<HdfsShuffleReadHandler> getHdfsShuffleFileReadHandlers() {
    return readHandlers;
  }

  protected int getReadHandlerIndex() {
    return readHandlerIndex;
  }
}
