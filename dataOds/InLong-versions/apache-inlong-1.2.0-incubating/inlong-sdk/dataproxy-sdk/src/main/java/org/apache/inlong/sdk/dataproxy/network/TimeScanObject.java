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

package org.apache.inlong.sdk.dataproxy.network;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by jesseyzhou on 2017/11/2.
 */
public class TimeScanObject {

    private AtomicInteger count = new AtomicInteger(0);
    private AtomicLong time = new AtomicLong(0);

    public TimeScanObject() {
        this.count.set(0);
        this.time.set(System.currentTimeMillis());
    }

    public int incrementAndGet() {
        this.time.set(System.currentTimeMillis());
        return this.count.incrementAndGet();
    }

    public long getTime() {
        return time.get();
    }

    public void updateCountToZero() {
        long oldValue = this.time.get();
        int oldCnt = this.count.get();
        if (System.currentTimeMillis() > oldValue) {
            if (this.time.compareAndSet(oldValue, System.currentTimeMillis())) {
                this.count.compareAndSet(oldCnt, 0);
            }
        }
    }

    public int getCurTimeoutCount() {
        return this.count.get();
    }

}
