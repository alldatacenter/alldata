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

package org.apache.uniffle.storage.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Queues;
import org.apache.commons.io.FileUtils;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.storage.StorageMedia;
import org.apache.uniffle.common.util.RssUtils;
import org.apache.uniffle.storage.handler.api.ServerReadHandler;
import org.apache.uniffle.storage.handler.api.ShuffleWriteHandler;
import org.apache.uniffle.storage.handler.impl.LocalFileServerReadHandler;
import org.apache.uniffle.storage.handler.impl.LocalFileWriteHandler;
import org.apache.uniffle.storage.request.CreateShuffleReadHandlerRequest;
import org.apache.uniffle.storage.request.CreateShuffleWriteHandlerRequest;

public class LocalStorage extends AbstractStorage {

  private static final Logger LOG = LoggerFactory.getLogger(LocalStorage.class);
  public static final String STORAGE_HOST = "local";

  private long capacity;
  private final String basePath;
  private final String mountPoint;
  private final double cleanupThreshold;
  private final long cleanIntervalMs;
  private final double highWaterMarkOfWrite;
  private final double lowWaterMarkOfWrite;
  private final long shuffleExpiredTimeoutMs;
  private final Queue<String> expiredShuffleKeys = Queues.newLinkedBlockingQueue();

  private LocalStorageMeta metaData = new LocalStorageMeta();
  private final StorageMedia media;
  private boolean isSpaceEnough = true;
  private volatile boolean isCorrupted = false;


  private LocalStorage(Builder builder) {
    this.basePath = builder.basePath;
    this.cleanupThreshold = builder.cleanupThreshold;
    this.cleanIntervalMs = builder.cleanIntervalMs;
    this.highWaterMarkOfWrite = builder.highWaterMarkOfWrite;
    this.lowWaterMarkOfWrite = builder.lowWaterMarkOfWrite;
    this.capacity = builder.capacity;
    this.shuffleExpiredTimeoutMs = builder.shuffleExpiredTimeoutMs;
    this.media = builder.media;

    File baseFolder = new File(basePath);
    try {
      // similar to mkdir -p, ensure the base folder is a dir
      FileUtils.forceMkdir(baseFolder);
      // clean the directory if it's data left from previous ran.
      FileUtils.cleanDirectory(baseFolder);
      FileStore store = Files.getFileStore(baseFolder.toPath());
      this.mountPoint =  store.name();
    } catch (IOException ioe) {
      LOG.warn("Init base directory " + basePath + " fail, the disk should be corrupted", ioe);
      throw new RuntimeException(ioe);
    }
    if (capacity < 0L) {
      long totalSpace = baseFolder.getTotalSpace();
      this.capacity = (long) (totalSpace * builder.ratio);
      LOG.info("The `rss.server.disk.capacity` is not specified nor negative, the "
          + "ratio(`rss.server.disk.capacity.ratio`:{}) * disk space({}) is used, ", builder.ratio, totalSpace);
    } else {
      long freeSpace = baseFolder.getFreeSpace();
      if (freeSpace < capacity) {
        throw new IllegalArgumentException("The Disk of " + basePath + " Available Capacity " + freeSpace
            + " is smaller than configuration");
      }
    }
  }

  @Override
  public String getStoragePath() {
    return basePath;
  }

  @Override
  public String getStorageHost() {
    return STORAGE_HOST;
  }

  @Override
  public boolean lockShuffleShared(String shuffleKey) {
    ReadWriteLock lock = getLock(shuffleKey);
    if (lock == null) {
      return false;
    }
    lock.readLock().lock();
    return true;
  }

  @Override
  public boolean unlockShuffleShared(String shuffleKey) {
    ReadWriteLock lock = getLock(shuffleKey);
    if (lock == null) {
      return false;
    }
    lock.readLock().unlock();
    return true;
  }

  @Override
  public void updateWriteMetrics(StorageWriteMetrics metrics) {
    updateWrite(RssUtils.generateShuffleKey(metrics.getAppId(), metrics.getShuffleId()),
        metrics.getDataSize(),
        metrics.getPartitions());
  }

