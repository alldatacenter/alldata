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

package org.apache.inlong.sort.base.util;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.metrics.Counter;
import org.apache.inlong.sort.base.enums.ReadPhase;
import org.apache.inlong.sort.base.metric.MetricState;
import org.apache.inlong.sort.base.metric.SinkMetricData;
import org.apache.inlong.sort.base.metric.SourceMetricData;
import org.apache.inlong.sort.base.metric.phase.ReadPhaseMetricData;
import org.apache.inlong.sort.base.metric.sub.SinkSubMetricData;
import org.apache.inlong.sort.base.metric.sub.SourceSubMetricData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.apache.inlong.sort.base.Constants.DIRTY_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.DIRTY_RECORDS_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_IN;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_IN;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_OUT;

/**
 * metric state for {@link MetricState} supporting snapshot and restore
 */
public class MetricStateUtils {

    public static final Logger LOGGER = LoggerFactory.getLogger(MetricStateUtils.class);

    /**
     *
     * restore metric state data
     * @param metricStateListState state data list
     * @param subtaskIndex current subtask index
     * @param currentSubtaskNum number of current parallel subtask
     * @return metric state
     * @throws Exception throw exception metricStateListState.get()
     */
    public static MetricState restoreMetricState(ListState<MetricState> metricStateListState, Integer subtaskIndex,
            Integer currentSubtaskNum) throws Exception {
        if (metricStateListState == null || metricStateListState.get() == null) {
            return null;
        }
        LOGGER.info("restoreMetricState:{}, subtaskIndex:{}, currentSubtaskNum:{}", metricStateListState, subtaskIndex,
                currentSubtaskNum);
        MetricState currentMetricState;
        Map<Integer, MetricState> map = new HashMap<>(16);
        for (MetricState metricState : metricStateListState.get()) {
            map.put(metricState.getSubtaskIndex(), metricState);
        }
        int previousSubtaskNum = map.size();
        if (currentSubtaskNum >= previousSubtaskNum) {
            currentMetricState = map.get(subtaskIndex);
        } else {
            Map<String, MetricState> subMetricStateMap = new HashMap<>();
            Map<String, Long> metrics = new HashMap<>(16);
            currentMetricState = new MetricState(subtaskIndex, metrics);
            currentMetricState.setSubMetricStateMap(subMetricStateMap);
            List<Integer> indexList = computeIndexList(subtaskIndex, currentSubtaskNum, previousSubtaskNum);
            for (Integer index : indexList) {
                MetricState metricState = map.get(index);
                for (Map.Entry<String, Long> entry : metricState.getMetrics().entrySet()) {
                    if (metrics.containsKey(entry.getKey())) {
                        metrics.put(entry.getKey(), metrics.get(entry.getKey()) + entry.getValue());
                    } else {
                        metrics.put(entry.getKey(), entry.getValue());
                    }
                }
                // restore sub source metric data state
                Map<String, MetricState> subIndexMetricStateMap = metricState.getSubMetricStateMap();
                if (subIndexMetricStateMap != null && !subIndexMetricStateMap.isEmpty()) {
                    for (Entry<String, MetricState> entry : subIndexMetricStateMap.entrySet()) {
                        MetricState subMetricState;
                        if (subMetricStateMap.containsKey(entry.getKey())) {
                            subMetricState = subMetricStateMap.get(entry.getKey());
                            Map<String, Long> subMetrics = subMetricState.getMetrics();
                            Map<String, Long> currentSubMetrics = entry.getValue().getMetrics();
                            for (Entry<String, Long> currentSubEntry : currentSubMetrics.entrySet()) {
                                if (subMetrics.containsKey(currentSubEntry.getKey())) {
                                    subMetrics.put(currentSubEntry.getKey(),
                                            subMetrics.get(currentSubEntry.getKey()) + currentSubEntry.getValue());
                                } else {
                                    subMetrics.put(currentSubEntry.getKey(), currentSubEntry.getValue());
                                }
                            }
                        } else {
                            subMetricState = entry.getValue();
                            subMetricState.setSubtaskIndex(subtaskIndex);
                        }
                        subMetricStateMap.put(entry.getKey(), subMetricState);
                    }
                }
            }
        }
        return currentMetricState;
    }

