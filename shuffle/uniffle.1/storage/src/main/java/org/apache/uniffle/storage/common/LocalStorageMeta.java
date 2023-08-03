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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Metadata has three dimensions from top to down including disk, shuffle, partition.
 *  And each dimension contains two aspects, status data and indicator data.
 *  Disk status data contains writable flag, Shuffle status data contains stable, uploading, deleting flag.
 *  Disk indicator data contains size, fileNum, shuffleNum, Shuffle indicator contains size, partition list,
 *  uploaded partition list and uploaded size.
 */
public class LocalStorageMeta {

  private static final Logger LOG = LoggerFactory.getLogger(LocalStorageMeta.class);
  private final AtomicLong size = new AtomicLong(0L);
  private final Map<String, ShuffleMeta> shuffleMetaMap = Maps.newConcurrentMap();

  // todo: add ut
  public List<String> getSortedShuffleKeys(boolean checkRead, int hint) {
    // Filter the unread shuffle is checkRead is true
    // Filter the remaining size is 0
    List<Map.Entry<String, ShuffleMeta>> shuffleMetaList = shuffleMetaMap
        .entrySet()
        .stream()
        .filter(e -> (!checkRead || e.getValue().isStartRead.get()) && e.getValue().getNotUploadedSize() > 0)
        .collect(Collectors.toList());

    shuffleMetaList.sort((Entry<String, ShuffleMeta> o1, Entry<String, ShuffleMeta> o2) -> {
      long sz1 = o1.getValue().getSize().longValue();
      long sz2 = o2.getValue().getSize().longValue();
      return Long.compare(sz2, sz1);
    });

    return shuffleMetaList
        .subList(0, Math.min(shuffleMetaList.size(), hint))
        .stream()
        .map(Entry::getKey).collect(Collectors.toList());
  }

  public RoaringBitmap getNotUploadedPartitions(String shuffleKey) {
    ShuffleMeta shuffleMeta = getShuffleMeta(shuffleKey);
    if (shuffleMeta == null) {
      return RoaringBitmap.bitmapOf();
    }

    RoaringBitmap partitionBitmap;
    RoaringBitmap uploadedPartitionBitmap;
    synchronized (shuffleMeta.partitionBitmap) {
      partitionBitmap = shuffleMeta.partitionBitmap.clone();
    }
    synchronized (shuffleMeta.uploadedPartitionBitmap) {
      uploadedPartitionBitmap = shuffleMeta.uploadedPartitionBitmap.clone();
    }
    for (int partition : uploadedPartitionBitmap) {
      partitionBitmap.remove(partition);
    }
    return partitionBitmap;
  }

  public long getNotUploadedSize(String shuffleKey) {
    ShuffleMeta shuffleMeta = getShuffleMeta(shuffleKey);
    return shuffleMeta == null ? 0 : shuffleMeta.getNotUploadedSize();
  }

  public long updateDiskSize(long delta) {
    return size.addAndGet(delta);
  }

  public long updateShuffleSize(String shuffleId, long delta) {
    ShuffleMeta shuffleMeta = getShuffleMeta(shuffleId);
    return shuffleMeta == null ? 0 : shuffleMeta.getSize().addAndGet(delta);
  }

  public long updateUploadedShuffleSize(String shuffleKey, long delta) {
    ShuffleMeta shuffleMeta = getShuffleMeta(shuffleKey);
    return shuffleMeta == null ? 0 : shuffleMeta.uploadedSize.addAndGet(delta);
  }

  public void addShufflePartitionList(String shuffleKey, List<Integer> partitions) {
    ShuffleMeta shuffleMeta = getShuffleMeta(shuffleKey);
    if (shuffleMeta != null) {
      RoaringBitmap bitmap = shuffleMeta.partitionBitmap;
      synchronized (bitmap) {
        partitions.forEach(bitmap::add);
      }
    }
  }

  public void addUploadedShufflePartitionList(String shuffleKey, List<Integer> partitions) {
    ShuffleMeta shuffleMeta = getShuffleMeta(shuffleKey);
    if (shuffleMeta != null) {
      RoaringBitmap bitmap = shuffleMeta.uploadedPartitionBitmap;
      synchronized (bitmap) {
        partitions.forEach(bitmap::add);
      }
    }
  }

