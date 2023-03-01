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

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.identity.UserIdentifier;
import org.apache.celeborn.common.meta.AppDiskUsageMetric;
import org.apache.celeborn.common.meta.DiskInfo;
import org.apache.celeborn.common.meta.WorkerInfo;
import org.apache.celeborn.common.quota.ResourceConsumption;
import org.apache.celeborn.common.rpc.RpcEnv;

public class SingleMasterMetaManager extends AbstractMetaManager {
  private static final Logger LOG = LoggerFactory.getLogger(SingleMasterMetaManager.class);

  public SingleMasterMetaManager(RpcEnv rpcEnv, CelebornConf conf) {
    this.rpcEnv = rpcEnv;
    this.conf = conf;
    this.initialEstimatedPartitionSize = conf.initialEstimatedPartitionSize();
    this.estimatedPartitionSize = initialEstimatedPartitionSize;
    this.appDiskUsageMetric = new AppDiskUsageMetric(conf);
  }

  @Override
  public void handleRequestSlots(
      String shuffleKey,
      String hostName,
      Map<String, Map<String, Integer>> workerToAllocatedSlots,
      String requestId) {
    updateRequestSlotsMeta(shuffleKey, hostName, workerToAllocatedSlots);
  }

  @Override
  public void handleReleaseSlots(
      String shuffleKey,
      List<String> workerIds,
      List<Map<String, Integer>> slotStrings,
      String requestId) {
    updateReleaseSlotsMeta(shuffleKey, workerIds, slotStrings);
  }

  @Override
  public void handleUnRegisterShuffle(String shuffleKey, String requestId) {
    updateUnregisterShuffleMeta(shuffleKey);
  }

  @Override
  public void handleAppHeartbeat(
      String appId, long totalWritten, long fileCount, long time, String requestId) {
    updateAppHeartbeatMeta(appId, time, totalWritten, fileCount);
  }

  @Override
  public void handleAppLost(String appId, String requestId) {
    updateAppLostMeta(appId);
  }

  @Override
  public void handleWorkerLost(
      String host, int rpcPort, int pushPort, int fetchPort, int replicatePort, String requestId) {
    updateWorkerLostMeta(host, rpcPort, pushPort, fetchPort, replicatePort);
  }

  @Override
  public void handleWorkerRemove(
      String host, int rpcPort, int pushPort, int fetchPort, int replicatePort, String requestId) {
    updateWorkerRemoveMeta(host, rpcPort, pushPort, fetchPort, replicatePort);
  }

  @Override
  public void handleWorkerHeartbeat(
      String host,
      int rpcPort,
      int pushPort,
      int fetchPort,
      int replicatePort,
      Map<String, DiskInfo> disks,
      Map<UserIdentifier, ResourceConsumption> userResourceConsumption,
      Map<String, Long> estimatedAppDiskUsage,
      long time,
      String requestId) {
    updateWorkerHeartbeatMeta(
        host,
        rpcPort,
        pushPort,
        fetchPort,
        replicatePort,
        disks,
        userResourceConsumption,
        estimatedAppDiskUsage,
        time);
  }

  @Override
  public void handleRegisterWorker(
      String host,
      int rpcPort,
      int pushPort,
      int fetchPort,
      int replicatePort,
      Map<String, DiskInfo> disks,
      Map<UserIdentifier, ResourceConsumption> userResourceConsumption,
      String requestId) {
    updateRegisterWorkerMeta(
        host, rpcPort, pushPort, fetchPort, replicatePort, disks, userResourceConsumption);
  }

  @Override
  public void handleReportWorkerUnavailable(List<WorkerInfo> failedNodes, String requestId) {
    updateBlacklistByReportWorkerUnavailable(failedNodes);
  }

  public void handleUpdatePartitionSize() {
    updatePartitionSize();
  }
}
