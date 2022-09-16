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

package org.apache.inlong.tubemq.corerpc;

import java.util.concurrent.atomic.AtomicLong;

public class RemoteConErrStats {
    private long statisticDuration = 60000;
    private int maxConnAllowedFailCount = 5;
    private AtomicLong errCounter = new AtomicLong(0);
    private AtomicLong lastTimeStamp = new AtomicLong(0);

    public RemoteConErrStats(final long statisticDuration,
                             final int maxConnAllowedFailCount) {
        this.errCounter.set(0);
        this.lastTimeStamp.set(System.currentTimeMillis());
        this.statisticDuration = statisticDuration;
        this.maxConnAllowedFailCount = maxConnAllowedFailCount;
    }

    public void resetErrCount() {
        this.errCounter.set(0);
        this.lastTimeStamp.set(System.currentTimeMillis());
    }

    public boolean increErrCount() {
        long curLastTimeStamp = lastTimeStamp.get();
        if (System.currentTimeMillis() - curLastTimeStamp > this.statisticDuration) {
            if (lastTimeStamp.compareAndSet(curLastTimeStamp, System.currentTimeMillis())) {
                this.errCounter.set(0);
            }
        }
        boolean isForbidden = false;
        if (this.errCounter.incrementAndGet() > this.maxConnAllowedFailCount) {
            isForbidden = true;
        }
        return isForbidden;
    }

    public boolean isExpiredRecord(long currentTIme) {
        if (currentTIme - lastTimeStamp.get() > this.statisticDuration * 10) {
            return true;
        }
        return false;
    }
}