  @Override
  public void updateReadMetrics(StorageReadMetrics metrics) {
    String shuffleKey = RssUtils.generateShuffleKey(metrics.getAppId(), metrics.getShuffleId());
    prepareStartRead(shuffleKey);
    updateShuffleLastReadTs(shuffleKey);
  }

  @Override
  ShuffleWriteHandler newWriteHandler(CreateShuffleWriteHandlerRequest request) {
    return new LocalFileWriteHandler(request.getAppId(),
        request.getShuffleId(),
        request.getStartPartition(),
        request.getEndPartition(),
        basePath,
        request.getFileNamePrefix()
    );
  }

  @Override
  protected ServerReadHandler newReadHandler(CreateShuffleReadHandlerRequest request) {
    return new LocalFileServerReadHandler(
        request.getAppId(),
        request.getShuffleId(),
        request.getPartitionId(),
        request.getPartitionNumPerRange(),
        request.getPartitionNum(),
        basePath);
  }

  @Override
  public boolean canWrite() {
    if (isSpaceEnough) {
      isSpaceEnough = metaData.getDiskSize().doubleValue() * 100 / capacity < highWaterMarkOfWrite;
    } else {
      isSpaceEnough = metaData.getDiskSize().doubleValue() * 100 / capacity < lowWaterMarkOfWrite;
    }
    return isSpaceEnough && !isCorrupted;
  }

  public String getBasePath() {
    return basePath;
  }

  public void createMetadataIfNotExist(String shuffleKey) {
    metaData.createMetadataIfNotExist(shuffleKey);
  }

  public void updateWrite(String shuffleKey, long delta, List<Integer> partitionList) {
    metaData.updateDiskSize(delta);
    metaData.addShufflePartitionList(shuffleKey, partitionList);
    metaData.updateShuffleSize(shuffleKey, delta);
  }

  public void prepareStartRead(String key) {
    metaData.prepareStartRead(key);
  }

  public boolean isShuffleLongTimeNotRead(String shuffleKey) {
    if (metaData.getShuffleLastReadTs(shuffleKey) == -1) {
      return false;
    }
    if (System.currentTimeMillis() - metaData.getShuffleLastReadTs(shuffleKey) > shuffleExpiredTimeoutMs) {
      return true;
    }
    return false;
  }

  public void updateShuffleLastReadTs(String shuffleKey) {
    metaData.updateShuffleLastReadTs(shuffleKey);
  }

  public RoaringBitmap getNotUploadedPartitions(String key) {
    return metaData.getNotUploadedPartitions(key);
  }

  public void updateUploadedShuffle(String shuffleKey, long size, List<Integer> partitions) {
    metaData.updateUploadedShuffleSize(shuffleKey, size);
    metaData.addUploadedShufflePartitionList(shuffleKey, partitions);
  }

  public long getDiskSize() {
    return metaData.getDiskSize().longValue();
  }

  @VisibleForTesting
  public LocalStorageMeta getMetaData() {
    return metaData;
  }

  public long getCapacity() {
    return capacity;
  }

  public String getMountPoint() {
    return mountPoint;
  }

  public StorageMedia getStorageMedia() {
    return media;
  }

  public double getHighWaterMarkOfWrite() {
    return highWaterMarkOfWrite;
  }

  public double getLowWaterMarkOfWrite() {
    return lowWaterMarkOfWrite;
  }

  public void addExpiredShuffleKey(String shuffleKey) {
    expiredShuffleKeys.offer(shuffleKey);
  }

