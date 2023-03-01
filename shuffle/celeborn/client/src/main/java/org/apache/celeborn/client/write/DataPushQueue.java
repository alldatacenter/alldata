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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
  private final int maxInFlight;

  private final String appId;
  private final int shuffleId;
  private final int numMappers;
  private final int numPartitions;
  private final ShuffleClient client;
  private final Set<String> reachLimitWorker = new HashSet<>();

  public DataPushQueue(
      CelebornConf conf,
      DataPusher dataPusher,
      ShuffleClient client,
      String appId,
      int shuffleId,
      int mapId,
      int attemptId,
      int numMappers,
      int numPartitions) {
    this.appId = appId;
    this.shuffleId = shuffleId;
    this.numMappers = numMappers;
    this.numPartitions = numPartitions;
    this.client = client;
    this.dataPusher = dataPusher;
    final String mapKey = Utils.makeMapKey(shuffleId, mapId, attemptId);
    this.pushState = client.getPushState(mapKey);
    this.maxInFlight = conf.pushMaxReqsInFlight();
    final int capacity = conf.pushQueueCapacity();
    workingQueue = new LinkedBlockingQueue<>(capacity);
  }

  /*
   * Now, `takePushTask` is only used by one thread,
   * so it is not thread-safe.
   * */
  public PushTask takePushTask() throws IOException {
    while (dataPusher.stillRunning()) {
      reachLimitWorker.clear();
      Iterator<PushTask> iterator = workingQueue.iterator();
      while (iterator.hasNext()) {
        PushTask task = iterator.next();
        int partitionId = task.getPartitionId();
        Map<Integer, PartitionLocation> partitionLocationMap =
            client.getPartitionLocation(appId, shuffleId, numMappers, numPartitions);
        if (partitionLocationMap != null) {
          PartitionLocation loc = partitionLocationMap.get(partitionId);
          if (!reachLimitWorker.contains(loc.hostAndPushPort())) {
            boolean reachLimit = pushState.reachLimit(loc.hostAndPushPort(), maxInFlight);
            if (!reachLimit) {
              iterator.remove();
              return task;
            } else {
              reachLimitWorker.add(loc.hostAndPushPort());
            }
          }
        } else {
          return task;
        }
      }
    }
    return null;
  }

  public boolean addPushTask(PushTask pushTask) throws InterruptedException {
    return workingQueue.offer(pushTask, WAIT_TIME_NANOS, TimeUnit.NANOSECONDS);
  }

  public void clear() {
    workingQueue.clear();
  }
}
