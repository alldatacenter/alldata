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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.exception.CelebornIOException;
import org.apache.celeborn.common.util.JavaUtils;

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
      JavaUtils.newConcurrentHashMap();

  private final int maxInFlightReqsTotal;
  private final LongAdder totalInflightReqs = new LongAdder();

  public InFlightRequestTracker(CelebornConf conf, PushState pushState) {
    this.waitInflightTimeoutMs = conf.clientPushLimitInFlightTimeoutMs();
    this.delta = conf.clientPushLimitInFlightSleepDeltaMs();
    this.pushState = pushState;
    this.pushStrategy = PushStrategy.getStrategy(conf);
    this.maxInFlightReqsTotal = conf.clientPushMaxReqsInFlightTotal();
  }

  public void addBatch(int batchId, String hostAndPushPort) {
    Set<Integer> batchIdSetPerPair =
        inflightBatchesPerAddress.computeIfAbsent(
            hostAndPushPort, id -> ConcurrentHashMap.newKeySet());
    batchIdSetPerPair.add(batchId);
    totalInflightReqs.increment();
  }

  public void removeBatch(int batchId, String hostAndPushPort) {
    Set<Integer> batchIdSet = inflightBatchesPerAddress.get(hostAndPushPort);
    // TODO: Need to debug why batchIdSet will be null.
    if (batchIdSet != null) {
      batchIdSet.remove(batchId);
    } else {
      logger.warn("BatchIdSet of {} is null.", hostAndPushPort);
    }
    totalInflightReqs.decrement();
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
        if (totalInflightReqs.sum() <= maxInFlightReqsTotal
            && batchIdSet.size() <= currentMaxReqsInFlight) {
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

    try {
      while (times > 0) {
        if (totalInflightReqs.sum() == 0) {
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
          "After waiting for {} ms, "
              + "there are still {} batches in flight "
              + "for hostAndPushPort {}, "
              + "which exceeds the current limit 0.",
          waitInflightTimeoutMs,
          totalInflightReqs.sum(),
          inflightBatchesPerAddress.keySet().stream().collect(Collectors.joining(", ", "[", "]")));
    }

    if (pushState.exception.get() != null) {
      throw pushState.exception.get();
    }

    return times <= 0;
  }

  protected int nextBatchId() {
    return batchId.incrementAndGet();
  }

  public void cleanup() {
    if (!inflightBatchesPerAddress.isEmpty()) {
      logger.warn("Clear {}", this.getClass().getSimpleName());
      inflightBatchesPerAddress.clear();
    }
    pushStrategy.clear();
  }
}
