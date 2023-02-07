/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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
    private final String p99FullKey;
    private final String p99ShortKey = "P99";
    private final String p999FullKey;
    private final String p999ShortKey = "P999";
    private final String p9999FullKey;
    private final String p9999ShortKey = "P9999";
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
        this.p99FullKey = getFullName() + "_" + p99ShortKey;
        this.p999FullKey = getFullName() + "_" + p999ShortKey;
        this.p9999FullKey = getFullName() + "_" + p9999ShortKey;
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
        int index = (newValue <= 0L) ? 0
                : ((newValue >= POWER_2_17)
                        ? MAX_BUCKET_INDEX
                        : ((int) (Math.log(newValue) / LOG2_VALUE)));
        this.buckets[index].incValue();
    }

    @Override
    public void getValue(Map<String, Long> keyValMap, boolean includeZero) {
        getValue2Map(keyValMap, false, includeZero);
    }

    @Override
    public void getValue(StringBuilder strBuff, boolean includeZero) {
        getValue2StrBuff(strBuff, false, includeZero);
    }

    @Override
    public void snapShort(Map<String, Long> keyValMap, boolean includeZero) {
        getValue2Map(keyValMap, true, includeZero);
    }

    @Override
    public void snapShort(StringBuilder strBuff, boolean includeZero) {
        getValue2StrBuff(strBuff, true, includeZero);
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

    private void getValue2Map(Map<String, Long> keyValMap,
            boolean snapShot, boolean includeZero) {
        long p99Cnt = 0L;
        long p999Cnt = 0L;
        long p9999Cnt = 0L;
        int maxValIndex = -1;
        int match99Index = 0;
        int match999Index = 0;
        int match9999Index = 0;
        long curCnt;
        long maxValue;
        long minValue;
        if (snapShot) {
            curCnt = this.count.getAndResetValue();
            maxValue = this.max.getAndResetValue();
            minValue = this.min.getAndResetValue();
        } else {
            curCnt = this.count.getValue();
            maxValue = this.max.getValue();
            minValue = this.min.getValue();
        }
        if (curCnt > 0) {
            p99Cnt = (long) (curCnt * 0.99);
            p999Cnt = (long) (curCnt * 0.999);
            p9999Cnt = (long) (curCnt * 0.9999);
        }
        // put key and value
        keyValMap.put(this.count.getFullName(), curCnt);
        keyValMap.put(this.min.getFullName(), minValue);
        keyValMap.put(this.max.getFullName(), maxValue);
        long bucketItemVal;
        for (int i = 0; i < NUM_BUCKETS; i++) {
            bucketItemVal = snapShot
                    ? this.buckets[i].getAndResetValue()
                    : this.buckets[i].getValue();
            if (bucketItemVal == 0) {
                if (includeZero) {
                    keyValMap.put(this.buckets[i].getFullName(), bucketItemVal);
                }
            } else {
                keyValMap.put(this.buckets[i].getFullName(), bucketItemVal);
                maxValIndex = i;
                // calculate p99 value
                if (p99Cnt > 0) {
                    p99Cnt -= bucketItemVal;
                    if (p99Cnt <= 0) {
                        match99Index = i;
                    }
                }
                // calculate p999 value
                if (p999Cnt > 0) {
                    p999Cnt -= bucketItemVal;
                    if (p999Cnt <= 0) {
                        match999Index = i;
                    }
                }
                // calculate p9999 value
                if (p9999Cnt > 0) {
                    p9999Cnt -= bucketItemVal;
                    if (p9999Cnt <= 0) {
                        match9999Index = i;
                    }
                }
            }
        }
        keyValMap.put(p99FullKey,
                getPxValue(match99Index, maxValIndex, maxValue));
        keyValMap.put(p999FullKey,
                getPxValue(match999Index, maxValIndex, maxValue));
        keyValMap.put(p9999FullKey,
                getPxValue(match9999Index, maxValIndex, maxValue));
    }

    private void getValue2StrBuff(StringBuilder strBuff,
            boolean snapShot, boolean includeZero) {
        long p99Cnt = 0L;
        long p999Cnt = 0L;
        long p9999Cnt = 0L;
        int maxValIndex = -1;
        int match99Index = 0;
        int match999Index = 0;
        int match9999Index = 0;
        long curCnt;
        long maxValue;
        long minValue;
        if (snapShot) {
            curCnt = this.count.getAndResetValue();
            maxValue = this.max.getAndResetValue();
            minValue = this.min.getAndResetValue();
        } else {
            curCnt = this.count.getValue();
            maxValue = this.max.getValue();
            minValue = this.min.getValue();
        }
        if (curCnt > 0) {
            p99Cnt = (long) (curCnt * 0.99);
            p999Cnt = (long) (curCnt * 0.999);
            p9999Cnt = (long) (curCnt * 0.9999);
        }
        // put key and value
        strBuff.append("\"").append(getFullName()).append("\":")
                .append("{\"").append(this.count.getShortName())
                .append("\":").append(curCnt).append(",\"")
                .append(this.min.getShortName()).append("\":")
                .append(minValue).append(",\"")
                .append(this.max.getShortName()).append("\":")
                .append(maxValue).append(",\"cells\":{");
        int count = 0;
        long bucketItemVal;
        for (int i = 0; i < NUM_BUCKETS; i++) {
            bucketItemVal = snapShot
                    ? this.buckets[i].getAndResetValue()
                    : this.buckets[i].getValue();
            if (bucketItemVal == 0) {
                if (includeZero) {
                    if (count++ > 0) {
                        strBuff.append(",");
                    }
                    strBuff.append("\"").append(this.buckets[i].getShortName())
                            .append("\":").append(bucketItemVal);
                }
            } else {
                if (count++ > 0) {
                    strBuff.append(",");
                }
                strBuff.append("\"").append(this.buckets[i].getShortName())
                        .append("\":").append(bucketItemVal);
                maxValIndex = i;
                // calculate p99 value
                if (p99Cnt > 0) {
                    p99Cnt -= bucketItemVal;
                    if (p99Cnt <= 0) {
                        match99Index = i;
                    }
                }
                // calculate p999 value
                if (p999Cnt > 0) {
                    p999Cnt -= bucketItemVal;
                    if (p999Cnt <= 0) {
                        match999Index = i;
                    }
                }
                // calculate p9999 value
                if (p9999Cnt > 0) {
                    p9999Cnt -= bucketItemVal;
                    if (p9999Cnt <= 0) {
                        match9999Index = i;
                    }
                }
            }
        }
        strBuff.append("},\"").append(p99ShortKey).append("\":")
                .append(getPxValue(match99Index, maxValIndex, maxValue))
                .append(",\"").append(p999ShortKey).append("\":")
                .append(getPxValue(match999Index, maxValIndex, maxValue))
                .append(",\"").append(p9999ShortKey).append("\":")
                .append(getPxValue(match9999Index, maxValIndex, maxValue))
                .append("}");
    }

    private long getPxValue(int endValIndex, int maxValIndex, long maxValue) {
        if (endValIndex < maxValIndex) {
            return (long) Math.pow(2, endValIndex + 1);
        } else {
            return (maxValue == Long.MIN_VALUE) ? 0 : maxValue;
        }
    }
}
