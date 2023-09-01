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

package org.apache.uniffle.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.util.RssUtils;
import org.apache.uniffle.storage.common.LocalStorage;
import org.apache.uniffle.storage.util.ShuffleStorageUtils;

public class LocalStorageChecker extends Checker {

  private static final Logger LOG = LoggerFactory.getLogger(LocalStorageChecker.class);
  public static final String CHECKER_DIR_NAME = ".check";

  private final double diskMaxUsagePercentage;
  private final double diskRecoveryUsagePercentage;
  private final double minStorageHealthyPercentage;
  private final List<StorageInfo> storageInfos  = Lists.newArrayList();
  private boolean isHealthy = true;

  public LocalStorageChecker(ShuffleServerConf conf, List<LocalStorage> storages) {
    super(conf);
    List<String> basePaths = RssUtils.getConfiguredLocalDirs(conf);
    if (CollectionUtils.isEmpty(basePaths)) {
      throw new IllegalArgumentException("The base path cannot be empty");
    }
    String storageType = conf.getString(ShuffleServerConf.RSS_STORAGE_TYPE);
    if (!ShuffleStorageUtils.containsLocalFile(storageType)) {
      throw new IllegalArgumentException("Only StorageType contains LOCALFILE support storageChecker");
    }
    for (LocalStorage storage : storages) {
      storageInfos.add(new StorageInfo(storage));
    }

    this.diskMaxUsagePercentage = conf.getDouble(ShuffleServerConf.HEALTH_STORAGE_MAX_USAGE_PERCENTAGE);
    this.diskRecoveryUsagePercentage = conf.getDouble(ShuffleServerConf.HEALTH_STORAGE_RECOVERY_USAGE_PERCENTAGE);
    this.minStorageHealthyPercentage = conf.getDouble(ShuffleServerConf.HEALTH_MIN_STORAGE_PERCENTAGE);
  }

  @Override
  public boolean checkIsHealthy() {
    int num = 0;
    long totalSpace = 0L;
    long usedSpace = 0L;
    int corruptedDirs = 0;

    for (StorageInfo storageInfo : storageInfos) {
      if (!storageInfo.checkStorageReadAndWrite()) {
        storageInfo.markCorrupted();
        corruptedDirs++;
        continue;
      }

      totalSpace += getTotalSpace(storageInfo.storageDir);
      usedSpace += getUsedSpace(storageInfo.storageDir);

      if (storageInfo.checkIsSpaceEnough()) {
        num++;
      }
    }

    ShuffleServerMetrics.gaugeLocalStorageTotalSpace.set(totalSpace);
    ShuffleServerMetrics.gaugeLocalStorageUsedSpace.set(usedSpace);
    ShuffleServerMetrics.gaugeLocalStorageTotalDirsNum.set(storageInfos.size());
    ShuffleServerMetrics.gaugeLocalStorageCorruptedDirsNum.set(corruptedDirs);
    ShuffleServerMetrics.gaugeLocalStorageUsedSpaceRatio.set(usedSpace * 1.0 / totalSpace);

    if (storageInfos.isEmpty()) {
      if (isHealthy) {
        LOG.info("shuffle server become unhealthy because of empty storage");
      }
      isHealthy = false;
      return false;
    }

    double availablePercentage = num * 100.0 / storageInfos.size();
    if (Double.compare(availablePercentage, minStorageHealthyPercentage) >= 0) {
      if (!isHealthy) {
        LOG.info("shuffle server become healthy");
      }
      isHealthy = true;
    } else {
      if (isHealthy) {
        LOG.info("shuffle server become unhealthy");
      }
      isHealthy = false;
    }
    return isHealthy;
  }

  // Only for testing
  @VisibleForTesting
  long getTotalSpace(File file) {
    return file.getTotalSpace();
  }

  // Only for testing
  @VisibleForTesting
  long getUsedSpace(File file) {
    return file.getTotalSpace() - file.getUsableSpace();
  }

  // todo: This function will be integrated to MultiStorageManager, currently we only support disk check.
  class StorageInfo {

    private final File storageDir;
    private final LocalStorage storage;
    private boolean isHealthy;

    StorageInfo(LocalStorage storage) {
      this.storageDir = new File(storage.getBasePath());
      this.isHealthy = true;
      this.storage = storage;
    }

    boolean checkIsSpaceEnough() {

      if (Double.compare(0.0, getTotalSpace(storageDir)) == 0) {
        this.isHealthy = false;
        return false;
      }
      double usagePercent = getUsedSpace(storageDir) * 100.0 / getTotalSpace(storageDir);
      if (isHealthy) {
        if (Double.compare(usagePercent, diskMaxUsagePercentage) >= 0) {
          isHealthy = false;
          LOG.info("storage {} become unhealthy", storageDir.getAbsolutePath());
        }
      } else {
        if (Double.compare(usagePercent, diskRecoveryUsagePercentage) <= 0) {
          isHealthy = true;
          LOG.info("storage {} become healthy", storageDir.getAbsolutePath());
        }
      }
      return isHealthy;
    }

    boolean checkStorageReadAndWrite() {
      if (storage.isCorrupted()) {
        return false;
      }
      // Use the hidden file to avoid being cleanup
      File checkDir = new File(storageDir, CHECKER_DIR_NAME);
      try {
        if (!checkDir.mkdirs()) {
          return false;
        }
        File writeFile = new File(checkDir, "test");
        if (!writeFile.createNewFile()) {
          return false;
        }
        byte[] data = RandomUtils.nextBytes(1024);
        try (FileOutputStream fos = new FileOutputStream(writeFile)) {
          fos.write(data);
          fos.flush();
          fos.getFD().sync();
        }
        byte[] readData = new byte[1024];
        int readBytes = -1;
        try (FileInputStream fis = new FileInputStream(writeFile)) {
          int hasReadBytes = 0;
          do {
            readBytes = fis.read(readData);
            if (hasReadBytes < 1024) {
              for (int i = 0; i < readBytes; i++) {
                if (data[hasReadBytes + i] != readData[i]) {
                  return false;
                }
              }
            }
            hasReadBytes += readBytes;
          } while (readBytes != -1);
        }
      } catch (Exception e) {
        LOG.error("Storage read and write error. Storage dir: {}", storageDir, e);
        return false;
      } finally {
        try {
          FileUtils.deleteDirectory(checkDir);
        } catch (IOException ioe) {
          LOG.error("delete directory fail. Storage dir: {}", storageDir, ioe);
          return false;
        }
      }
      return true;
    }

    public void markCorrupted() {
      storage.markCorrupted();
    }
  }
}
