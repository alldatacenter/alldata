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

package org.apache.celeborn.service.deploy.master.clustermeta;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import scala.Option;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.common.meta.AppDiskUsageMetric;
import org.apache.celeborn.common.meta.AppDiskUsageSnapShot;
import org.apache.celeborn.common.meta.DiskInfo;
import org.apache.celeborn.common.meta.WorkerInfo;
import org.apache.celeborn.common.protocol.PbSnapshotMetaInfo;
import org.apache.celeborn.common.quota.ResourceConsumption;
import org.apache.celeborn.common.rpc.RpcEnv;
import org.apache.celeborn.common.util.JavaUtils;
import org.apache.celeborn.common.util.PbSerDeUtils;
import org.apache.celeborn.common.util.Utils;
import org.apache.celeborn.service.deploy.master.network.CelebornRackResolver;

public abstract class AbstractMetaManager implements IMetadataHandler {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractMetaManager.class);

  // Metadata for master service
  public final Set<String> registeredShuffle = ConcurrentHashMap.newKeySet();
  public final Set<String> hostnameSet = ConcurrentHashMap.newKeySet();
  public final ArrayList<WorkerInfo> workers = new ArrayList<>();
  public final ConcurrentHashMap<WorkerInfo, Long> lostWorkers = JavaUtils.newConcurrentHashMap();
  public final ConcurrentHashMap<String, Long> appHeartbeatTime = JavaUtils.newConcurrentHashMap();
  public final Set<WorkerInfo> excludedWorkers = ConcurrentHashMap.newKeySet();
  public final Set<WorkerInfo> shutdownWorkers = ConcurrentHashMap.newKeySet();
  public final Set<WorkerInfo> workerLostEvents = ConcurrentHashMap.newKeySet();

  protected RpcEnv rpcEnv;
  protected CelebornConf conf;
  protected CelebornRackResolver rackResolver;

  public long initialEstimatedPartitionSize;
  public long estimatedPartitionSize;
  public final LongAdder partitionTotalWritten = new LongAdder();
  public final LongAdder partitionTotalFileCount = new LongAdder();
  public AppDiskUsageMetric appDiskUsageMetric = null;

  public void updateRequestSlotsMeta(
      String shuffleKey, String hostName, Map<String, Map<String, Integer>> workerWithAllocations) {
    registeredShuffle.add(shuffleKey);

    String appId = Utils.splitShuffleKey(shuffleKey)._1;
    appHeartbeatTime.compute(
        appId,
        (applicationId, oldTimestamp) -> {
          long oldTime = System.currentTimeMillis();
          if (oldTimestamp != null) {
            oldTime = oldTimestamp;
          }
          return Math.max(System.currentTimeMillis(), oldTime);
        });

    if (hostName != null) {
      hostnameSet.add(hostName);
    }
  }

  public void updateReleaseSlotsMeta(String shuffleKey) {
    updateReleaseSlotsMeta(shuffleKey, null, null);
  }

  public void updateReleaseSlotsMeta(
      String shuffleKey, List<String> workerIds, List<Map<String, Integer>> slots) {
    if (workerIds != null && !workerIds.isEmpty()) {
      for (int i = 0; i < workerIds.size(); i++) {
        String workerId = workerIds.get(i);
        WorkerInfo worker = WorkerInfo.fromUniqueId(workerId);
        for (WorkerInfo w : workers) {
          if (w.equals(worker)) {
            Map<String, Integer> slotToRelease = slots.get(i);
            LOG.info("release slots for worker {}, to release: {}", w, slotToRelease);
            w.releaseSlots(shuffleKey, slotToRelease);
          }
        }
      }
    } else {
      workers.forEach(workerInfo -> workerInfo.releaseSlots(shuffleKey));
    }
  }

  public void updateUnregisterShuffleMeta(String shuffleKey) {
    registeredShuffle.remove(shuffleKey);
  }

  public void updateAppHeartbeatMeta(String appId, long time, long totalWritten, long fileCount) {
    appHeartbeatTime.put(appId, time);
    partitionTotalWritten.add(totalWritten);
    partitionTotalFileCount.add(fileCount);
  }

  public void updateAppLostMeta(String appId) {
    registeredShuffle.removeIf(shuffleKey -> shuffleKey.startsWith(appId));
    appHeartbeatTime.remove(appId);
  }

  public void updateWorkerLostMeta(
      String host, int rpcPort, int pushPort, int fetchPort, int replicatePort) {
    WorkerInfo worker = new WorkerInfo(host, rpcPort, pushPort, fetchPort, replicatePort);
    workerLostEvents.add(worker);
    // remove worker from workers
    synchronized (workers) {
      workers.remove(worker);
      lostWorkers.put(worker, System.currentTimeMillis());
    }
    excludedWorkers.remove(worker);
    workerLostEvents.remove(worker);
  }

  public void updateWorkerRemoveMeta(
      String host, int rpcPort, int pushPort, int fetchPort, int replicatePort) {
    WorkerInfo worker = new WorkerInfo(host, rpcPort, pushPort, fetchPort, replicatePort);
    // remove worker from workers
    synchronized (workers) {
      workers.remove(worker);
      lostWorkers.put(worker, System.currentTimeMillis());
    }
    excludedWorkers.remove(worker);
  }

  public void updateWorkerHeartbeatMeta(
      String host,
      int rpcPort,
      int pushPort,
      int fetchPort,
      int replicatePort,
      Map<String, DiskInfo> disks,
      Map<UserIdentifier, ResourceConsumption> userResourceConsumption,
      Map<String, Long> estimatedAppDiskUsage,
      long time) {
    WorkerInfo worker =
        new WorkerInfo(
            host, rpcPort, pushPort, fetchPort, replicatePort, disks, userResourceConsumption);
    AtomicLong availableSlots = new AtomicLong();
    LOG.debug("update worker {}:{} heartbeat {}", host, rpcPort, disks);
    synchronized (workers) {
      Optional<WorkerInfo> workerInfo = workers.stream().filter(w -> w.equals(worker)).findFirst();
      workerInfo.ifPresent(
          info -> {
            info.updateThenGetDiskInfos(disks, Option.apply(estimatedPartitionSize));
            info.updateThenGetUserResourceConsumption(userResourceConsumption);
            availableSlots.set(info.totalAvailableSlots());
            info.lastHeartbeat_$eq(time);
          });
    }
    appDiskUsageMetric.update(estimatedAppDiskUsage);
    // If using HDFSONLY mode, workers with empty disks should not be put into excluded worker list.
    if (!excludedWorkers.contains(worker) && (disks.isEmpty() && !conf.hasHDFSStorage())) {
      LOG.debug("Worker: {} num total slots is 0, add to excluded list", worker);
      excludedWorkers.add(worker);
    } else if (availableSlots.get() > 0) {
      // only unblack if numSlots larger than 0
      excludedWorkers.remove(worker);
    }
  }

  public void updateRegisterWorkerMeta(
      String host,
      int rpcPort,
      int pushPort,
      int fetchPort,
      int replicatePort,
      Map<String, DiskInfo> disks,
      Map<UserIdentifier, ResourceConsumption> userResourceConsumption) {
    WorkerInfo workerInfo =
        new WorkerInfo(
            host, rpcPort, pushPort, fetchPort, replicatePort, disks, userResourceConsumption);
    workerInfo.lastHeartbeat_$eq(System.currentTimeMillis());
    workerInfo.networkLocation_$eq(rackResolver.resolve(host).getNetworkLocation());
    workerInfo.updateDiskMaxSlots(estimatedPartitionSize);
    synchronized (workers) {
      if (!workers.contains(workerInfo)) {
        workers.add(workerInfo);
        shutdownWorkers.remove(workerInfo);
        lostWorkers.remove(workerInfo);
      }
    }
  }

  /**
   * Used for ratis state machine to take snapshot
   *
   * @param file
   * @throws IOException
   */
  public void writeMetaInfoToFile(File file) throws IOException, RuntimeException {
    byte[] snapshotBytes =
        PbSerDeUtils.toPbSnapshotMetaInfo(
                estimatedPartitionSize,
                registeredShuffle,
                hostnameSet,
                excludedWorkers,
                workerLostEvents,
                appHeartbeatTime,
                workers,
                partitionTotalWritten.sum(),
                partitionTotalFileCount.sum(),
                appDiskUsageMetric.snapShots(),
                appDiskUsageMetric.currentSnapShot().get(),
                lostWorkers,
                shutdownWorkers)
            .toByteArray();
    Files.write(file.toPath(), snapshotBytes);
  }

  /**
   * Used for ratis state machine to load snapshot
   *
   * @param file
   * @throws IOException
   */
  public void restoreMetaFromFile(File file) throws IOException {
    try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
      PbSnapshotMetaInfo snapshotMetaInfo = PbSnapshotMetaInfo.parseFrom(in);

      estimatedPartitionSize = snapshotMetaInfo.getEstimatedPartitionSize();

      registeredShuffle.addAll(snapshotMetaInfo.getRegisteredShuffleList());
      hostnameSet.addAll(snapshotMetaInfo.getHostnameSetList());
      excludedWorkers.addAll(
          snapshotMetaInfo.getExcludedWorkersList().stream()
              .map(PbSerDeUtils::fromPbWorkerInfo)
              .collect(Collectors.toSet()));
      workerLostEvents.addAll(
          snapshotMetaInfo.getWorkerLostEventsList().stream()
              .map(PbSerDeUtils::fromPbWorkerInfo)
              .collect(Collectors.toSet()));
      appHeartbeatTime.putAll(snapshotMetaInfo.getAppHeartbeatTimeMap());

      registeredShuffle.forEach(
          shuffleKey -> {
            String appId = shuffleKey.split("-")[0];
            if (!appHeartbeatTime.containsKey(appId)) {
              appHeartbeatTime.put(appId, System.currentTimeMillis());
            }
          });

      workers.addAll(
          snapshotMetaInfo.getWorkersList().stream()
              .map(PbSerDeUtils::fromPbWorkerInfo)
              .collect(Collectors.toSet())
              .stream()
              .map(
                  workerInfo -> {
                    // Reset worker's network location with current master's configuration.
                    workerInfo.networkLocation_$eq(
                        rackResolver.resolve(workerInfo.host()).getNetworkLocation());
                    return workerInfo;
                  })
              .collect(Collectors.toSet()));

      snapshotMetaInfo
          .getLostWorkersMap()
          .entrySet()
          .forEach(
              entry -> lostWorkers.put(WorkerInfo.fromUniqueId(entry.getKey()), entry.getValue()));

      shutdownWorkers.addAll(
          snapshotMetaInfo.getShutdownWorkersList().stream()
              .map(PbSerDeUtils::fromPbWorkerInfo)
              .collect(Collectors.toSet()));

      partitionTotalWritten.reset();
      partitionTotalWritten.add(snapshotMetaInfo.getPartitionTotalWritten());
      partitionTotalFileCount.reset();
      partitionTotalFileCount.add(snapshotMetaInfo.getPartitionTotalFileCount());
      appDiskUsageMetric.restoreFromSnapshot(
          snapshotMetaInfo.getAppDiskUsageMetricSnapshotsList().stream()
              .map(PbSerDeUtils::fromPbAppDiskUsageSnapshot)
              .toArray(AppDiskUsageSnapShot[]::new));
      appDiskUsageMetric.currentSnapShot_$eq(
          new AtomicReference<AppDiskUsageSnapShot>(
              PbSerDeUtils.fromPbAppDiskUsageSnapshot(
                  snapshotMetaInfo.getCurrentAppDiskUsageMetricsSnapshot())));
    } catch (Exception e) {
      throw new IOException(e);
    }
    LOG.info("Successfully restore meta info from snapshot {}", file.getAbsolutePath());
    LOG.info(
        "Worker size: {}, Registered shuffle size: {}. Worker excluded list size: {}.",
        workers.size(),
        registeredShuffle.size(),
        excludedWorkers.size());
    workers.forEach(workerInfo -> LOG.info(workerInfo.toString()));
    registeredShuffle.forEach(shuffle -> LOG.info("RegisteredShuffle {}", shuffle));
  }

  public void updateMetaByReportWorkerUnavailable(List<WorkerInfo> failedWorkers) {
    synchronized (this.workers) {
      shutdownWorkers.addAll(failedWorkers);
    }
  }

  public void updatePartitionSize() {
    long oldEstimatedPartitionSize = estimatedPartitionSize;
    long tmpTotalWritten = partitionTotalWritten.sumThenReset();
    long tmpFileCount = partitionTotalFileCount.sumThenReset();
    LOG.debug(
        "update partition size total written {}, file count {}",
        Utils.bytesToString(tmpTotalWritten),
        tmpFileCount);
    if (tmpFileCount != 0) {
      estimatedPartitionSize = tmpTotalWritten / tmpFileCount;
    } else {
      estimatedPartitionSize = initialEstimatedPartitionSize;
    }
    LOG.warn(
        "Celeborn cluster estimated partition size changed from {} to {}",
        Utils.bytesToString(oldEstimatedPartitionSize),
        Utils.bytesToString(estimatedPartitionSize));
    workers.stream()
        .filter(worker -> !excludedWorkers.contains(worker))
        .forEach(workerInfo -> workerInfo.updateDiskMaxSlots(estimatedPartitionSize));
  }
}
