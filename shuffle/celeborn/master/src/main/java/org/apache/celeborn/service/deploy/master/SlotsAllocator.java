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

package org.apache.celeborn.service.deploy.master;

import java.util.*;

import scala.Tuple2;

import org.apache.commons.lang3.StringUtils;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.meta.DiskInfo;
import org.apache.celeborn.common.meta.DiskStatus;
import org.apache.celeborn.common.meta.WorkerInfo;
import org.apache.celeborn.common.protocol.PartitionLocation;
import org.apache.celeborn.common.protocol.StorageInfo;

public class SlotsAllocator {
  static class UsableDiskInfo {
    DiskInfo diskInfo;
    long usableSlots;

    UsableDiskInfo(DiskInfo diskInfo, long usableSlots) {
      this.diskInfo = diskInfo;
      this.usableSlots = usableSlots;
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(SlotsAllocator.class);
  private static final Random rand = new Random();
  private static boolean initialized = false;
  private static double[] taskAllocationRatio = null;

  public static Map<WorkerInfo, Tuple2<List<PartitionLocation>, List<PartitionLocation>>>
      offerSlotsRoundRobin(
          List<WorkerInfo> workers, List<Integer> partitionIds, boolean shouldReplicate) {
    if (partitionIds.isEmpty()) {
      return new HashMap<>();
    }
    if (workers.size() < 2 && shouldReplicate) {
      return new HashMap<>();
    }
    Map<WorkerInfo, Tuple2<List<PartitionLocation>, List<PartitionLocation>>> slots =
        new HashMap<>();
    Map<WorkerInfo, List<UsableDiskInfo>> restrictions = new HashMap<>();
    for (WorkerInfo worker : workers) {
      List<UsableDiskInfo> usableDisks =
          restrictions.computeIfAbsent(worker, v -> new ArrayList<>());
      for (Map.Entry<String, DiskInfo> diskInfoEntry : worker.diskInfos().entrySet()) {
        if (diskInfoEntry.getValue().status().equals(DiskStatus.HEALTHY)) {
          usableDisks.add(
              new UsableDiskInfo(
                  diskInfoEntry.getValue(), diskInfoEntry.getValue().availableSlots()));
        }
      }
    }
    List<Integer> remain = roundRobin(slots, partitionIds, workers, restrictions, shouldReplicate);
    if (!remain.isEmpty()) {
      roundRobin(slots, remain, workers, null, shouldReplicate);
    }
    return slots;
  }

  /**
   * It assumes that all disks whose available space is greater than the minimum space are divided
   * into multiple groups. A faster group will allocate more allocations than a slower group by
   * diskGroupGradient.
   */
  public static Map<WorkerInfo, Tuple2<List<PartitionLocation>, List<PartitionLocation>>>
      offerSlotsLoadAware(
          List<WorkerInfo> workers,
          List<Integer> partitionIds,
          boolean shouldReplicate,
          long minimumUsableSize,
          int diskGroupCount,
          double diskGroupGradient) {
    if (partitionIds.isEmpty()) {
      return new HashMap<>();
    }
    if (workers.size() < 2 && shouldReplicate) {
      return new HashMap<>();
    }

    List<DiskInfo> usableDisks = new ArrayList<>();
    Map<DiskInfo, WorkerInfo> diskToWorkerMap = new HashMap<>();

    workers.forEach(
        i ->
            i.diskInfos()
                .forEach(
                    (key, diskInfo) -> {
                      diskToWorkerMap.put(diskInfo, i);
                      if (diskInfo.actualUsableSpace() > minimumUsableSize
                          && diskInfo.status().equals(DiskStatus.HEALTHY)) {
                        usableDisks.add(diskInfo);
                      }
                    }));

    Set<WorkerInfo> usableWorkers = new HashSet<>();
    for (DiskInfo disk : usableDisks) {
      usableWorkers.add(diskToWorkerMap.get(disk));
    }
    if ((shouldReplicate && usableWorkers.size() <= 1) || usableDisks.isEmpty()) {
      logger.warn(
          "offer slots for {} fallback to roundrobin because there is no usable disks",
          StringUtils.join(partitionIds, ','));
      return offerSlotsRoundRobin(workers, partitionIds, shouldReplicate);
    }

    if (!initialized) {
      initLoadAwareAlgorithm(diskGroupCount, diskGroupGradient);
    }

    Map<WorkerInfo, List<UsableDiskInfo>> restriction =
        getRestriction(
            placeDisksToGroups(usableDisks, diskGroupCount),
            diskToWorkerMap,
            shouldReplicate ? partitionIds.size() * 2 : partitionIds.size());

    Map<WorkerInfo, Tuple2<List<PartitionLocation>, List<PartitionLocation>>> slots =
        new HashMap<>();
    List<Integer> remainPartitions =
        roundRobin(
            slots,
            partitionIds,
            new ArrayList<>(restriction.keySet()),
            restriction,
            shouldReplicate);
    if (!remainPartitions.isEmpty()) {
      roundRobin(slots, remainPartitions, new ArrayList<>(workers), null, shouldReplicate);
    }

    return slots;
  }

  private static StorageInfo getStorageInfo(
      List<WorkerInfo> workers,
      int workerIndex,
      Map<WorkerInfo, List<UsableDiskInfo>> restrictions,
      Map<WorkerInfo, Integer> workerDiskIndex) {
    WorkerInfo selectedWorker = workers.get(workerIndex);
    List<UsableDiskInfo> usableDiskInfos = restrictions.get(selectedWorker);
    int diskIndex = workerDiskIndex.computeIfAbsent(selectedWorker, v -> 0);
    while (usableDiskInfos.get(diskIndex).usableSlots <= 0) {
      diskIndex = (diskIndex + 1) % usableDiskInfos.size();
    }
    usableDiskInfos.get(diskIndex).usableSlots--;
    StorageInfo storageInfo = new StorageInfo(usableDiskInfos.get(diskIndex).diskInfo.mountPoint());
    workerDiskIndex.put(selectedWorker, (diskIndex + 1) % usableDiskInfos.size());
    return storageInfo;
  }

  private static List<Integer> roundRobin(
      Map<WorkerInfo, Tuple2<List<PartitionLocation>, List<PartitionLocation>>> slots,
      List<Integer> partitionIds,
      List<WorkerInfo> workers,
      Map<WorkerInfo, List<UsableDiskInfo>> restrictions,
      boolean shouldReplicate) {
    // workerInfo -> (diskIndexForMaster, diskIndexForSlave)
    Map<WorkerInfo, Integer> workerDiskIndexForMaster = new HashMap<>();
    Map<WorkerInfo, Integer> workerDiskIndexForSlave = new HashMap<>();
    List<Integer> partitionIdList = new ArrayList<>(partitionIds);
    int masterIndex = rand.nextInt(workers.size());
    Iterator<Integer> iter = partitionIdList.iterator();
    outer:
    while (iter.hasNext()) {
      int nextMasterInd = masterIndex;

      int partitionId = iter.next();
      StorageInfo storageInfo = new StorageInfo();
      if (restrictions != null) {
        while (restrictions.get(workers.get(nextMasterInd)).stream()
                .mapToLong(i -> i.usableSlots)
                .sum()
            <= 0) {
          nextMasterInd = (nextMasterInd + 1) % workers.size();
          if (nextMasterInd == masterIndex) {
            break outer;
          }
        }
        storageInfo =
            getStorageInfo(workers, nextMasterInd, restrictions, workerDiskIndexForMaster);
      }
      PartitionLocation masterPartition =
          createLocation(partitionId, workers.get(nextMasterInd), null, storageInfo, true);

      if (shouldReplicate) {
        int nextSlaveInd = (nextMasterInd + 1) % workers.size();
        if (restrictions != null) {
          while (restrictions.get(workers.get(nextSlaveInd)).stream()
                  .mapToLong(i -> i.usableSlots)
                  .sum()
              <= 0) {
            nextSlaveInd = (nextSlaveInd + 1) % workers.size();
            if (nextSlaveInd == nextMasterInd) {
              break outer;
            }
          }
          storageInfo =
              getStorageInfo(workers, nextSlaveInd, restrictions, workerDiskIndexForSlave);
        }
        PartitionLocation slavePartition =
            createLocation(
                partitionId, workers.get(nextSlaveInd), masterPartition, storageInfo, false);
        masterPartition.setPeer(slavePartition);
        Tuple2<List<PartitionLocation>, List<PartitionLocation>> locations =
            slots.computeIfAbsent(
                workers.get(nextSlaveInd), v -> new Tuple2<>(new ArrayList<>(), new ArrayList<>()));
        locations._2.add(slavePartition);
      }

      Tuple2<List<PartitionLocation>, List<PartitionLocation>> locations =
          slots.computeIfAbsent(
              workers.get(nextMasterInd), v -> new Tuple2<>(new ArrayList<>(), new ArrayList<>()));
      locations._1.add(masterPartition);
      masterIndex = (nextMasterInd + 1) % workers.size();
      iter.remove();
    }
    return partitionIdList;
  }

  private static void initLoadAwareAlgorithm(int diskGroups, double diskGroupGradient) {
    taskAllocationRatio = new double[diskGroups];
    double totalAllocations = 0;

    for (int i = 0; i < diskGroups; i++) {
      totalAllocations += Math.pow(1 + diskGroupGradient, diskGroups - 1 - i);
    }
    for (int i = 0; i < diskGroups; i++) {
      taskAllocationRatio[i] =
          Math.pow(1 + diskGroupGradient, diskGroups - 1 - i) / totalAllocations;
    }
    logger.info(
        "load-aware offer slots algorithm init with taskAllocationRatio {}",
        StringUtils.join(taskAllocationRatio, ','));
    initialized = true;
  }

  private static List<List<DiskInfo>> placeDisksToGroups(
      List<DiskInfo> usableDisks, int diskGroupCount) {
    List<List<DiskInfo>> diskGroups = new ArrayList<>();
    usableDisks.sort((o1, o2) -> Math.toIntExact(o1.avgFlushTime() - o2.avgFlushTime()));
    int diskCount = usableDisks.size();
    int startIndex = 0;
    int groupSizeSize = (int) Math.ceil(usableDisks.size() / (double) diskGroupCount);
    for (int i = 0; i < diskGroupCount; i++) {
      List<DiskInfo> diskList = new ArrayList<>();
      if (startIndex >= usableDisks.size()) {
        continue;
      }
      if (startIndex + groupSizeSize <= diskCount) {
        diskList.addAll(usableDisks.subList(startIndex, startIndex + groupSizeSize));
        startIndex += groupSizeSize;
      } else {
        diskList.addAll(usableDisks.subList(startIndex, usableDisks.size()));
        startIndex = usableDisks.size();
      }
      diskGroups.add(diskList);
    }
    return diskGroups;
  }

  private static Map<WorkerInfo, List<UsableDiskInfo>> getRestriction(
      List<List<DiskInfo>> groups, Map<DiskInfo, WorkerInfo> diskWorkerMap, int partitionCnt) {
    int groupSize = groups.size();
    long[] groupAllocations = new long[groupSize];
    Map<WorkerInfo, List<UsableDiskInfo>> restrictions = new HashMap<>();
    long[] groupAvailableSlots = new long[groupSize];
    for (int i = 0; i < groupSize; i++) {
      for (DiskInfo disk : groups.get(i)) {
        groupAvailableSlots[i] += disk.availableSlots();
      }
    }
    double[] currentAllocation = new double[groupSize];
    double currentAllocationSum = 0;
    for (int i = 0; i < groupSize; i++) {
      if (!groups.get(i).isEmpty()) {
        currentAllocationSum += taskAllocationRatio[i];
      }
    }
    for (int i = 0; i < groupSize; i++) {
      if (!groups.get(i).isEmpty()) {
        currentAllocation[i] = taskAllocationRatio[i] / currentAllocationSum;
      }
    }
    long toNextGroup = 0;
    long left = partitionCnt;
    for (int i = 0; i < groupSize; i++) {
      if (left <= 0) {
        break;
      }
      long estimateAllocation = (int) Math.ceil(partitionCnt * currentAllocation[i]);
      if (estimateAllocation > left) {
        estimateAllocation = left;
      }
      if (estimateAllocation + toNextGroup > groupAvailableSlots[i]) {
        groupAllocations[i] = groupAvailableSlots[i];
        toNextGroup = estimateAllocation - groupAvailableSlots[i] + toNextGroup;
      } else {
        groupAllocations[i] = estimateAllocation + toNextGroup;
      }
      left -= groupAllocations[i];
    }

    long groupLeft = 0;
    for (int i = 0; i < groups.size(); i++) {
      int disksInsideGroup = groups.get(i).size();
      long groupRequired = groupAllocations[i] + groupLeft;
      for (DiskInfo disk : groups.get(i)) {
        if (groupRequired <= 0) {
          break;
        }
        List<UsableDiskInfo> diskAllocation =
            restrictions.computeIfAbsent(diskWorkerMap.get(disk), v -> new ArrayList<>());
        long allocated =
            (int) Math.ceil((groupAllocations[i] + groupLeft) / (double) disksInsideGroup);
        if (allocated > disk.availableSlots()) {
          allocated = disk.availableSlots();
        }
        if (allocated > groupRequired) {
          allocated = groupRequired;
        }
        diskAllocation.add(new UsableDiskInfo(disk, Math.toIntExact(allocated)));
        groupRequired -= allocated;
      }
      groupLeft = groupRequired;
    }

    if (logger.isDebugEnabled()) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < groups.size(); i++) {
        sb.append("| group ").append(i).append(" ");
        for (DiskInfo diskInfo : groups.get(i)) {
          WorkerInfo workerInfo = diskWorkerMap.get(diskInfo);
          String workerHost = workerInfo.host();
          long allocation = 0;
          if (restrictions.get(workerInfo) != null) {
            for (UsableDiskInfo usableInfo : restrictions.get(workerInfo)) {
              if (usableInfo.diskInfo.equals(diskInfo)) {
                allocation = usableInfo.usableSlots;
              }
            }
          }
          sb.append(workerHost)
              .append("-")
              .append(diskInfo.mountPoint())
              .append(" flushtime:")
              .append(diskInfo.avgFlushTime())
              .append(" allocation: ")
              .append(allocation)
              .append(" ");
        }
        sb.append(" | ");
      }
      logger.debug(
          "total {} allocate with group {} with allocations {}",
          partitionCnt,
          StringUtils.join(groupAllocations, ','),
          sb);
    }
    return restrictions;
  }

