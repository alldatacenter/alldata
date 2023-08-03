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

package org.apache.inlong.dataproxy.metrics.stats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * MonitorIndex
 *
 *
 * The index statistics received or sent by DataProxy nodes, and output to file.
 */
public class MonitorIndex extends AbsStatsDaemon {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorIndex.class);

    private static final AtomicLong RECODE_ID = new AtomicLong(0);
    private final StatsUnit[] statsUnits = new StatsUnit[2];

    public MonitorIndex(String name, long intervalMill, int maxCnt) {
        super(name, intervalMill, maxCnt);
        this.statsUnits[0] = new StatsUnit(name);
        this.statsUnits[1] = new StatsUnit(name);
    }

    /**
     * Add success statistic
     *
     * @param key the statistic key
     * @param msgCnt  the message count
     * @param packCnt the package count
     * @param packSize the package size
     */
    public void addSuccStats(String key, int msgCnt, int packCnt, long packSize) {
        if (isStopped()) {
            return;
        }
        statsUnits[getWriteIndex()].addSuccCnt(key, msgCnt, packCnt, packSize);
    }

    /**
     * Add failure statistic
     *
     * @param key  the statistic key
     * @param failCnt  the failure count
     */
    public void addFailStats(String key, int failCnt) {
        if (isStopped()) {
            return;
        }
        statsUnits[getWriteIndex()].addFailCnt(key, failCnt);
    }

    @Override
    protected int loopProcess(long startTime) {
        return statsUnits[getReadIndex()].printAndResetStatsInfo(startTime);
    }

    @Override
    protected int exitProcess(long startTime) {
        int totalCnt = 0;
        if (!statsUnits[getReadIndex()].isEmpty()) {
            totalCnt += statsUnits[getReadIndex()].printAndResetStatsInfo(startTime);
        }
        if (!statsUnits[getWriteIndex()].isEmpty()) {
            totalCnt += statsUnits[getWriteIndex()].printAndResetStatsInfo(startTime);
        }
        return totalCnt;
    }

    private static class StatsUnit {

        private final String statsName;
        private final ConcurrentHashMap<String, StatsItem> counterMap = new ConcurrentHashMap<>();

        public StatsUnit(String statsName) {
            this.statsName = statsName;
        }

        public boolean isEmpty() {
            return counterMap.isEmpty();
        }

        public void addSuccCnt(String key, int cnt, int packcnt, long packsize) {
            StatsItem statsItem = counterMap.get(key);
            if (statsItem == null) {
                StatsItem tmpItem = new StatsItem();
                statsItem = counterMap.putIfAbsent(key, tmpItem);
                if (statsItem == null) {
                    statsItem = tmpItem;
                }
            }
            statsItem.addSuccessCnt(cnt, packcnt, packsize);
        }

        public void addFailCnt(String key, int failCnt) {
            StatsItem statsItem = counterMap.get(key);
            if (statsItem == null) {
                StatsItem tmpItem = new StatsItem();
                statsItem = counterMap.putIfAbsent(key, tmpItem);
                if (statsItem == null) {
                    statsItem = tmpItem;
                }
            }
            statsItem.addFailCnt(failCnt);
        }

        public int printAndResetStatsInfo(long startTime) {
            int printCnt = 0;
            // get print time (second)
            long printTime = startTime / 1000;
            for (Map.Entry<String, StatsItem> entry : counterMap.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                LOGGER.info("{}#{}_{}#{}#{}", this.statsName, printTime,
                        RECODE_ID.incrementAndGet(), entry.getKey(), entry.getValue().toString());
                printCnt++;
            }
            counterMap.clear();
            return printCnt;
        }
    }

    private static class StatsItem {

        private final LongAdder msgCnt = new LongAdder();
        private final LongAdder packCnt = new LongAdder();
        private final LongAdder packSize = new LongAdder();
        private final LongAdder failCnt = new LongAdder();

        public StatsItem() {

        }

        public void addSuccessCnt(int msgCnt, int packCnt, long packSize) {
            this.msgCnt.add(msgCnt);
            this.packCnt.add(packCnt);
            this.packSize.add(packSize);
        }

        public void addFailCnt(int failCnt) {
            this.failCnt.add(failCnt);
        }

        @Override
        public String toString() {
            return msgCnt.longValue() + "#" + packCnt.longValue() + "#"
                    + packSize.longValue() + "#" + failCnt.longValue();
        }
    }

}
