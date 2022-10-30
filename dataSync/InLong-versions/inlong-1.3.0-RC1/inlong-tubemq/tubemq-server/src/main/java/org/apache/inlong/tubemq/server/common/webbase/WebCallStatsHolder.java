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

package org.apache.inlong.tubemq.server.common.webbase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.metric.impl.ESTHistogram;
import org.apache.inlong.tubemq.corebase.metric.impl.SimpleHistogram;
import org.apache.inlong.tubemq.corebase.metric.impl.SinceTime;

/**
 * WebCallStatsHolder, statistic for web api calls
 *
 * This method class statistic the total number of web api calls and
 * the distribution of call time consumption, as well as the total number of times and
 * extreme time consumption of each method
 */
public class WebCallStatsHolder {
    // Switchable statistic items
    private static final WebCallStatsItemSet[] switchableSets = new WebCallStatsItemSet[2];
    // Current writable index
    private static final AtomicInteger writableIndex = new AtomicInteger(0);
    // Last snapshot time
    private static final AtomicLong lstSnapshotTime = new AtomicLong(0);
    // whether the statistic is manual closed
    private static volatile boolean isManualClosed = false;

    // Initial service statistic set
    static {
        switchableSets[0] = new WebCallStatsItemSet();
        switchableSets[1] = new WebCallStatsItemSet();
    }

    // metric set operate APIs begin
    public static void getValue(Map<String, Long> statsMap) {
        getStatsValue(switchableSets[getIndex()], false, statsMap);
    }

    public static void getValue(StringBuilder strBuff) {
        getStatsValue(switchableSets[getIndex()], false, strBuff);
    }

    public static void snapShort(Map<String, Long> statsMap) {
        if (switchWritingStatsUnit()) {
            getStatsValue(switchableSets[getIndex(writableIndex.get() - 1)], true, statsMap);
        } else {
            getStatsValue(switchableSets[getIndex()], false, statsMap);
        }
    }

    public static void snapShort(StringBuilder strBuff) {
        if (switchWritingStatsUnit()) {
            getStatsValue(switchableSets[getIndex(writableIndex.get() - 1)], true, strBuff);
        } else {
            getStatsValue(switchableSets[getIndex()], false, strBuff);
        }
    }

    /**
     * Set manually the statistic status.
     *
     * @param enableStats  enable or disable the statistic.
     */
    public static synchronized void setStatsStatus(boolean enableStats) {
        WebCallStatsHolder.isManualClosed = !enableStats;
    }

    /**
     * Query whether the statistic is closed.
     *
     * @return the statistic status
     */
    public static boolean isStatsClosed() {
        return WebCallStatsHolder.isManualClosed;
    }
    // metric set operate APIs end

    // metric item operate APIs begin
    public static void addMethodCall(String method, long callDlt) {
        if (isManualClosed) {
            return;
        }
        method = (method == null) ? "NULL" : method;
        WebCallStatsItemSet webCallStatsSet = switchableSets[getIndex()];
        webCallStatsSet.totalCallStats.update(callDlt);
        SimpleHistogram curMethodStat = webCallStatsSet.methodStatsMap.get(method);
        if (curMethodStat == null) {
            SimpleHistogram tmpSimpleStat = new SimpleHistogram(method, "method");
            curMethodStat = webCallStatsSet.methodStatsMap.putIfAbsent(method, tmpSimpleStat);
            if (curMethodStat == null) {
                curMethodStat = tmpSimpleStat;
            }
        }
        curMethodStat.update(callDlt);
    }
    // metric set operate APIs end

    // private functions
    private static boolean switchWritingStatsUnit() {
        long curSnapshotTime = lstSnapshotTime.get();
        // Avoid frequent snapshots
        if ((System.currentTimeMillis() - curSnapshotTime)
                >= TBaseConstants.CFG_STATS_MIN_SNAPSHOT_PERIOD_MS) {
            if (lstSnapshotTime.compareAndSet(curSnapshotTime, System.currentTimeMillis())) {
                switchableSets[getIndex(writableIndex.incrementAndGet())].resetSinceTime();
                return true;
            }
        }
        return false;
    }

    private static void getStatsValue(WebCallStatsItemSet statsSet,
                                      boolean resetValue,
                                      Map<String, Long> statsMap) {
        statsMap.put(statsSet.lstResetTime.getFullName(),
                statsSet.lstResetTime.getSinceTime());
        statsMap.put("isClosed", (isManualClosed ? 1L : 0L));
        if (resetValue) {
            statsSet.totalCallStats.snapShort(statsMap, false);
        } else {
            statsSet.totalCallStats.getValue(statsMap, false);
        }
        statsSet.getMethodStatsInfo(statsMap, resetValue);
    }

    private static void getStatsValue(WebCallStatsItemSet statsSet,
                                      boolean resetValue,
                                      StringBuilder strBuff) {
        strBuff.append("{\"").append(statsSet.lstResetTime.getFullName())
                .append("\":\"").append(statsSet.lstResetTime.getStrSinceTime())
                .append("\",\"isClosed\":").append(isManualClosed).append(",");
        if (resetValue) {
            statsSet.totalCallStats.snapShort(strBuff, false);
        } else {
            statsSet.totalCallStats.getValue(strBuff, false);
        }
        strBuff.append(",");
        statsSet.getMethodStatsInfo(strBuff, resetValue);
        strBuff.append("}");
    }

    /**
     * Get current writable block index.
     *
     * @return the writable block index
     */
    private static int getIndex() {
        return getIndex(writableIndex.get());
    }

    /**
     * Gets the metric block index based on the specified value.
     *
     * @param origIndex    the specified value
     * @return the metric block index
     */
    private static int getIndex(int origIndex) {
        return Math.abs(origIndex % 2);
    }

    /**
     * WebCallStatsItemSet, Switchable web call statistics block
     *
     * In which the object is the metric item that can be counted in stages
     */
    private static class WebCallStatsItemSet {
        protected final SinceTime lstResetTime =
                new SinceTime("reset_time", null);
        // Total call statistics
        protected final ESTHistogram totalCallStats =
                new ESTHistogram("web_calls", null);
        // Simple Statistics Based on Methods
        protected final ConcurrentHashMap<String, SimpleHistogram> methodStatsMap =
                new ConcurrentHashMap();

        public WebCallStatsItemSet() {
            resetSinceTime();
        }

        public void resetSinceTime() {
            this.lstResetTime.reset();
        }

        /**
         * Gets the method statistics information
         *
         * @param statsMap    the statistics content contain
         * @param resetValue  whether reset value
         */
        public void getMethodStatsInfo(Map<String, Long> statsMap, boolean resetValue) {
            for (SimpleHistogram itemStats : methodStatsMap.values()) {
                itemStats.getValue(statsMap, false);
            }
            if (resetValue) {
                methodStatsMap.clear();
            }
        }

        /**
         * Gets the method statistics information
         *
         * @param strBuff     the statistics content contain
         * @param resetValue  whether reset value
         */
        public void getMethodStatsInfo(StringBuilder strBuff, boolean resetValue) {
            int totalCnt = 0;
            strBuff.append("\"").append("methods\":{");
            for (SimpleHistogram itemStats : methodStatsMap.values()) {
                if (itemStats == null) {
                    continue;
                }
                if (totalCnt++ > 0) {
                    strBuff.append(",");
                }
                itemStats.getValue(strBuff, false);
            }
            strBuff.append("}");
            if (resetValue) {
                methodStatsMap.clear();
            }
        }
    }
}

