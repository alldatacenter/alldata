/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.common.utils;

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.inlong.tubemq.server.Stoppable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sleeper for current thread. Sleeps for passed period. Also checks passed boolean and if
 * interrupted, will return if the flag is set (rather than go back to sleep until its sleep time is
 * up).
 * Copied from <a href="http://hbase.apache.org">Apache HBase Project</a>
 */
public class Sleeper {
    private static final long MINIMAL_DELTA_FOR_LOGGING = 10000;
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private final int period;
    private final Stoppable stopper;
    private final Object sleepLock = new Object();
    private final AtomicBoolean triggerWake = new AtomicBoolean(false);

    /**
     * Initial a Sleeper object
     *
     * @param sleep   sleep time in milliseconds
     * @param stopper When {@link Stoppable#isStopped()} is true, this thread will cleanup and exit
     *                cleanly.
     */
    public Sleeper(final int sleep, final Stoppable stopper) {
        this.period = sleep;
        this.stopper = stopper;
    }

    /**
     * If currently asleep, stops sleeping; if not asleep, will skip the next sleep cycle.
     */
    public void skipSleepCycle() {
        if (triggerWake.compareAndSet(false, true)) {
            synchronized (sleepLock) {
                sleepLock.notifyAll();
            }
        }
    }

    /**
     * Sleep for period.
     */
    public void sleep() {
        sleep(System.currentTimeMillis());
    }

    /**
     * Sleep for period adjusted by passed startTime
     *
     * @param startTime Time some task started previous to now.
     */
    public void sleep(final long startTime) {
        if (this.stopper.isStopped()) {
            return;
        }
        long now = System.currentTimeMillis();
        long waitTime = this.period - (now - startTime);
        if (waitTime > this.period) {
            logger.warn("Calculated wait time > " + this.period + "; setting to this.period: "
                    + System.currentTimeMillis() + ", " + startTime);
            waitTime = this.period;
        }
        while (waitTime > 0) {
            long woke = -1;
            try {
                if (triggerWake.get()) {
                    break;
                }
                synchronized (sleepLock) {
                    sleepLock.wait(waitTime);
                }
                woke = System.currentTimeMillis();
                long slept = woke - now;
                if (slept - this.period > MINIMAL_DELTA_FOR_LOGGING) {
                    logger.warn("We slept " + slept + "ms instead of " + this.period
                            + "ms, this is likely due to a long "
                            + "garbage collecting pause and it's usually bad!");
                }
            } catch (InterruptedException iex) {
                // We we interrupted because we're meant to close? If not, just
                // continue ignoring the interruption
                if (this.stopper.isStopped()) {
                    return;
                }
            }
            // Recalculate waitTime.
            woke = (woke == -1) ? System.currentTimeMillis() : woke;
            waitTime = this.period - (woke - startTime);
        }
        triggerWake.compareAndSet(true, false);
    }
}
