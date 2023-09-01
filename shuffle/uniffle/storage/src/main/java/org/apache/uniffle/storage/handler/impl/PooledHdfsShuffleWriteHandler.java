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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.ShufflePartitionedBlock;
import org.apache.uniffle.storage.handler.api.ShuffleWriteHandler;
import org.apache.uniffle.storage.util.ShuffleStorageUtils;

public class PooledHdfsShuffleWriteHandler implements ShuffleWriteHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(PooledHdfsShuffleWriteHandler.class);

  private final BlockingQueue<HdfsShuffleWriteHandler> queue;
  private final int maxConcurrency;
  private final String basePath;

  public PooledHdfsShuffleWriteHandler(
      String appId,
      int shuffleId,
      int startPartition,
      int endPartition,
      String storageBasePath,
      String fileNamePrefix,
      Configuration hadoopConf,
      String user,
      int concurrency) {
    // todo: support max concurrency specified by client side
    this.maxConcurrency = concurrency;
    this.queue = new LinkedBlockingQueue<>(maxConcurrency);
    this.basePath = ShuffleStorageUtils.getFullShuffleDataFolder(storageBasePath,
        ShuffleStorageUtils.getShuffleDataPath(appId, shuffleId, startPartition, endPartition));

    // todo: support init lazily
    try {
      for (int i = 0; i < maxConcurrency; i++) {
        // Use add() here because we are sure the capacity will not be exceeded.
        // Note: add() throws IllegalStateException when queue is full.
        queue.add(
            new HdfsShuffleWriteHandler(
                appId,
                shuffleId,
                startPartition,
                endPartition,
                storageBasePath,
                fileNamePrefix + "_" + i,
                hadoopConf,
                user
            )
        );
      }
    } catch (Exception e) {
      throw new RuntimeException("Errors on initializing Hdfs writer handler.", e);
    }
  }

  @Override
  public void write(List<ShufflePartitionedBlock> shuffleBlocks) throws Exception {
    if (queue.isEmpty()) {
      LOGGER.warn("No free hdfs writer handler, it will wait. storage path: {}", basePath);
    }
    HdfsShuffleWriteHandler writeHandler = queue.take();
    try {
      writeHandler.write(shuffleBlocks);
    } finally {
      // Use add() here because we are sure the capacity will not be exceeded.
      // Note: add() throws IllegalStateException when queue is full.
      queue.add(writeHandler);
    }
  }
}