    /**
     *
     * Assignment previous subtask index to current subtask when reduce parallelism
     * n = N/m, get n old task per new subtask, mth new subtask get (N - (m - 1) * n) old task
     * @param subtaskIndex current subtask index
     * @param currentSubtaskNum number of current parallel subtask
     * @param previousSubtaskNum number of previous parallel subtask
     * @return index list
     */
    public static List<Integer> computeIndexList(Integer subtaskIndex, Integer currentSubtaskNum,
            Integer previousSubtaskNum) {
        List<Integer> indexList = new ArrayList<>();
        int assignTaskNum = previousSubtaskNum / currentSubtaskNum;
        if (subtaskIndex == currentSubtaskNum - 1) {
            for (int i = subtaskIndex * assignTaskNum; i < previousSubtaskNum; i++) {
                indexList.add(i);
            }
        } else {
            for (int i = 1; i <= assignTaskNum; i++) {
                indexList.add(i + subtaskIndex * assignTaskNum - 1);
            }
        }
        return indexList;
    }

    /**
     *
     * Snapshot metric state data for {@link SourceMetricData}
     * @param metricStateListState state data list
     * @param sourceMetricData {@link SourceMetricData} A collection class for handling metrics
     * @param subtaskIndex subtask index
     * @throws Exception throw exception when add metric state
     */
    public static void snapshotMetricStateForSourceMetricData(ListState<MetricState> metricStateListState,
            SourceMetricData sourceMetricData, Integer subtaskIndex)
            throws Exception {
        LOGGER.info("snapshotMetricStateForSourceMetricData:{}, sourceMetricData:{}, subtaskIndex:{}",
                metricStateListState, sourceMetricData, subtaskIndex);
        metricStateListState.clear();
        Map<String, Long> metricDataMap = new HashMap<>();
        metricDataMap.put(NUM_RECORDS_IN, sourceMetricData.getNumRecordsIn().getCount());
        metricDataMap.put(NUM_BYTES_IN, sourceMetricData.getNumBytesIn().getCount());
        MetricState metricState = new MetricState(subtaskIndex, metricDataMap);

        // snapshot sub metric data state
        snapshotMetricStateForSourceSubMetricData(sourceMetricData, subtaskIndex, metricState);
        metricStateListState.add(metricState);
    }

    /**
     *
     * Snapshot sub metric state data for {@link SourceSubMetricData}
     * @param sourceMetricData {@link SourceMetricData} A collection class for handling metrics
     * @param subtaskIndex subtask index
     * @param metricState state of source metric data
     * @throws Exception throw exception when add sub metric state
     */
    private static void snapshotMetricStateForSourceSubMetricData(SourceMetricData sourceMetricData,
            Integer subtaskIndex, MetricState metricState) {
        if (!(sourceMetricData instanceof SourceSubMetricData)) {
            return;
        }
        SourceSubMetricData sourcesubMetricData = (SourceSubMetricData) sourceMetricData;
        // snapshot read phase metric
        Map<String, Long> metricDataMap = metricState.getMetrics();
        Map<ReadPhase, ReadPhaseMetricData> readPhaseMetricMap = sourcesubMetricData.getReadPhaseMetricMap();
        if (readPhaseMetricMap != null && !readPhaseMetricMap.isEmpty()) {
            Set<Entry<ReadPhase, ReadPhaseMetricData>> entries = readPhaseMetricMap.entrySet();
            for (Entry<ReadPhase, ReadPhaseMetricData> entry : entries) {
                Counter readPhase = entry.getValue().getReadPhase();
                metricDataMap.put(entry.getKey().getPhase(), (readPhase != null) ? readPhase.getCount() : 0L);
            }
        }
        // snapshot sub source metric
        Map<String, SourceMetricData> subSourceMetricMap = sourcesubMetricData.getSubSourceMetricMap();
        if (subSourceMetricMap != null && !subSourceMetricMap.isEmpty()) {
            Map<String, MetricState> subMetricStateMap = new HashMap<>();
            Set<Entry<String, SourceMetricData>> entries = subSourceMetricMap.entrySet();
            for (Entry<String, SourceMetricData> entry : entries) {
                Map<String, Long> subMetricDataMap = new HashMap<>(4);
                subMetricDataMap.put(NUM_RECORDS_IN, entry.getValue().getNumRecordsIn().getCount());
                subMetricDataMap.put(NUM_BYTES_IN, entry.getValue().getNumBytesIn().getCount());
                subMetricStateMap.put(entry.getKey(), new MetricState(subtaskIndex, subMetricDataMap));
            }
            metricState.setSubMetricStateMap(subMetricStateMap);
        }
    }

