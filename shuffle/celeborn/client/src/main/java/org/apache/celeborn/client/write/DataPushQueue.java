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

package org.apache.celeborn.client.write;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.client.ShuffleClient;
import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.protocol.PartitionLocation;
import org.apache.celeborn.common.util.Utils;
import org.apache.celeborn.common.write.PushState;

/*
 * Queue for push data,
 * it can take one PushTask whose worker inflight request is not reach limit,
 * and it can add one PushTask.
 *
 * */
public class DataPushQueue {
  private static final Logger logger = LoggerFactory.getLogger(DataPushQueue.class);

  private final long WAIT_TIME_NANOS = TimeUnit.MILLISECONDS.toNanos(500);

  private final LinkedBlockingQueue<PushTask> workingQueue;
  private final PushState pushState;
  private final DataPusher dataPusher;
  private final int maxInFlightPerWorker;
  private final int shuffleId;
  private final int numMappers;
  private final int numPartitions;
  private final ShuffleClient client;
  private final long takeTaskWaitIntervalMs;
  private final int takeTaskMaxWaitAttempts;

  public DataPushQueue(
      CelebornConf conf,
      DataPusher dataPusher,
      ShuffleClient client,
      int shuffleId,
      int mapId,
      int attemptId,
      int numMappers,
      int numPartitions) {
    this.shuffleId = shuffleId;
    this.numMappers = numMappers;
    this.numPartitions = numPartitions;
    this.client = client;
    this.dataPusher = dataPusher;
    final String mapKey = Utils.makeMapKey(shuffleId, mapId, attemptId);
    this.pushState = client.getPushState(mapKey);
    this.maxInFlightPerWorker = conf.clientPushMaxReqsInFlightPerWorker();
    this.takeTaskWaitIntervalMs = conf.clientPushTakeTaskWaitIntervalMs();
    this.takeTaskMaxWaitAttempts = conf.clientPushTakeTaskMaxWaitAttempts();
    final int capacity = conf.clientPushQueueCapacity();
    workingQueue = new LinkedBlockingQueue<>(capacity);
  }

  /*
   * Now, `takePushTasks` is only used by one thread,
   * so it is not thread-safe.
   * */
  public ArrayList<PushTask> takePushTasks() throws IOException, InterruptedException {
    ArrayList<PushTask> tasks = new ArrayList<>();
    HashMap<String, Integer> workerCapacity = new HashMap<>();
    HashMap<String, AtomicInteger> workerWaitAttempts = new HashMap<>();
    while (dataPusher.stillRunning()) {
      // clear() here is necessary since inflight pushes might change after sleeping
      // takeTaskWaitTimeMs
      // in last loop
      workerCapacity.clear();
      Iterator<PushTask> iterator = workingQueue.iterator();
      while (iterator.hasNext()) {
        PushTask task = iterator.next();
        int partitionId = task.getPartitionId();
        Map<Integer, PartitionLocation> partitionLocationMap =
            client.getPartitionLocation(shuffleId, numMappers, numPartitions);
        if (partitionLocationMap != null) {
          PartitionLocation loc = partitionLocationMap.get(partitionId);
          // According to CELEBORN-560, call rerun task and speculative task after LifecycleManager
          // handle StageEnd will return empty PartitionLocation map, here loc can be null
          if (loc != null) {
            Integer oldCapacity = workerCapacity.get(loc.hostAndPushPort());
            if (oldCapacity == null) {
              oldCapacity = maxInFlightPerWorker - pushState.inflightPushes(loc.hostAndPushPort());
              workerCapacity.put(loc.hostAndPushPort(), oldCapacity);
            }
            workerWaitAttempts.putIfAbsent(loc.hostAndPushPort(), new AtomicInteger(0));
            if (oldCapacity > 0) {
              iterator.remove();
              tasks.add(task);
              workerCapacity.put(loc.hostAndPushPort(), oldCapacity - 1);
            } else if (workerWaitAttempts.get(loc.hostAndPushPort()).get()
                >= takeTaskMaxWaitAttempts) {
              iterator.remove();
              tasks.add(task);
              // For such worker under high pressure, we only take one task each turn.
              workerWaitAttempts.get(loc.hostAndPushPort()).set(0);
            }
          } else {
            iterator.remove();
            tasks.add(task);
          }
        } else {
          iterator.remove();
          tasks.add(task);
        }
      }
      if (!tasks.isEmpty()) {
        return tasks;
      }
      try {
        // Reaching here means no available tasks can be pushed to any worker, wait for a while
        Thread.sleep(takeTaskWaitIntervalMs);
        workerWaitAttempts.values().forEach(AtomicInteger::incrementAndGet);
      } catch (InterruptedException ie) {
        logger.info("Thread interrupted while waiting push task.");
        throw ie;
      }
    }
    return tasks;
  }

  public boolean addPushTask(PushTask pushTask) throws InterruptedException {
    return workingQueue.offer(pushTask, WAIT_TIME_NANOS, TimeUnit.NANOSECONDS);
  }

  public void clear() {
    workingQueue.clear();
  }
}
