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

import java.util.concurrent.atomic.LongAdder;
import org.apache.inlong.tubemq.corebase.metric.Counter;

/**
 * LongStatsCounter, store current value information.
 *
 * The metric used for statistics, value is stored by LongAdder type.
 */
public class LongStatsCounter extends BaseMetric implements Counter {
    // value counter
    private final LongAdder value = new LongAdder();

    public LongStatsCounter(String metricName, String prefix) {
        super(metricName, prefix);
    }

    @Override
    public void incValue() {
        this.value.increment();
    }

    @Override
    public void decValue() {
        this.value.decrement();
    }

    @Override
    public void addValue(long delta) {
        this.value.add(delta);
    }

    @Override
    public void clear() {
        this.value.reset();
    }

    @Override
    public long getValue() {
        return this.value.sum();
    }

    @Override
    public long getAndResetValue() {
        return this.value.sumThenReset();
    }
}
