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
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.metric.Histogram;

/**
 * ESTHistogram, Exponential Statistics for Time
 *
 *  This class performs the index statistics of the time period with the exponential power of 2.
 *  Here we count up to the 17th power of 2, a total of 18 buckets, according to the input data and
 *  the corresponding time index, the corresponding cell is selected for statistics.
 *  When outputting data, the full statistical output or
 *  only the output is not 0 according to different requirements
 */
public class ESTHistogram extends BaseMetric implements Histogram {
    // Total number of exponential statistic blocks
    private static final int NUM_BUCKETS = 18;
    // The upper boundary index of exponential statistic blocks
    private static final int MAX_BUCKET_INDEX = NUM_BUCKETS - 1;
    // The lower boundary value of the last statistical block
    private static final long POWER_2_17 = 131072L;
    // Natural logarithm of 2
    private static final double LOG2_VALUE = Math.log(2);
    // Simple statistic items
    private final LongStatsCounter count;
    private final LongMinGauge min;
    private final LongMaxGauge max;
    // Exponential Statistic cells for Time
    private final LongStatsCounter[] buckets = new LongStatsCounter[NUM_BUCKETS];

    /**
     * Initial Exponential Statistics for Time Histogram
     *
     * @param metricName   metric name
     * @param prefix       the prefix of metric item
     */
    public ESTHistogram(String metricName, String prefix) {
        super(metricName, prefix);
        this.count = new LongStatsCounter("count", getFullName());
        this.min = new LongMinGauge("min", getFullName());
        this.max = new LongMaxGauge("max", getFullName());
        StringBuilder strBuff =
                new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE);
        // Initialize the name and store room of each cell
        // each cell is a left-closed right-open interval, and
        // the cell name consists of left boundary value + "t" + right boundary value
        for (int i = 0; i < NUM_BUCKETS; i++) {
            strBuff.append("cell_");
            if (i == 0) {
                strBuff.append(0).append("t").append((int) Math.pow(2, i + 1));
            } else if (i == MAX_BUCKET_INDEX) {
                strBuff.append((int) Math.pow(2, i)).append("tMax");
            } else {
                strBuff.append((int) Math.pow(2, i))
                        .append("t").append((int) Math.pow(2, i + 1));
            }
            this.buckets[i] = new LongStatsCounter(strBuff.toString(), getFullName());
            strBuff.delete(0, strBuff.length());
        }
    }

    @Override
    public void update(long newValue) {
        this.count.incValue();
        this.min.update(newValue);
        this.max.update(newValue);
        int index = (newValue <= 0L) ? 0 : ((newValue >= POWER_2_17)
                ? MAX_BUCKET_INDEX : ((int) (Math.log(newValue) / LOG2_VALUE)));
        this.buckets[index].incValue();
    }

    @Override
    public void getValue(Map<String, Long> keyValMap, boolean includeZero) {
        keyValMap.put(this.count.getFullName(), this.count.getValue());
        keyValMap.put(this.min.getFullName(), this.min.getValue());
        keyValMap.put(this.max.getFullName(), this.max.getValue());
        if (includeZero) {
            for (int i = 0; i < NUM_BUCKETS; i++) {
                keyValMap.put(this.buckets[i].getFullName(), this.buckets[i].getValue());
            }
        } else {
            long tmpValue;
            for (int i = 0; i < NUM_BUCKETS; i++) {
                tmpValue = this.buckets[i].getValue();
                if (tmpValue > 0) {
                    keyValMap.put(this.buckets[i].getFullName(), tmpValue);
                }
            }
        }
    }

    @Override
    public void getValue(StringBuilder strBuff, boolean includeZero) {
        strBuff.append("\"").append(getFullName()).append("\":")
                .append("{\"").append(this.count.getShortName()).append("\":")
                .append(this.count.getValue()).append(",\"")
                .append(this.min.getShortName()).append("\":")
                .append(this.min.getValue()).append(",\"")
                .append(this.max.getShortName()).append("\":")
                .append(this.max.getValue()).append(",\"cells\":{");
        int count = 0;
        if (includeZero) {
            for (int i = 0; i < NUM_BUCKETS; i++) {
                if (count++ > 0) {
                    strBuff.append(",");
                }
                strBuff.append("\"").append(this.buckets[i].getShortName()).append("\":")
                        .append(this.buckets[i].getValue());
            }
        } else {
            long tmpValue;
            for (int i = 0; i < NUM_BUCKETS; i++) {
                tmpValue = this.buckets[i].getValue();
                if (tmpValue > 0) {
                    if (count++ > 0) {
                        strBuff.append(",");
                    }
                    strBuff.append("\"").append(this.buckets[i].getShortName()).append("\":").append(tmpValue);
                }
            }
        }
        strBuff.append("}}");
    }

    @Override
    public void snapShort(Map<String, Long> keyValMap, boolean includeZero) {
        keyValMap.put(this.count.getFullName(), this.count.getAndResetValue());
        keyValMap.put(this.min.getFullName(), this.min.getAndResetValue());
        keyValMap.put(this.max.getFullName(), this.max.getAndResetValue());
        if (includeZero) {
            for (int i = 0; i < NUM_BUCKETS; i++) {
                keyValMap.put(this.buckets[i].getFullName(), this.buckets[i].getAndResetValue());
            }
        } else {
            long tmpValue;
            for (int i = 0; i < NUM_BUCKETS; i++) {
                tmpValue = this.buckets[i].getAndResetValue();
                if (tmpValue > 0) {
                    keyValMap.put(this.buckets[i].getFullName(), tmpValue);
                }
            }
        }
    }

    @Override
    public void snapShort(StringBuilder strBuff, boolean includeZero) {
        strBuff.append("\"").append(getFullName()).append("\":")
                .append("{\"").append(this.count.getShortName()).append("\":")
                .append(this.count.getAndResetValue()).append(",\"")
                .append(this.min.getShortName()).append("\":")
                .append(this.min.getAndResetValue()).append(",\"")
                .append(this.max.getShortName()).append("\":")
                .append(this.max.getAndResetValue()).append(",\"cells\":{");
        int count = 0;
        if (includeZero) {
            for (int i = 0; i < NUM_BUCKETS; i++) {
                if (count++ > 0) {
                    strBuff.append(",");
                }
                strBuff.append("\"").append(this.buckets[i].getShortName()).append("\":")
                        .append(this.buckets[i].getAndResetValue());
            }
        } else {
            long tmpValue;
            for (int i = 0; i < NUM_BUCKETS; i++) {
                tmpValue = this.buckets[i].getAndResetValue();
                if (tmpValue > 0) {
                    if (count++ > 0) {
                        strBuff.append(",");
                    }
                    strBuff.append("\"").append(this.buckets[i].getShortName()).append("\":").append(tmpValue);
                }
            }
        }
        strBuff.append("}}");
    }

    @Override
    public void clear() {
        this.count.clear();
        this.min.clear();
        this.max.clear();
        for (int i = 0; i < NUM_BUCKETS; i++) {
            this.buckets[i].clear();
        }
    }
}
