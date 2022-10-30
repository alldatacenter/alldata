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

import java.util.Map;
import org.apache.inlong.tubemq.corebase.metric.Histogram;

/**
 * Simple Histogram Statistics, only include count, min, max value information.
 */
public class SimpleHistogram extends BaseMetric implements Histogram {
    private final LongStatsCounter count;
    private final LongMinGauge min;
    private final LongMaxGauge max;

    public SimpleHistogram(String metricName, String prefix) {
        super(metricName, prefix);
        this.count = new LongStatsCounter("count", getFullName());
        this.min = new LongMinGauge("min", getFullName());
        this.max = new LongMaxGauge("max", getFullName());
    }

    @Override
    public void update(long newValue) {
        this.count.incValue();
        this.min.update(newValue);
        this.max.update(newValue);
    }

    @Override
    public void getValue(Map<String, Long> keyValMap, boolean includeZero) {
        keyValMap.put(this.count.getFullName(), this.count.getValue());
        keyValMap.put(this.min.getFullName(), this.min.getValue());
        keyValMap.put(this.max.getFullName(), this.max.getValue());
    }

    @Override
    public void getValue(StringBuilder strBuff, boolean includeZero) {
        strBuff.append("\"").append(getFullName()).append("\":{\"")
                .append(this.count.getShortName()).append("\":")
                .append(this.count.getValue()).append(",\"")
                .append(this.min.getShortName()).append("\":")
                .append(this.min.getValue()).append(",\"")
                .append(this.max.getShortName()).append("\":")
                .append(this.max.getValue()).append("}");
    }

    @Override
    public void snapShort(Map<String, Long> keyValMap, boolean includeZero) {
        keyValMap.put(this.count.getFullName(), this.count.getAndResetValue());
        keyValMap.put(this.min.getFullName(), this.min.getAndResetValue());
        keyValMap.put(this.max.getFullName(), this.max.getAndResetValue());
    }

    @Override
    public void snapShort(StringBuilder strBuff, boolean includeZero) {
        strBuff.append("\"").append(getFullName()).append("\":{\"")
                .append(this.count.getShortName()).append("\":")
                .append(this.count.getAndResetValue()).append(",\"")
                .append(this.min.getShortName()).append("\":")
                .append(this.min.getAndResetValue()).append(",\"")
                .append(this.max.getShortName()).append("\":")
                .append(this.max.getAndResetValue()).append("}");
    }

    @Override
    public void clear() {
        this.count.clear();
        this.min.clear();
        this.max.clear();
    }
}