  public void prepareStartRead(String shuffleId) {
    ShuffleMeta shuffleMeta = getShuffleMeta(shuffleId);
    if (shuffleMeta != null) {
      shuffleMeta.markStartRead();
    }
  }

  public void removeShufflePartitionList(String shuffleKey, List<Integer> partitions) {
    ShuffleMeta shuffleMeta = getShuffleMeta(shuffleKey);
    if (shuffleMeta != null) {
      RoaringBitmap bitmap = shuffleMeta.partitionBitmap;
      synchronized (bitmap) {
        partitions.forEach(bitmap::remove);
      }
    }
  }

  public void remoteShuffle(String shuffleKey) {
    shuffleMetaMap.remove(shuffleKey);
  }

  public AtomicLong getDiskSize() {
    return size;
  }

  public long getShuffleSize(String shuffleKey) {
    ShuffleMeta shuffleMeta = getShuffleMeta(shuffleKey);
    return shuffleMeta == null ? 0 : shuffleMeta.getSize().get();
  }

  public Set<String> getShuffleMetaSet() {
    return shuffleMetaMap.keySet();
  }

  @VisibleForTesting
  public void setSize(long diskSize) {
    this.size.set(diskSize);
  }

  /**
   *  If the method is implemented as below:
   *
   *     if (shuffleMetaMap.contains(shuffleId)) {
   *        // `Time A`
   *        return shuffleMetaMap.get(shuffleId)
   *     } else {
   *        shuffleMetaMap.putIfAbsent(shuffleId, newMeta)
   *        return newMeta
   *    }
   *
   *  Because if shuffleMetaMap remove shuffleId at `Time A` in another thread,
   *  shuffleMetaMap.get(shuffleId) will return null.
   *  We need to guarantee that this method is thread safe, and won't return null.
   **/
  public void createMetadataIfNotExist(String shuffleKey) {
    ShuffleMeta meta = new ShuffleMeta();
    ShuffleMeta oldMeta = shuffleMetaMap.putIfAbsent(shuffleKey, meta);
    if (oldMeta == null) {
      LOG.info("Create metadata of shuffle {}.", shuffleKey);
    }
  }

  private ShuffleMeta getShuffleMeta(String shuffleKey) {
    ShuffleMeta shuffleMeta = shuffleMetaMap.get(shuffleKey);
    if (shuffleMeta == null) {
      LOG.debug("Shuffle {} metadata has been removed!", shuffleKey);
    }
    return shuffleMeta;
  }

  public long getShuffleLastReadTs(String shuffleKey) {
    ShuffleMeta shuffleMeta = getShuffleMeta(shuffleKey);
    return shuffleMeta == null ? -1 : shuffleMeta.getShuffleLastReadTs();
  }

  public void updateShuffleLastReadTs(String shuffleKey) {
    ShuffleMeta shuffleMeta = getShuffleMeta(shuffleKey);
    if (shuffleMeta != null) {
      shuffleMeta.updateLastReadTs();
    }
  }

  public ReadWriteLock getLock(String shuffleKey) {
    ShuffleMeta shuffleMeta = getShuffleMeta(shuffleKey);
    return shuffleMeta == null ? null : shuffleMeta.getLock();
  }

  // Consider that ShuffleMeta is a simple class, we keep the class ShuffleMeta as an inner class.
  private static class ShuffleMeta {
    private final AtomicLong size = new AtomicLong(0);
    private final RoaringBitmap partitionBitmap = RoaringBitmap.bitmapOf();
    private final AtomicLong uploadedSize = new AtomicLong(0);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicBoolean isStartRead = new AtomicBoolean(false);
    private final RoaringBitmap uploadedPartitionBitmap = RoaringBitmap.bitmapOf();
    private AtomicLong lastReadTs = new AtomicLong(-1L);

    public AtomicLong getSize() {
      return size;
    }

    public AtomicLong getUploadedSize() {
      return uploadedSize;
    }

    public long getNotUploadedSize() {
      return size.longValue() - uploadedSize.longValue();
    }

    public boolean isStartRead() {
      return isStartRead.get();
    }

    public void markStartRead() {
      isStartRead.set(true);
    }

    public void updateLastReadTs() {
      lastReadTs.set(System.currentTimeMillis());
    }


    public long getShuffleLastReadTs() {
      return lastReadTs.get();
    }

    public ReadWriteLock getLock() {
      return lock;
    }
  }
}
