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

package org.apache.celeborn.plugin.flink;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.google.common.collect.Sets;
import org.apache.flink.api.common.JobID;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.runtime.shuffle.JobShuffleContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import org.apache.celeborn.client.LifecycleManager;
import org.apache.celeborn.client.listener.WorkersStatus;
import org.apache.celeborn.common.meta.ShufflePartitionLocationInfo;
import org.apache.celeborn.common.meta.WorkerInfo;
import org.apache.celeborn.common.protocol.PartitionLocation;

public class ShuffleResourceTrackerTest {

  @Test
  public void testNotifyUnknownWorkers() {
    LifecycleManager lifecycleManager = Mockito.mock(LifecycleManager.class);
    ScheduledThreadPoolExecutor executor = Mockito.mock(ScheduledThreadPoolExecutor.class);

    ConcurrentHashMap<WorkerInfo, ShufflePartitionLocationInfo> map = new ConcurrentHashMap<>();
    WorkerInfo workerInfo = new WorkerInfo("mock", -1, -1, -1, -1);
    map.put(workerInfo, mockShufflePartitionLocationInfo());

    ConcurrentHashMap<WorkerInfo, ShufflePartitionLocationInfo> map2 = new ConcurrentHashMap<>();
    map2.put(workerInfo, mockShufflePartitionLocationInfo());

    ConcurrentHashMap<WorkerInfo, ShufflePartitionLocationInfo> map3 = new ConcurrentHashMap<>();
    map3.put(workerInfo, mockShufflePartitionLocationInfo());

    Mockito.when(lifecycleManager.workerSnapshots(Mockito.anyInt())).thenReturn(map, map2, map3);

    ShuffleResourceTracker shuffleResourceTracker =
        new ShuffleResourceTracker(executor, lifecycleManager);

    JobID jobID1 = new JobID();
    shuffleResourceTracker.registerJob(createJobShuffleContext(jobID1));
    shuffleResourceTracker.addPartitionResource(jobID1, 1, 1, new ResultPartitionID());
    shuffleResourceTracker.addPartitionResource(jobID1, 1, 2, new ResultPartitionID());
    shuffleResourceTracker.addPartitionResource(jobID1, 1, 3, new ResultPartitionID());
    shuffleResourceTracker.addPartitionResource(jobID1, 2, 3, new ResultPartitionID());

    JobID jobID2 = new JobID();
    shuffleResourceTracker.registerJob(createJobShuffleContext(jobID2));
    shuffleResourceTracker.addPartitionResource(jobID2, 3, 1, new ResultPartitionID());

    List<WorkerInfo> workerInfoList = new ArrayList<>();
    workerInfoList.add(workerInfo);
    shuffleResourceTracker.notifyChangedWorkersStatus(new WorkersStatus(workerInfoList, null));

    Assert.assertEquals(
        Sets.newHashSet(3),
        shuffleResourceTracker
            .getJobResourceListener(jobID1)
            .getResultPartitionMap()
            .get(2)
            .keySet());
    Assert.assertEquals(
        Sets.newHashSet(3),
        shuffleResourceTracker
            .getJobResourceListener(jobID1)
            .getResultPartitionMap()
            .get(2)
            .keySet());

    Assert.assertTrue(
        shuffleResourceTracker
            .getJobResourceListener(jobID2)
            .getResultPartitionMap()
            .get(3)
            .isEmpty());
  }

  public ShufflePartitionLocationInfo mockShufflePartitionLocationInfo() {
    ShufflePartitionLocationInfo shufflePartitionLocationInfo = new ShufflePartitionLocationInfo();

    List<PartitionLocation> primaryLocations = new ArrayList<>();
    primaryLocations.add(mockShufflePartitionLocationInfo(1));
    primaryLocations.add(mockShufflePartitionLocationInfo(2));

    List<PartitionLocation> replicaLocations = new ArrayList<>();
    replicaLocations.add(mockShufflePartitionLocationInfo(3));
    replicaLocations.add(mockShufflePartitionLocationInfo(4));

    shufflePartitionLocationInfo.addPrimaryPartitions(primaryLocations);
    shufflePartitionLocationInfo.addReplicaPartitions(replicaLocations);
    return shufflePartitionLocationInfo;
  }

  public JobShuffleContext createJobShuffleContext(JobID jobId) {
    return new JobShuffleContext() {
      @Override
      public JobID getJobId() {
        return jobId;
      }

      @Override
      public CompletableFuture<?> stopTrackingAndReleasePartitions(
          Collection<ResultPartitionID> collection) {
        return CompletableFuture.completedFuture(null);
      }
    };
  }

  private PartitionLocation mockShufflePartitionLocationInfo(int partitionId) {
    return new PartitionLocation(
        partitionId, -1, "mock", -1, -1, -1, -1, PartitionLocation.Mode.PRIMARY);
  }
}