  private static PartitionLocation createLocation(
      int partitionIndex,
      WorkerInfo workerInfo,
      PartitionLocation peer,
      StorageInfo storageInfo,
      boolean isMaster) {
    return new PartitionLocation(
        partitionIndex,
        0,
        workerInfo.host(),
        workerInfo.rpcPort(),
        workerInfo.pushPort(),
        workerInfo.fetchPort(),
        workerInfo.replicatePort(),
        isMaster ? PartitionLocation.Mode.MASTER : PartitionLocation.Mode.SLAVE,
        peer,
        storageInfo,
        new RoaringBitmap());
  }

  public static Map<WorkerInfo, Map<String, Integer>> slotsToDiskAllocations(
      Map<WorkerInfo, Tuple2<List<PartitionLocation>, List<PartitionLocation>>> slots) {
    Iterator<WorkerInfo> workers = slots.keySet().iterator();
    Map<WorkerInfo, Map<String, Integer>> workerToSlots = new HashMap<>();
    while (workers.hasNext()) {
      WorkerInfo worker = workers.next();
      Map<String, Integer> slotsPerDisk =
          workerToSlots.computeIfAbsent(worker, v -> new HashMap<>());
      List<PartitionLocation> jointLocations = new ArrayList<>();
      jointLocations.addAll(slots.get(worker)._1);
      jointLocations.addAll(slots.get(worker)._2);
      for (PartitionLocation location : jointLocations) {
        String mountPoint = location.getStorageInfo().getMountPoint();
        if (slotsPerDisk.containsKey(mountPoint)) {
          slotsPerDisk.put(mountPoint, slotsPerDisk.get(mountPoint) + 1);
        } else {
          slotsPerDisk.put(mountPoint, 1);
        }
      }
    }
    return workerToSlots;
  }
}
