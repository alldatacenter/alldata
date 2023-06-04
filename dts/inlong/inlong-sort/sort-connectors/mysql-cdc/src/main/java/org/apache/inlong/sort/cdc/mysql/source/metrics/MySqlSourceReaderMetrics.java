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

package org.apache.inlong.sort.cdc.mysql.source.metrics;

import static org.apache.inlong.sort.base.Constants.NUM_BYTES_IN;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_IN;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.flink.metrics.Gauge;
import org.apache.flink.metrics.MetricGroup;
import org.apache.inlong.sort.base.Constants;
import org.apache.inlong.sort.base.enums.ReadPhase;
import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.base.metric.MetricState;
import org.apache.inlong.sort.base.metric.sub.SourceTableMetricData;
import org.apache.inlong.sort.cdc.mysql.source.reader.MySqlSourceReader;
import org.apache.inlong.sort.cdc.mysql.source.split.MySqlMetricSplit.MySqlTableMetric;

/**
 * A collection class for handling metrics in {@link MySqlSourceReader}.
 */
public class MySqlSourceReaderMetrics {

    private final MetricGroup metricGroup;

    /**
     * The last record processing time, which is updated after {@link MySqlSourceReader} fetches a
     * batch of data. It's mainly used to report metrics sourceIdleTime for sourceIdleTime =
     * System.currentTimeMillis() - processTime.
     */
    private volatile long processTime = 0L;

    /**
     * currentFetchEventTimeLag = FetchTime - messageTimestamp, where the FetchTime is the time the
     * record fetched into the source operator.
     */
    private volatile long fetchDelay = 0L;

    /**
     * emitDelay = EmitTime - messageTimestamp, where the EmitTime is the time the record leaves the
     * source operator.
     */
    private volatile long emitDelay = 0L;

    private SourceTableMetricData sourceTableMetricData;

    public MySqlSourceReaderMetrics(MetricGroup metricGroup) {
        this.metricGroup = metricGroup;
    }

    public void registerMetrics(MetricOption metricOption) {
        if (metricOption != null) {
            sourceTableMetricData = new SourceTableMetricData(metricOption, metricGroup,
                    Arrays.asList(Constants.DATABASE_NAME, Constants.TABLE_NAME));
        }
        metricGroup.gauge("currentFetchEventTimeLag", (Gauge<Long>) this::getFetchDelay);
        metricGroup.gauge("currentEmitEventTimeLag", (Gauge<Long>) this::getEmitDelay);
        metricGroup.gauge("sourceIdleTime", (Gauge<Long>) this::getIdleTime);
    }

    public long getFetchDelay() {
        return fetchDelay;
    }

    public long getEmitDelay() {
        return emitDelay;
    }

    public long getIdleTime() {
        // no previous process time at the beginning, return 0 as idle time
        if (processTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - processTime;
    }

    public void recordProcessTime(long processTime) {
        this.processTime = processTime;
    }

    public void recordFetchDelay(long fetchDelay) {
        this.fetchDelay = fetchDelay;
    }

    public void recordEmitDelay(long emitDelay) {
        this.emitDelay = emitDelay;
    }

    public void outputMetrics(String database, String table, boolean isSnapshotRecord, Object data) {
        if (sourceTableMetricData != null) {
            sourceTableMetricData.outputMetricsWithEstimate(database, table, isSnapshotRecord, data);
        }
    }

    public void initMetrics(long rowCountSize, long rowDataSize, Map<String, Long> readPhaseMetricMap,
            Map<String, MySqlTableMetric> tableMetricMap) {
        if (sourceTableMetricData != null) {
            // node level metric data
            sourceTableMetricData.getNumBytesIn().inc(rowDataSize);
            sourceTableMetricData.getNumRecordsIn().inc(rowCountSize);

            // register read phase metric data and table level metric data
            if (readPhaseMetricMap != null && tableMetricMap != null) {
                MetricState metricState = new MetricState();
                metricState.setMetrics(readPhaseMetricMap);
                Map<String, MetricState> subMetricStateMap = new HashMap<>();
                tableMetricMap.entrySet().stream().filter(v -> v.getValue() != null).forEach(entry -> {
                    MetricState subMetricState = new MetricState();
                    subMetricState.setMetrics(ImmutableMap
                            .of(NUM_RECORDS_IN, entry.getValue().getNumRecordsIn(), NUM_BYTES_IN,
                                    entry.getValue().getNumBytesIn()));
                    subMetricStateMap.put(entry.getKey(), subMetricState);
                });
                metricState.setSubMetricStateMap(subMetricStateMap);
                sourceTableMetricData.registerSubMetricsGroup(metricState);
            }
        }
    }

    public SourceTableMetricData getSourceMetricData() {
        return sourceTableMetricData;
    }

    /**
     * output read phase metric
     *
     * @param readPhase the readPhase of record
     */
    public void outputReadPhaseMetrics(ReadPhase readPhase) {
        sourceTableMetricData.outputReadPhaseMetrics(readPhase);
    }
}
