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

package org.apache.inlong.tubemq.corebase.metric.impl;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.corebase.utils.DateTimeConvertUtils;

/**
 * SinceTime, store the start time of the metric items set
 *
 * After calling the snapshot() function, it need to be updated to the snapshot time.
 */
public class SinceTime extends BaseMetric {
    private final AtomicLong sinceTime = new AtomicLong();

    public SinceTime(String metricName, String prefix) {
        super(metricName, prefix);
        reset();
    }

    public long getSinceTime() {
        return this.sinceTime.get();
    }

    public String getStrSinceTime() {
        return DateTimeConvertUtils.ms2yyyyMMddHHmmss(this.sinceTime.get());
    }

    public long getAndResetSinceTime() {
        return this.sinceTime.getAndSet(System.currentTimeMillis());
    }

    public void reset() {
        this.sinceTime.set(System.currentTimeMillis());
    }

    public void reset(long resetTime) {
        this.sinceTime.set(resetTime);
    }

}
