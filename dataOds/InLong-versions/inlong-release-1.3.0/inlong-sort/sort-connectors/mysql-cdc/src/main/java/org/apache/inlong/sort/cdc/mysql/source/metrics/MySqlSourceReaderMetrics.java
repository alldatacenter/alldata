/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.cdc.mysql.source.metrics;

import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Gauge;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.metrics.MetricGroup;
import org.apache.inlong.audit.AuditImp;
import org.apache.inlong.sort.base.Constants;
import org.apache.inlong.sort.cdc.mysql.source.reader.MySqlSourceReader;

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

    private Counter numRecordsIn;
    private Counter numBytesIn;
    private Meter numRecordsInPerSecond;
    private Meter numBytesInPerSecond;
    private static Integer TIME_SPAN_IN_SECONDS = 60;
    private static String STREAM_ID = "streamId";
    private static String GROUP_ID = "groupId";
    private static String NODE_ID = "nodeId";
    private String inlongGroupId;
    private String inlongSteamId;
    private String nodeId;
    private AuditImp auditImp;

    public MySqlSourceReaderMetrics(MetricGroup metricGroup) {
        this.metricGroup = metricGroup;
    }

    public void registerMetrics() {
        metricGroup.gauge("currentFetchEventTimeLag", (Gauge<Long>) this::getFetchDelay);
        metricGroup.gauge("currentEmitEventTimeLag", (Gauge<Long>) this::getEmitDelay);
        metricGroup.gauge("sourceIdleTime", (Gauge<Long>) this::getIdleTime);
    }

    public void registerMetricsForNumRecordsIn(String metricName) {
        numRecordsIn =
                metricGroup.addGroup(GROUP_ID, this.inlongGroupId).addGroup(STREAM_ID, this.inlongSteamId)
                        .addGroup(NODE_ID, this.nodeId)
                        .counter(metricName);
    }

    public void registerMetricsForNumBytesIn(String metricName) {
        numBytesIn =
                metricGroup.addGroup(GROUP_ID, this.inlongGroupId).addGroup(STREAM_ID, this.inlongSteamId)
                        .addGroup(NODE_ID, this.nodeId)
                        .counter(metricName);
    }

    public void registerMetricsForNumRecordsInPerSecond(String metricName) {
        numRecordsInPerSecond =
                metricGroup.addGroup(GROUP_ID, this.inlongGroupId).addGroup(STREAM_ID, this.inlongSteamId)
                        .addGroup(NODE_ID, nodeId)
                        .meter(metricName, new MeterView(this.numRecordsIn, TIME_SPAN_IN_SECONDS));
    }

    public void registerMetricsForNumBytesInPerSecond(String metricName) {
        numBytesInPerSecond = metricGroup.addGroup(GROUP_ID, this.inlongGroupId).addGroup(STREAM_ID, this.inlongSteamId)
                .addGroup(NODE_ID, this.nodeId)
                .meter(metricName, new MeterView(this.numBytesIn, TIME_SPAN_IN_SECONDS));
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

    public Counter getNumRecordsIn() {
        return numRecordsIn;
    }

    public Counter getNumBytesIn() {
        return numBytesIn;
    }

    public Meter getNumRecordsInPerSecond() {
        return numRecordsInPerSecond;
    }

    public Meter getNumBytesInPerSecond() {
        return numBytesInPerSecond;
    }

    public String getInlongGroupId() {
        return inlongGroupId;
    }

    public String getInlongSteamId() {
        return inlongSteamId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setInlongGroupId(String inlongGroupId) {
        this.inlongGroupId = inlongGroupId;
    }

    public void setInlongSteamId(String inlongSteamId) {
        this.inlongSteamId = inlongSteamId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public AuditImp getAuditImp() {
        return auditImp;
    }

    public void setAuditImp(AuditImp auditImp) {
        this.auditImp = auditImp;
    }

    public void outputMetrics(long rowCountSize, long rowDataSize) {
        outputMetricForFlink(rowCountSize, rowDataSize);
        outputMetricForAudit(rowCountSize, rowDataSize);
    }

    public void outputMetricForAudit(long rowCountSize, long rowDataSize) {
        if (this.auditImp != null) {
            this.auditImp.add(
                    Constants.AUDIT_SORT_INPUT,
                    getInlongGroupId(),
                    getInlongSteamId(),
                    System.currentTimeMillis(),
                    rowCountSize,
                    rowDataSize);
        }
    }

    public void outputMetricForFlink(long rowCountSize, long rowDataSize) {
        if (this.numBytesIn != null) {
            numBytesIn.inc(rowDataSize);
        }
        if (this.numRecordsIn != null) {
            this.numRecordsIn.inc(rowCountSize);
        }
    }
}