    /**
     * Snapshot metric state data for {@link SinkMetricData}
     * @param metricStateListState state data list
     * @param sinkMetricData {@link SinkMetricData} A collection class for handling metrics
     * @param subtaskIndex subtask index
     * @throws Exception throw exception when add metric state
     */
    public static void snapshotMetricStateForSinkMetricData(ListState<MetricState> metricStateListState,
            SinkMetricData sinkMetricData, Integer subtaskIndex)
            throws Exception {
        LOGGER.info("snapshotMetricStateForSinkMetricData:{}, sinkMetricData:{}, subtaskIndex:{}",
                metricStateListState, sinkMetricData, subtaskIndex);
        metricStateListState.clear();
        Map<String, Long> metricDataMap = new HashMap<>();
        metricDataMap.put(NUM_RECORDS_OUT, sinkMetricData.getNumRecordsOut().getCount());
        metricDataMap.put(NUM_BYTES_OUT, sinkMetricData.getNumBytesOut().getCount());
        if (sinkMetricData.getDirtyRecordsOut() != null) {
            metricDataMap.put(DIRTY_RECORDS_OUT, sinkMetricData.getDirtyRecordsOut().getCount());
        }
        if (sinkMetricData.getDirtyBytesOut() != null) {
            metricDataMap.put(DIRTY_BYTES_OUT, sinkMetricData.getDirtyBytesOut().getCount());
        }
        MetricState metricState = new MetricState(subtaskIndex, metricDataMap);

        // snapshot sub metric data state
        snapshotMetricStateForSinkSubMetricData(sinkMetricData, subtaskIndex, metricState);
        metricStateListState.add(metricState);
    }

    /**
     * Snapshot sub metric state data for {@link SinkSubMetricData}
     * @param sinkMetricData {@link SinkMetricData} A collection class for handling metrics
     * @param subtaskIndex subtask index
     * @param metricState state of source metric data
     */
    private static void snapshotMetricStateForSinkSubMetricData(SinkMetricData sinkMetricData,
            Integer subtaskIndex, MetricState metricState) {
        if (!(sinkMetricData instanceof SinkSubMetricData)) {
            return;
        }
        SinkSubMetricData sinkSubMetricData = (SinkSubMetricData) sinkMetricData;

        Map<String, SinkMetricData> subSinkMetricMap = sinkSubMetricData.getSubSinkMetricMap();
        if (subSinkMetricMap != null && !subSinkMetricMap.isEmpty()) {
            Map<String, MetricState> subMetricStateMap = new HashMap<>();
            Set<Entry<String, SinkMetricData>> entries = subSinkMetricMap.entrySet();
            for (Entry<String, SinkMetricData> entry : entries) {
                Map<String, Long> subMetricDataMap = new HashMap<>();
                subMetricDataMap.put(NUM_RECORDS_OUT, entry.getValue().getNumRecordsOut().getCount());
                subMetricDataMap.put(NUM_BYTES_OUT, entry.getValue().getNumBytesOut().getCount());
                subMetricDataMap.put(DIRTY_RECORDS_OUT, entry.getValue().getDirtyRecordsOut().getCount());
                subMetricDataMap.put(DIRTY_BYTES_OUT, entry.getValue().getDirtyBytesOut().getCount());
                subMetricStateMap.put(entry.getKey(), new MetricState(subtaskIndex, subMetricDataMap));
            }
            metricState.setSubMetricStateMap(subMetricStateMap);
        }
    }

}
