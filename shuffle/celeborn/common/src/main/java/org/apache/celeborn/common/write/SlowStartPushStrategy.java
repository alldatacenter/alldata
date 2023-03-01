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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.exception.CelebornIOException;

public class SlowStartPushStrategy extends PushStrategy {

  protected static class CongestControlContext {
    private final AtomicInteger currentMaxReqsInFlight;

    // Indicate the number of congested times even after the in flight requests reduced to 1
    private final AtomicInteger continueCongestedNumber;
    private int congestionAvoidanceFlag;
    private int reqsInFlightBlockThreshold;

    public CongestControlContext(int reqsInFlightBlockThreshold) {
      this.currentMaxReqsInFlight = new AtomicInteger(1);
      this.continueCongestedNumber = new AtomicInteger(0);
      this.congestionAvoidanceFlag = 0;
      this.reqsInFlightBlockThreshold = reqsInFlightBlockThreshold;
    }

    public synchronized void increaseCurrentMaxReqs() {
      continueCongestedNumber.set(0);
      if (currentMaxReqsInFlight.get() >= reqsInFlightBlockThreshold) {
        // Congestion avoidance
        congestionAvoidanceFlag++;
        if (congestionAvoidanceFlag >= currentMaxReqsInFlight.get()) {
          currentMaxReqsInFlight.incrementAndGet();
          congestionAvoidanceFlag = 0;
        }
      } else {
        // Slow start
        currentMaxReqsInFlight.incrementAndGet();
      }
    }

    public synchronized void decreaseCurrentMaxReqs() {
      if (currentMaxReqsInFlight.get() <= 1) {
        currentMaxReqsInFlight.set(1);
        continueCongestedNumber.incrementAndGet();
      } else {
        currentMaxReqsInFlight.updateAndGet(pre -> pre / 2);
      }
      reqsInFlightBlockThreshold = currentMaxReqsInFlight.get();
      congestionAvoidanceFlag = 0;
    }

    public int getCurrentMaxReqsInFlight() {
      return currentMaxReqsInFlight.get();
    }

    public int getContinueCongestedNumber() {
      return continueCongestedNumber.get();
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(SlowStartPushStrategy.class);

  private final int maxInFlight;
  private final long initialSleepMills;
  private final long maxSleepMills;
  private final ConcurrentHashMap<String, CongestControlContext> congestControlInfoPerAddress;

  public SlowStartPushStrategy(CelebornConf conf) {
    super(conf);
    this.maxInFlight = conf.pushMaxReqsInFlight();
    this.initialSleepMills = conf.pushSlowStartInitialSleepTime();
    this.maxSleepMills = conf.pushSlowStartMaxSleepMills();
    this.congestControlInfoPerAddress = new ConcurrentHashMap<>();
  }

  @VisibleForTesting
  protected CongestControlContext getCongestControlContextByAddress(String hostAndPushPort) {
    return congestControlInfoPerAddress.computeIfAbsent(
        hostAndPushPort, host -> new CongestControlContext(maxInFlight));
  }

  /**
   * If `pushDataSlowStart` is enabled, will increase `currentMaxReqsInFlight` gradually to meet the
   * max push speed.
   *
   * <p>1. slow start period: every RTT period, `currentMaxReqsInFlight` is doubled.
   *
   * <p>2. congestion avoidance: every RTT period, `currentMaxReqsInFlight` plus 1.
   *
   * <p>Note that here we define one RTT period: one batch(currentMaxReqsInFlight) of push data
   * requests.
   */
  @Override
  public void onSuccess(String hostAndPushPort) {
    CongestControlContext congestControlContext =
        getCongestControlContextByAddress(hostAndPushPort);
    congestControlContext.increaseCurrentMaxReqs();
  }

  @Override
  public void onCongestControl(String hostAndPushPort) {
    CongestControlContext congestControlContext =
        getCongestControlContextByAddress(hostAndPushPort);
    congestControlContext.decreaseCurrentMaxReqs();
  }

  protected long getSleepTime(CongestControlContext context) {
    int currentMaxReqs = context.getCurrentMaxReqsInFlight();
    if (currentMaxReqs >= conf.pushMaxReqsInFlight()) {
      return 0;
    }

    long sleepInterval = initialSleepMills - 60L * currentMaxReqs;

    if (currentMaxReqs == 1) {
      return Math.min(sleepInterval + context.getContinueCongestedNumber() * 1000L, maxSleepMills);
    }

    return Math.max(sleepInterval, 0);
  }

  @Override
  public void limitPushSpeed(PushState pushState, String hostAndPushPort) throws IOException {
    if (pushState.exception.get() != null) {
      throw pushState.exception.get();
    }
    CongestControlContext congestControlContext =
        getCongestControlContextByAddress(hostAndPushPort);
    long sleepInterval = getSleepTime(congestControlContext);
    if (sleepInterval > 0L) {
      try {
        logger.debug("Will sleep {} ms to control the push speed.", sleepInterval);
        Thread.sleep(sleepInterval);
      } catch (InterruptedException e) {
        pushState.exception.set(new CelebornIOException(e));
      }
    }
  }

  @Override
  public int getCurrentMaxReqsInFlight(String hostAndPushPort) {
    return getCongestControlContextByAddress(hostAndPushPort).getCurrentMaxReqsInFlight();
  }

  @Override
  public void clear() {
    congestControlInfoPerAddress.clear();
  }
}
