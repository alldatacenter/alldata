/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.inlong.sort.base.metric;

import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.metrics.SimpleCounter;
import org.apache.inlong.audit.AuditImp;
import org.apache.inlong.sort.base.Constants;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;

import static org.apache.inlong.sort.base.Constants.DELIMITER;
import static org.apache.inlong.sort.base.Constants.DIRTY_BYTES;
import static org.apache.inlong.sort.base.Constants.DIRTY_RECORDS;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_OUT_PER_SECOND;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_OUT_PER_SECOND;

/**
 * A collection class for handling metrics
 */
public class SinkMetricData implements MetricData {

    private final MetricGroup metricGroup;
    private final String groupId;
    private final String streamId;
    private final String nodeId;
    private AuditImp auditImp;
    private Counter numRecordsOut;
    private Counter numBytesOut;
    private Counter dirtyRecords;
    private Counter dirtyBytes;
    private Meter numRecordsOutPerSecond;
    private Meter numBytesOutPerSecond;

    public SinkMetricData(String groupId, String streamId, String nodeId, MetricGroup metricGroup) {
        this(groupId, streamId, nodeId, metricGroup, null);
    }

    public SinkMetricData(MetricOption option, MetricGroup metricGroup) {
        this(option.getGroupId(), option.getStreamId(), option.getNodeId(), metricGroup, option.getIpPorts());
    }

    public SinkMetricData(
            String groupId, String streamId, String nodeId, MetricGroup metricGroup,
            @Nullable String auditHostAndPorts) {
        this.metricGroup = metricGroup;
        this.groupId = groupId;
        this.streamId = streamId;
        this.nodeId = nodeId;
        if (auditHostAndPorts != null) {
            AuditImp.getInstance().setAuditProxy(new HashSet<>(Arrays.asList(auditHostAndPorts.split(DELIMITER))));
            this.auditImp = AuditImp.getInstance();
        }
    }

    /**
     * Default counter is {@link SimpleCounter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumRecordsOut() {
        registerMetricsForNumRecordsOut(new SimpleCounter());
    }

    /**
     * User can use custom counter that extends from {@link Counter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumRecordsOut(Counter counter) {
        numRecordsOut = registerCounter(NUM_RECORDS_OUT, counter);
    }

    /**
     * Default counter is {@link SimpleCounter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumBytesOut() {
        registerMetricsForNumBytesOut(new SimpleCounter());

    }

    /**
     * User can use custom counter that extends from {@link Counter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumBytesOut(Counter counter) {
        numBytesOut = registerCounter(NUM_BYTES_OUT, counter);
    }

    public void registerMetricsForNumRecordsOutPerSecond() {
        numRecordsOutPerSecond = registerMeter(NUM_RECORDS_OUT_PER_SECOND, this.numRecordsOut);
    }

    public void registerMetricsForNumBytesOutPerSecond() {
        numBytesOutPerSecond = registerMeter(NUM_BYTES_OUT_PER_SECOND, this.numBytesOut);
    }

    public void registerMetricsForDirtyRecords() {
        registerMetricsForDirtyRecords(new SimpleCounter());
    }

    public void registerMetricsForDirtyRecords(Counter counter) {
        dirtyRecords = registerCounter(DIRTY_RECORDS, counter);
    }

    /**
     * Default counter is {@link SimpleCounter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForDirtyBytes() {
        registerMetricsForDirtyBytes(new SimpleCounter());
    }

    /**
     * User can use custom counter that extends from {@link Counter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForDirtyBytes(Counter counter) {
        dirtyBytes = registerCounter(DIRTY_BYTES, counter);
    }

    public Counter getNumRecordsOut() {
        return numRecordsOut;
    }

    public Counter getNumBytesOut() {
        return numBytesOut;
    }

    public Counter getDirtyRecords() {
        return dirtyRecords;
    }

    public Counter getDirtyBytes() {
        return dirtyBytes;
    }

    public Meter getNumRecordsOutPerSecond() {
        return numRecordsOutPerSecond;
    }

    public Meter getNumBytesOutPerSecond() {
        return numBytesOutPerSecond;
    }

    @Override
    public MetricGroup getMetricGroup() {
        return metricGroup;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getStreamId() {
        return streamId;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    public void invokeWithEstimate(Object o) {
        long size = o.toString().getBytes(StandardCharsets.UTF_8).length;
        getNumRecordsOut().inc();
        getNumBytesOut().inc(size);
        if (auditImp != null) {
            auditImp.add(
                    Constants.AUDIT_SORT_OUTPUT,
                    getGroupId(),
                    getStreamId(),
                    System.currentTimeMillis(),
                    1,
                    size);
        }
    }

    public void invoke(long rowCount, long rowSize) {
        getNumRecordsOut().inc(rowCount);
        getNumBytesOut().inc(rowSize);
        if (auditImp != null) {
            auditImp.add(
                    Constants.AUDIT_SORT_OUTPUT,
                    getGroupId(),
                    getStreamId(),
                    System.currentTimeMillis(),
                    rowCount,
                    rowSize);
        }
    }
}
