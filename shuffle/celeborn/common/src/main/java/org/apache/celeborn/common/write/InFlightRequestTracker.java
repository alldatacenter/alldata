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

package org.apache.celeborn.common.write;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.exception.CelebornIOException;

/*
 * This class is for track in flight request and limit request.
 * */
public class InFlightRequestTracker {
  private static final Logger logger = LoggerFactory.getLogger(InFlightRequestTracker.class);

  private final long waitInflightTimeoutMs;
  private final long delta;
  private final PushState pushState;
  private final PushStrategy pushStrategy;

  private final AtomicInteger batchId = new AtomicInteger();
  private final ConcurrentHashMap<String, Set<Integer>> inflightBatchesPerAddress =
      new ConcurrentHashMap<>();

  public InFlightRequestTracker(CelebornConf conf, PushState pushState) {
    this.waitInflightTimeoutMs = conf.pushLimitInFlightTimeoutMs();
    this.delta = conf.pushLimitInFlightSleepDeltaMs();
    this.pushState = pushState;
    this.pushStrategy = PushStrategy.getStrategy(conf);
  }

  public void addBatch(int batchId, String hostAndPushPort) {
    Set<Integer> batchIdSetPerPair =
        inflightBatchesPerAddress.computeIfAbsent(
            hostAndPushPort, id -> ConcurrentHashMap.newKeySet());
    batchIdSetPerPair.add(batchId);
  }

  public void removeBatch(int batchId, String hostAndPushPort) {
    Set<Integer> batchIdSet = inflightBatchesPerAddress.get(hostAndPushPort);
    batchIdSet.remove(batchId);
  }

  public void onSuccess(String hostAndPushPort) {
    pushStrategy.onSuccess(hostAndPushPort);
  }

  public void onCongestControl(String hostAndPushPort) {
    pushStrategy.onCongestControl(hostAndPushPort);
  }

  public Set<Integer> getBatchIdSetByAddressPair(String hostAndPort) {
    return inflightBatchesPerAddress.computeIfAbsent(
        hostAndPort, pair -> ConcurrentHashMap.newKeySet());
  }

  public boolean limitMaxInFlight(String hostAndPushPort) throws IOException {
    if (pushState.exception.get() != null) {
      throw pushState.exception.get();
    }

    pushStrategy.limitPushSpeed(pushState, hostAndPushPort);
    int currentMaxReqsInFlight = pushStrategy.getCurrentMaxReqsInFlight(hostAndPushPort);

    Set batchIdSet = getBatchIdSetByAddressPair(hostAndPushPort);
    long times = waitInflightTimeoutMs / delta;
    try {
      while (times > 0) {
        if (batchIdSet.size() <= currentMaxReqsInFlight) {
          break;
        }
        if (pushState.exception.get() != null) {
          throw pushState.exception.get();
        }
        Thread.sleep(delta);
        times--;
      }
    } catch (InterruptedException e) {
      pushState.exception.set(new CelebornIOException(e));
    }

    if (times <= 0) {
      logger.warn(
          "After waiting for {} ms, "
              + "there are still {} batches in flight "
              + "for hostAndPushPort {}, "
              + "which exceeds the current limit {}.",
          waitInflightTimeoutMs,
          batchIdSet.size(),
          hostAndPushPort,
          currentMaxReqsInFlight);
    }

    if (pushState.exception.get() != null) {
      throw pushState.exception.get();
    }

    return times <= 0;
  }

  public boolean limitZeroInFlight() throws IOException {
    if (pushState.exception.get() != null) {
      throw pushState.exception.get();
    }
    long times = waitInflightTimeoutMs / delta;

    int inFlightSize = 0;
    try {
      while (times > 0) {
        Optional<Integer> inFlightSizeOptional =
            inflightBatchesPerAddress.values().stream().map(Set::size).reduce(Integer::sum);
        inFlightSize = inFlightSizeOptional.orElse(0);
        if (inFlightSize == 0) {
          break;
        }
        if (pushState.exception.get() != null) {
          throw pushState.exception.get();
        }
        Thread.sleep(delta);
        times--;
      }
    } catch (InterruptedException e) {
      pushState.exception.set(new CelebornIOException(e));
    }

    if (times <= 0) {
      logger.error(
          "After waiting for {} ms, there are still {} batches in flight, expect 0 batches",
          waitInflightTimeoutMs,
          inFlightSize);
    }

    if (pushState.exception.get() != null) {
      throw pushState.exception.get();
    }

    return times <= 0;
  }

  public boolean reachLimit(String hostAndPushPort, int maxInFlight) throws IOException {
    if (pushState.exception.get() != null) {
      throw pushState.exception.get();
    }

    Set<Integer> batchIdSet = getBatchIdSetByAddressPair(hostAndPushPort);
    return batchIdSet.size() > maxInFlight;
  }

  protected int nextBatchId() {
    return batchId.incrementAndGet();
  }

  public void cleanup() {
    if (!inflightBatchesPerAddress.isEmpty()) {
      inflightBatchesPerAddress.clear();
    }
    pushStrategy.clear();
  }
}
