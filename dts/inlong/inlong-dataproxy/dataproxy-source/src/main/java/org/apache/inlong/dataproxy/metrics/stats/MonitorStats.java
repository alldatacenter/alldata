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
import java.util.concurrent.atomic.LongAdder;

/**
 * MonitorStats
 *
 *
 * Summary statistics and detailed statistics on failure, output to file.
 */
public class MonitorStats extends AbsStatsDaemon {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorStats.class);

    private final StatsUnit[] statsUnits = new StatsUnit[2];

    public MonitorStats(String name, long intervalMill, int maxCnt) {
        super(name, intervalMill, maxCnt);
        this.statsUnits[0] = new StatsUnit(name);
        this.statsUnits[1] = new StatsUnit(name);
    }

    /**
     * add summary statistic items
     *
     * @param sumKey the summary key
     */
    public void incSumStats(String sumKey) {
        if (isStopped()) {
            return;
        }
        statsUnits[getWriteIndex()].incSumStats(sumKey);
    }

    /**
     * add detail statistic items
     *
     * @param detailKey the detail key
     */
    public void incDetailStats(String detailKey) {
        if (isStopped()) {
            return;
        }
        statsUnits[getWriteIndex()].incDetailStats(detailKey);
    }

    @Override
    protected int loopProcess(long startTime) {
        return statsUnits[getReadIndex()].printAndResetStatsInfo();
    }

    @Override
    protected int exitProcess(long startTime) {
        int totalCnt = 0;
        if (!statsUnits[getReadIndex()].isEmpty()) {
            totalCnt += statsUnits[getReadIndex()].printAndResetStatsInfo();
        }
        if (!statsUnits[getWriteIndex()].isEmpty()) {
            totalCnt += statsUnits[getWriteIndex()].printAndResetStatsInfo();
        }
        return totalCnt;
    }

    private static class StatsUnit {

        private final String statsName;
        private final ConcurrentHashMap<String, LongAdder> sumMap = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, LongAdder> detailsMap = new ConcurrentHashMap<>();

        public StatsUnit(String statsName) {
            this.statsName = statsName;
        }

        public boolean isEmpty() {
            return sumMap.isEmpty() && detailsMap.isEmpty();
        }

        public void incSumStats(String key) {
            LongAdder statsItem = sumMap.get(key);
            if (statsItem == null) {
                LongAdder tmpItem = new LongAdder();
                statsItem = sumMap.putIfAbsent(key, tmpItem);
                if (statsItem == null) {
                    statsItem = tmpItem;
                }
            }
            statsItem.increment();
        }

        public void incDetailStats(String key) {
            LongAdder statsItem = detailsMap.get(key);
            if (statsItem == null) {
                LongAdder tmpItem = new LongAdder();
                statsItem = detailsMap.putIfAbsent(key, tmpItem);
                if (statsItem == null) {
                    statsItem = tmpItem;
                }
            }
            statsItem.increment();
        }

        public int printAndResetStatsInfo() {
            int sumCnt = 0;
            // print summary info
            for (Map.Entry<String, LongAdder> entry : sumMap.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                LOGGER.info("{}.summary.{}={}", this.statsName, entry.getKey(), entry.getValue());
                sumCnt++;
            }
            // print detail info
            for (Map.Entry<String, LongAdder> entry : detailsMap.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                LOGGER.info("{}.detail.{}={}", this.statsName, entry.getKey(), entry.getValue());
                sumCnt++;
            }
            sumMap.clear();
            detailsMap.clear();
            return sumCnt;
        }
    }

}
