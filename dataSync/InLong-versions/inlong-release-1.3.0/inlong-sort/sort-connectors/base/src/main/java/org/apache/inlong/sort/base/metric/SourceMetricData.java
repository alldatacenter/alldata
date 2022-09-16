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

import java.util.Arrays;
import java.util.HashSet;

import static org.apache.inlong.sort.base.Constants.DELIMITER;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_IN;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_IN_PER_SECOND;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_IN;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_IN_PER_SECOND;

/**
 * A collection class for handling metrics
 */
public class SourceMetricData implements MetricData {

    private final MetricGroup metricGroup;
    private final String groupId;
    private final String streamId;
    private final String nodeId;
    private Counter numRecordsIn;
    private Counter numBytesIn;
    private Meter numRecordsInPerSecond;
    private Meter numBytesInPerSecond;
    private final AuditImp auditImp;

    public SourceMetricData(String groupId, String streamId, String nodeId, MetricGroup metricGroup) {
        this(groupId, streamId, nodeId, metricGroup, (AuditImp) null);
    }

    public SourceMetricData(MetricOption option, MetricGroup metricGroup) {
        this(option.getGroupId(), option.getStreamId(), option.getNodeId(), metricGroup, option.getIpPorts());
    }

    public SourceMetricData(String groupId, String streamId, String nodeId, MetricGroup metricGroup,
            AuditImp auditImp) {
        this.groupId = groupId;
        this.streamId = streamId;
        this.nodeId = nodeId;
        this.metricGroup = metricGroup;
        this.auditImp = auditImp;
    }

    public SourceMetricData(String groupId, String streamId, String nodeId, MetricGroup metricGroup,
            @Nullable String auditHostAndPorts) {
        this.groupId = groupId;
        this.streamId = streamId;
        this.nodeId = nodeId;
        this.metricGroup = metricGroup;
        if (auditHostAndPorts != null) {
            AuditImp.getInstance().setAuditProxy(new HashSet<>(Arrays.asList(auditHostAndPorts.split(DELIMITER))));
            this.auditImp = AuditImp.getInstance();
        } else {
            this.auditImp = null;
        }
    }

    /**
     * Default counter is {@link SimpleCounter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumRecordsIn() {
        registerMetricsForNumRecordsIn(new SimpleCounter());
    }

    /**
     * User can use custom counter that extends from {@link Counter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumRecordsIn(Counter counter) {
        numRecordsIn = registerCounter(NUM_RECORDS_IN, counter);
    }

    /**
     * Default counter is {@link SimpleCounter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumBytesIn() {
        registerMetricsForNumBytesIn(new SimpleCounter());
    }

    /**
     * User can use custom counter that extends from {@link Counter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumBytesIn(Counter counter) {
        numBytesIn = registerCounter(NUM_BYTES_IN, counter);
    }

    public void registerMetricsForNumRecordsInPerSecond() {
        numRecordsInPerSecond = registerMeter(NUM_RECORDS_IN_PER_SECOND, this.numRecordsIn);
    }

    public void registerMetricsForNumBytesInPerSecond() {
        numBytesInPerSecond = registerMeter(NUM_BYTES_IN_PER_SECOND, this.numBytesIn);
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

    public void outputMetrics(long rowCountSize, long rowDataSize) {
        outputMetricForFlink(rowCountSize, rowDataSize);
        outputMetricForAudit(rowCountSize, rowDataSize);
    }

    public void outputMetricForAudit(long rowCountSize, long rowDataSize) {
        if (auditImp != null) {
            auditImp.add(
                    Constants.AUDIT_SORT_INPUT,
                    getGroupId(),
                    getStreamId(),
                    System.currentTimeMillis(),
                    rowCountSize,
                    rowDataSize);
        }
    }

    public void outputMetricForFlink(long rowCountSize, long rowDataSize) {
        this.numBytesIn.inc(rowDataSize);
        this.numRecordsIn.inc(rowCountSize);
    }
}
