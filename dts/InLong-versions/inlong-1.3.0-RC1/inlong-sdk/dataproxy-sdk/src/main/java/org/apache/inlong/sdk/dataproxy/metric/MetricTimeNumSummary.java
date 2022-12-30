/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.metric;

import java.util.concurrent.atomic.AtomicLong;

public class MetricTimeNumSummary {

    private final AtomicLong send10msBelow = new AtomicLong(0);

    private final AtomicLong sendBetween10msAnd100ms = new AtomicLong(0);

    private final AtomicLong sendBetween100msAnd500ms = new AtomicLong(0);

    private final AtomicLong sendBetween500msAnd1s = new AtomicLong(0);

    private final AtomicLong send1sAbove = new AtomicLong(0);

    private final AtomicLong successNum = new AtomicLong(0);
    private final AtomicLong failedNum = new AtomicLong(0);

    private final long startCalculateTime;

    public MetricTimeNumSummary(long startCalculateTime) {
        this.startCalculateTime = startCalculateTime;
    }

    /**
     * get summary time
     *
     * @return
     */
    public long getSummaryTime() {
        return System.currentTimeMillis() - this.startCalculateTime;
    }

    /**
     * record time
     * 1. [-, 10)
     * 2. [10, 100)
     * 3. [100, 500)
     * 4. [500, 1000)
     * 5. [100, -)
     *
     * @param sendTimeInMs - send time
     * @param sendNum      - send num
     */
    public void recordSuccessSendTime(long sendTimeInMs, int sendNum) {
        if (sendTimeInMs < 10) {
            send10msBelow.addAndGet(sendNum);
        } else if (sendTimeInMs < 100) {
            sendBetween10msAnd100ms.addAndGet(sendNum);
        } else if (sendTimeInMs < 500) {
            sendBetween100msAnd500ms.addAndGet(sendNum);
        } else if (sendTimeInMs < 1000) {
            sendBetween500msAnd1s.addAndGet(sendNum);
        } else {
            send1sAbove.addAndGet(sendNum);
        }
        increaseSuccessNum(sendNum);
    }

    public void increaseSuccessNum(int sendNum) {
        successNum.addAndGet(sendNum);
    }

    public void increaseFailedNum(int sendNum) {
        failedNum.addAndGet(sendNum);
    }

    public long getFailedNum() {
        return failedNum.get();
    }

    public long getSuccessNum() {
        return successNum.get();
    }

    public long getTotalNum() {
        return failedNum.get() + successNum.get();
    }

    public String getTimeString() {
        return send10msBelow.get() + "#" + sendBetween10msAnd100ms.get()
                + "#" + sendBetween100msAnd500ms.get() + "#"
                + sendBetween500msAnd1s.get() + "#" + send1sAbove.get();
    }

    public long getStartCalculateTime() {
        return startCalculateTime;
    }
}
