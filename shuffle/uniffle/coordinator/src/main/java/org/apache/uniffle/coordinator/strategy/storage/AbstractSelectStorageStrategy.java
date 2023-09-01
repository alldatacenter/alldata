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

package org.apache.uniffle.coordinator.strategy.storage;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.RandomUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.coordinator.util.CoordinatorUtils;

/**
 * This is a simple implementation class, which provides some methods to check whether the path is normal
 */
public abstract class AbstractSelectStorageStrategy implements SelectStorageStrategy {
  /**
   * store remote path -> application count for assignment strategy
   */
  protected final Map<String, RankValue> remoteStoragePathRankValue;
  protected final int fileSize;
  private final String coordinatorId;

  public AbstractSelectStorageStrategy(
      Map<String, RankValue> remoteStoragePathRankValue,
      CoordinatorConf conf) {
    this.remoteStoragePathRankValue = remoteStoragePathRankValue;
    fileSize = conf.getInteger(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_SCHEDULE_FILE_SIZE);
    this.coordinatorId = conf.getString(CoordinatorUtils.COORDINATOR_ID, UUID.randomUUID().toString());
  }

  public void readAndWriteHdfsStorage(FileSystem fs, Path testPath,
      String uri, RankValue rankValue) throws IOException {
    byte[] data = RandomUtils.nextBytes(fileSize);
    try (FSDataOutputStream fos = fs.create(testPath)) {
      fos.write(data);
      fos.flush();
    }
    byte[] readData = new byte[fileSize];
    int readBytes;
    try (FSDataInputStream fis = fs.open(testPath)) {
      int hasReadBytes = 0;
      do {
        readBytes = fis.read(readData);
        if (hasReadBytes < fileSize) {
          for (int i = 0; i < readBytes; i++) {
            if (data[hasReadBytes + i] != readData[i]) {
              remoteStoragePathRankValue.put(uri, new RankValue(Long.MAX_VALUE, rankValue.getAppNum().get()));
              throw new RssException("The content of reading and writing is inconsistent.");
            }
          }
        }
        hasReadBytes += readBytes;
      } while (readBytes != -1);
    }
  }

  String getCoordinatorId() {
    return coordinatorId;
  }
}