  // This is the only place to remove shuffle metadata, clean and gc thread may remove
  // the shuffle metadata concurrently or serially. Force uploader thread may update the
  // shuffle size so gc thread must acquire write lock before updating disk size, and force
  // uploader thread will not get the lock if the shuffle is removed by gc thread, so
  // add the shuffle key back to the expiredShuffleKeys if get lock but fail to acquire write lock.
  public void removeResources(String shuffleKey) {
    LOG.info("Start to remove resource of {}", shuffleKey);
    ReadWriteLock lock = metaData.getLock(shuffleKey);
    if (lock == null) {
      LOG.info("Ignore shuffle {} for its resource was removed already", shuffleKey);
      return;
    }

    if (lock.writeLock().tryLock()) {
      try {
        metaData.updateDiskSize(-metaData.getShuffleSize(shuffleKey));
        metaData.remoteShuffle(shuffleKey);
        LOG.info("Finish remove resource of {}, disk size is {} and {} shuffle metadata",
            shuffleKey, metaData.getDiskSize(), metaData.getShuffleMetaSet().size());
      } catch (Exception e) {
        LOG.error("Fail to update disk size", e);
        expiredShuffleKeys.offer(shuffleKey);
      } finally {
        lock.writeLock().unlock();
      }
    } else {
      LOG.info("Fail to get write lock of {}, add it back to expired shuffle queue", shuffleKey);
      expiredShuffleKeys.offer(shuffleKey);
    }
  }

  public ReadWriteLock getLock(String shuffleKey) {
    return metaData.getLock(shuffleKey);
  }

  public long getNotUploadedSize(String key) {
    return metaData.getNotUploadedSize(key);
  }

  public List<String> getSortedShuffleKeys(boolean checkRead, int num) {
    return metaData.getSortedShuffleKeys(checkRead, num);
  }

  public Set<String> getShuffleMetaSet() {
    return metaData.getShuffleMetaSet();
  }

  public void removeShuffle(String shuffleKey, long size, List<Integer> partitions) {
    metaData.removeShufflePartitionList(shuffleKey, partitions);
    metaData.updateDiskSize(-size);
    metaData.updateShuffleSize(shuffleKey, -size);
  }

  public Queue<String> getExpiredShuffleKeys() {
    return expiredShuffleKeys;
  }

  public boolean isCorrupted() {
    return isCorrupted;
  }

  public void markCorrupted() {
    isCorrupted = true;
  }

  public Set<String> getAppIds() {
    Set<String> appIds = new HashSet<>();
    File baseFolder = new File(basePath);
    File[] files = baseFolder.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isDirectory() && !file.isHidden()) {
          appIds.add(file.getName());
        }
      }
    }
    return appIds;
  }

  // Only for test
  @VisibleForTesting
  public void markSpaceFull() {
    isSpaceEnough = false;
  }

  public static class Builder {
    private long capacity;
    private double ratio;
    private double lowWaterMarkOfWrite;
    private double highWaterMarkOfWrite;
    private double cleanupThreshold;
    private String basePath;
    private long cleanIntervalMs;
    private long shuffleExpiredTimeoutMs;
    private StorageMedia media;

    private Builder() {
    }

    public Builder capacity(long capacity) {
      this.capacity = capacity;
      return this;
    }

    public Builder ratio(double ratio) {
      this.ratio = ratio;
      return this;
    }

    public Builder lowWaterMarkOfWrite(double lowWaterMarkOfWrite) {
      this.lowWaterMarkOfWrite = lowWaterMarkOfWrite;
      return this;
    }

    public Builder basePath(String basePath) {
      this.basePath = basePath;
      return this;
    }

    public Builder cleanupThreshold(double cleanupThreshold) {
      this.cleanupThreshold = cleanupThreshold;
      return this;
    }

    public Builder highWaterMarkOfWrite(double highWaterMarkOfWrite) {
      this.highWaterMarkOfWrite = highWaterMarkOfWrite;
      return this;
    }

    public Builder cleanIntervalMs(long cleanIntervalMs) {
      this.cleanIntervalMs = cleanIntervalMs;
      return this;
    }

    public Builder shuffleExpiredTimeoutMs(long shuffleExpiredTimeoutMs) {
      this.shuffleExpiredTimeoutMs = shuffleExpiredTimeoutMs;
      return this;
    }

    public Builder localStorageMedia(StorageMedia media) {
      this.media = media;
      return this;
    }

    public LocalStorage build() {
      return new LocalStorage(this);
    }
  }

  public static Builder newBuilder() {
    return new Builder();
  }
}
