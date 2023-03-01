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

import static org.apache.celeborn.common.protocol.RpcNameConstants.WORKER_EP;

import java.io.*;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.common.meta.*;
import org.apache.celeborn.common.protocol.PbSnapshotMetaInfo;
import org.apache.celeborn.common.quota.ResourceConsumption;
import org.apache.celeborn.common.rpc.RpcAddress;
import org.apache.celeborn.common.rpc.RpcEnv;
import org.apache.celeborn.common.util.PbSerDeUtils;
import org.apache.celeborn.common.util.Utils;

public abstract class AbstractMetaManager implements IMetadataHandler {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractMetaManager.class);

  // Meta data for master service
  public final Set<String> registeredShuffle = ConcurrentHashMap.newKeySet();
  public final Set<String> hostnameSet = ConcurrentHashMap.newKeySet();
  public final ArrayList<WorkerInfo> workers = new ArrayList<>();
  public final ConcurrentHashMap<String, Long> appHeartbeatTime = new ConcurrentHashMap<>();
  // blacklist
  public final Set<WorkerInfo> blacklist = ConcurrentHashMap.newKeySet();
  // workerLost events
  public final Set<WorkerInfo> workerLostEvents = ConcurrentHashMap.newKeySet();

  protected RpcEnv rpcEnv;
  protected CelebornConf conf;

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
    if (!workerWithAllocations.isEmpty()) {
      synchronized (workers) {
        for (WorkerInfo workerInfo : workers) {
          String workerUniqueId = workerInfo.toUniqueId();
          if (workerWithAllocations.containsKey(workerUniqueId)) {
            workerInfo.allocateSlots(shuffleKey, workerWithAllocations.get(workerUniqueId));
          }
        }
      }
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
    registeredShuffle.stream()
        .filter(shuffle -> shuffle.startsWith(appId))
        .forEach(this::updateReleaseSlotsMeta);
    registeredShuffle.removeIf(shuffleKey -> shuffleKey.startsWith(appId));
    appHeartbeatTime.remove(appId);
  }

  public void updateWorkerLostMeta(
      String host, int rpcPort, int pushPort, int fetchPort, int replicatePort) {
    WorkerInfo worker = new WorkerInfo(host, rpcPort, pushPort, fetchPort, replicatePort, null);
    workerLostEvents.add(worker);
    // remove worker from workers
    synchronized (workers) {
      workers.remove(worker);
    }
    // delete from blacklist
    blacklist.remove(worker);
    workerLostEvents.remove(worker);
  }

  public void updateWorkerRemoveMeta(
      String host, int rpcPort, int pushPort, int fetchPort, int replicatePort) {
    WorkerInfo worker = new WorkerInfo(host, rpcPort, pushPort, fetchPort, replicatePort, null);
    // remove worker from workers
    synchronized (workers) {
      workers.remove(worker);
    }
    // delete from blacklist
    blacklist.remove(worker);
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
            host,
            rpcPort,
            pushPort,
            fetchPort,
            replicatePort,
            disks,
            userResourceConsumption,
            null);
    AtomicLong availableSlots = new AtomicLong();
    LOG.debug("update worker {}:{} heart beat {}", host, rpcPort, disks);
    synchronized (workers) {
      Optional<WorkerInfo> workerInfo = workers.stream().filter(w -> w.equals(worker)).findFirst();
      workerInfo.ifPresent(
          info -> {
            info.updateThenGetDiskInfos(disks, estimatedPartitionSize);
            info.updateThenGetUserResourceConsumption(userResourceConsumption);
            availableSlots.set(info.totalAvailableSlots());
            info.lastHeartbeat_$eq(time);
          });
    }
    appDiskUsageMetric.update(estimatedAppDiskUsage);
    if (!blacklist.contains(worker) && disks.isEmpty()) {
      LOG.debug("Worker: {} num total slots is 0, add to blacklist", worker);
      blacklist.add(worker);
    } else if (availableSlots.get() > 0) {
      // only unblack if numSlots larger than 0
      blacklist.remove(worker);
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
            host,
            rpcPort,
            pushPort,
            fetchPort,
            replicatePort,
            disks,
            userResourceConsumption,
            null);
    workerInfo.lastHeartbeat_$eq(System.currentTimeMillis());

    try {
      workerInfo.setupEndpoint(rpcEnv.setupEndpointRef(RpcAddress.apply(host, rpcPort), WORKER_EP));
    } catch (Exception e) {
      LOG.warn("Worker register setupEndpoint failed {}, will retry", e);
      try {
        workerInfo.setupEndpoint(
            rpcEnv.setupEndpointRef(RpcAddress.apply(host, rpcPort), WORKER_EP));
      } catch (Exception e1) {
        workerInfo.setupEndpoint(null);
      }
    }

    workerInfo.updateDiskMaxSlots(estimatedPartitionSize);
    synchronized (workers) {
      if (!workers.contains(workerInfo)) {
        workers.add(workerInfo);
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
                blacklist,
                workerLostEvents,
                appHeartbeatTime,
                workers,
                partitionTotalWritten.sum(),
                partitionTotalFileCount.sum(),
                appDiskUsageMetric.snapShots(),
                appDiskUsageMetric.currentSnapShot().get())
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
      blacklist.addAll(
          snapshotMetaInfo.getBlacklistList().stream()
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
        "Worker size: {}, Registered shuffle size: {}, Worker blacklist size: {}.",
        workers.size(),
        registeredShuffle.size(),
        blacklist.size());
    workers.forEach(workerInfo -> LOG.info(workerInfo.toString()));
    registeredShuffle.forEach(shuffle -> LOG.info("RegisteredShuffle {}", shuffle));
  }

  public void updateBlacklistByReportWorkerUnavailable(List<WorkerInfo> failedWorkers) {
    synchronized (this.workers) {
      failedWorkers.retainAll(this.workers);
      this.blacklist.addAll(failedWorkers);
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
        "Rss cluster estimated partition size changed from {} to {}",
        Utils.bytesToString(oldEstimatedPartitionSize),
        Utils.bytesToString(estimatedPartitionSize));
    workers.stream()
        .filter(worker -> !blacklist.contains(worker))
        .forEach(workerInfo -> workerInfo.updateDiskMaxSlots(estimatedPartitionSize));
  }
}
