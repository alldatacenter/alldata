/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.yarn.appMaster;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Clock driver that calls a callback once each pulse period. Used to react to
 * time-based events such as timeouts, checking for changed files, etc.
 * This is called a "pulse" because it is periodic, like your pulse. But,
 * unlike the "heartbeat" between the AM and YARN or the AM and ZK,
 * this is purely internal.
 */

public class PulseRunnable implements Runnable {
  private static final Log LOG = LogFactory.getLog(PulseRunnable.class);

  /**
   * Interface implemented to receive calls on each clock "tick."
   */

  public interface PulseCallback {
    void onTick(long curTime);
  }

  private final int pulsePeriod;
  private final PulseRunnable.PulseCallback callback;
  public AtomicBoolean isLive = new AtomicBoolean(true);

  public PulseRunnable(int pulsePeriodMS,
      PulseRunnable.PulseCallback callback) {
    pulsePeriod = pulsePeriodMS;
    this.callback = callback;
  }

  @Override
  public void run() {
    while (isLive.get()) {
      try {
        Thread.sleep(pulsePeriod);
      } catch (InterruptedException e) {
        break;
      }
      try {
        callback.onTick(System.currentTimeMillis());
      } catch (Exception e) {

        // Ignore exceptions. Seems strange, but is required to allow
        // graceful shutdown of the AM when errors occur. For example, we
        // start tasks on tick events. If those tasks fail, the timer
        // goes down. But, the timer is also needed to time out failed
        // requests in order to bring down the AM. So, just log the error
        // and soldier on.

        LOG.error("Timer thread caught, ignored an exception", e);
      }
    }
  }

  public void stop() { isLive.set(false); }
}