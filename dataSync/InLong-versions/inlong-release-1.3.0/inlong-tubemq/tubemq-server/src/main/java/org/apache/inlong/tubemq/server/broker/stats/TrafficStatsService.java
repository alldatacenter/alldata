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

package org.apache.inlong.tubemq.server.broker.stats;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.inlong.tubemq.corebase.daemon.AbstractDaemonService;
import org.apache.inlong.tubemq.corebase.metric.TrafficStatsUnit;
import org.apache.inlong.tubemq.corebase.metric.impl.LongOnlineCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TrafficStatsService, Input and Output traffic statistics Service
 *
 *  Due to the large amount of traffic-related metric data, this statistics service uses
 *  a daemon thread to periodically refresh the data to the special metric file
 *  for metric data collection.
 */
public class TrafficStatsService extends AbstractDaemonService implements TrafficService {
    // Maximum write wait time
    private static final long MAX_WRITING_WAIT_DLT = 5000L;
    // Statistics output log file
    private final Logger logger;
    // Statistic category
    private final String statsCat;
    // Switchable traffic statistic units
    private final WritableUnit[] switchableUnits = new WritableUnit[2];
    // Current writable index
    private final AtomicInteger writableIndex = new AtomicInteger(0);

    /**
     * Initial traffic statistics service
     *
     * @param logFileName      the output file name
     * @param countType        the statistic type
     * @param scanIntervalMs   the snapshot interval
     */
    public TrafficStatsService(String logFileName, String countType, long scanIntervalMs) {
        super(logFileName, scanIntervalMs);
        this.statsCat = countType;
        if (logFileName == null) {
            this.logger = LoggerFactory.getLogger(TrafficStatsService.class);
        } else {
            this.logger = LoggerFactory.getLogger(logFileName);
        }
        switchableUnits[0] = new WritableUnit();
        switchableUnits[1] = new WritableUnit();
        super.start();
    }

    @Override
    protected void loopProcess(long intervalMs) {
        int befIndex;
        while (!super.isStopped()) {
            try {
                Thread.sleep(intervalMs);
                // Snapshot metric data
                befIndex = writableIndex.getAndIncrement();
                // Output 2 file
                output2file(befIndex);
            } catch (InterruptedException e) {
                return;
            } catch (Throwable t) {
                //
            }
        }
    }

    @Override
    public void close(long waitTimeMs) {
        if (super.stop()) {
            return;
        }
        // Output remain information
        output2file(writableIndex.get() - 1);
        output2file(writableIndex.get());
    }

    @Override
    public void add(Map<String, TrafficInfo> trafficInfos) {
        TrafficStatsUnit tmpStatsSet;
        TrafficStatsUnit trafficStatsSet;
        // Increment write reference count
        WritableUnit selectedUnit = switchableUnits[getIndex()];
        selectedUnit.refCnt.incValue();
        try {
            // Accumulate statistics information
            ConcurrentHashMap<String, TrafficStatsUnit> tmpStatsSetMap =
                    selectedUnit.statsUnitMap;
            for (Entry<String, TrafficInfo> entry : trafficInfos.entrySet()) {
                trafficStatsSet = tmpStatsSetMap.get(entry.getKey());
                if (trafficStatsSet == null) {
                    tmpStatsSet = new TrafficStatsUnit("msg_cnt", "msg_size", null);
                    trafficStatsSet = tmpStatsSetMap.putIfAbsent(entry.getKey(), tmpStatsSet);
                    if (trafficStatsSet == null) {
                        trafficStatsSet = tmpStatsSet;
                    }
                }
                trafficStatsSet.addMsgCntAndSize(
                        entry.getValue().getMsgCount(), entry.getValue().getMsgSize());
            }
        } finally {
            // Decrement write reference count
            selectedUnit.refCnt.decValue();
        }
    }

    @Override
    public void add(String statsKey, long msgCnt, long msgSize) {
        // Increment write reference count
        WritableUnit selectedUnit = switchableUnits[getIndex()];
        selectedUnit.refCnt.incValue();
        try {
            // Accumulate statistics information
            ConcurrentHashMap<String, TrafficStatsUnit> tmpStatsSetMap =
                    selectedUnit.statsUnitMap;
            TrafficStatsUnit trafficStatsSet = tmpStatsSetMap.get(statsKey);
            if (trafficStatsSet == null) {
                TrafficStatsUnit tmpStatsSet = new TrafficStatsUnit("msg_cnt", "msg_size", null);
                trafficStatsSet = tmpStatsSetMap.putIfAbsent(statsKey, tmpStatsSet);
                if (trafficStatsSet == null) {
                    trafficStatsSet = tmpStatsSet;
                }
            }
            trafficStatsSet.addMsgCntAndSize(msgCnt, msgSize);
        } finally {
            // Decrement write reference count
            selectedUnit.refCnt.decValue();
        }
    }

    /**
     * Print statistics data to file
     *
     * @param readIndex   the readable index
     */
    private void output2file(int readIndex) {
        WritableUnit selectedUnit =
                switchableUnits[getIndex(readIndex)];
        if (selectedUnit == null) {
            return;
        }
        // Wait for the data update operation to complete
        long startTime = System.currentTimeMillis();
        do {
            if (System.currentTimeMillis() - startTime >= MAX_WRITING_WAIT_DLT) {
                break;
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                break;
            }
        } while (selectedUnit.refCnt.getValue() > 0);
        // Output data to file
        Map<String, TrafficStatsUnit> statsMap = selectedUnit.statsUnitMap;
        for (Entry<String, TrafficStatsUnit> entry : statsMap.entrySet()) {
            logger.info("{}#{}#{}#{}", statsCat, entry.getKey(),
                    entry.getValue().msgCnt.getValue(),
                    entry.getValue().msgSize.getValue());
        }
        statsMap.clear();
    }

    /**
     * Get current writable block index.
     *
     * @return the writable block index
     */
    private int getIndex() {
        return getIndex(this.writableIndex.get());
    }

    /**
     * Gets the metric block index based on the specified value.
     *
     * @param origIndex    the specified value
     * @return the metric block index
     */
    private int getIndex(int origIndex) {
        return Math.abs(origIndex % 2);
    }

    /**
     * WritableUnit,
     *
     * This class is mainly defined to facilitate reading and writing of
     * statistic set through array operations, which contains a Map of
     * statistic dimensions and corresponding metric values
     */
    private static class WritableUnit {
        // Current writing thread count
        public LongOnlineCounter refCnt =
                new LongOnlineCounter("ref_count", null);
        // statistic unit map
        protected ConcurrentHashMap<String, TrafficStatsUnit> statsUnitMap =
                new ConcurrentHashMap<>(512);
    }
}
