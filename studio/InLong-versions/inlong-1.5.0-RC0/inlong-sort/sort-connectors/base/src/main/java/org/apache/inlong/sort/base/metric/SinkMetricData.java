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

package org.apache.inlong.sort.base.metric;

import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.metrics.SimpleCounter;
import org.apache.inlong.audit.AuditOperator;
import org.apache.inlong.sort.base.Constants;
import org.apache.inlong.sort.base.metric.MetricOption.RegisteredMetric;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.apache.inlong.sort.base.Constants.DIRTY_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.DIRTY_RECORDS_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_OUT_FOR_METER;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_OUT_PER_SECOND;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_OUT_FOR_METER;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_OUT_PER_SECOND;

/**
 * A collection class for handling metrics
 */
public class SinkMetricData implements MetricData {

    private final MetricGroup metricGroup;
    private final Map<String, String> labels;
    private final RegisteredMetric registeredMetric;
    private AuditOperator auditOperator;
    private Counter numRecordsOut;
    private Counter numBytesOut;
    private Counter numRecordsOutForMeter;
    private Counter numBytesOutForMeter;
    private Counter dirtyRecordsOut;
    private Counter dirtyBytesOut;
    private Meter numRecordsOutPerSecond;
    private Meter numBytesOutPerSecond;

    public SinkMetricData(MetricOption option, MetricGroup metricGroup) {
        this.metricGroup = metricGroup;
        this.labels = option.getLabels();
        this.registeredMetric = option.getRegisteredMetric();

        ThreadSafeCounter recordsOutCounter = new ThreadSafeCounter();
        ThreadSafeCounter bytesOutCounter = new ThreadSafeCounter();
        ThreadSafeCounter dirtyRecordsOutCounter = new ThreadSafeCounter();
        ThreadSafeCounter dirtyBytesOutCounter = new ThreadSafeCounter();
        switch (registeredMetric) {
            case DIRTY:
                registerMetricsForDirtyBytesOut(new ThreadSafeCounter());
                registerMetricsForDirtyRecordsOut(new ThreadSafeCounter());
                break;
            case NORMAL:
                recordsOutCounter.inc(option.getInitRecords());
                bytesOutCounter.inc(option.getInitBytes());
                registerMetricsForNumBytesOut(bytesOutCounter);
                registerMetricsForNumRecordsOut(recordsOutCounter);
                registerMetricsForNumBytesOutForMeter(new ThreadSafeCounter());
                registerMetricsForNumRecordsOutForMeter(new ThreadSafeCounter());
                registerMetricsForNumBytesOutPerSecond();
                registerMetricsForNumRecordsOutPerSecond();
                break;
            default:
                recordsOutCounter.inc(option.getInitRecords());
                bytesOutCounter.inc(option.getInitBytes());
                dirtyRecordsOutCounter.inc(option.getInitDirtyRecords());
                dirtyBytesOutCounter.inc(option.getInitDirtyBytes());
                registerMetricsForNumBytesOut(bytesOutCounter);
                registerMetricsForNumRecordsOut(recordsOutCounter);
                registerMetricsForDirtyRecordsOut(dirtyRecordsOutCounter);
                registerMetricsForDirtyBytesOut(dirtyBytesOutCounter);
                registerMetricsForNumBytesOutForMeter(new ThreadSafeCounter());
                registerMetricsForNumRecordsOutForMeter(new ThreadSafeCounter());
                registerMetricsForNumBytesOutPerSecond();
                registerMetricsForNumRecordsOutPerSecond();
                break;

        }

        if (option.getIpPorts().isPresent()) {
            AuditOperator.getInstance().setAuditProxy(option.getIpPortList());
            this.auditOperator = AuditOperator.getInstance();
        }
    }

    /**
     * Default counter is {@link SimpleCounter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumRecordsOutForMeter() {
        registerMetricsForNumRecordsOutForMeter(new SimpleCounter());
    }

    /**
     * User can use custom counter that extends from {@link Counter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumRecordsOutForMeter(Counter counter) {
        numRecordsOutForMeter = registerCounter(NUM_RECORDS_OUT_FOR_METER, counter);
    }

    /**
     * Default counter is {@link SimpleCounter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumBytesOutForMeter() {
        registerMetricsForNumBytesOutForMeter(new SimpleCounter());

    }

    /**
     * User can use custom counter that extends from {@link Counter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForNumBytesOutForMeter(Counter counter) {
        numBytesOutForMeter = registerCounter(NUM_BYTES_OUT_FOR_METER, counter);
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
        numRecordsOutPerSecond = registerMeter(NUM_RECORDS_OUT_PER_SECOND, this.numRecordsOutForMeter);
    }

    public void registerMetricsForNumBytesOutPerSecond() {
        numBytesOutPerSecond = registerMeter(NUM_BYTES_OUT_PER_SECOND, this.numBytesOutForMeter);
    }

    public void registerMetricsForDirtyRecordsOut() {
        registerMetricsForDirtyRecordsOut(new SimpleCounter());
    }

    public void registerMetricsForDirtyRecordsOut(Counter counter) {
        dirtyRecordsOut = registerCounter(DIRTY_RECORDS_OUT, counter);
    }

    /**
     * Default counter is {@link SimpleCounter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForDirtyBytesOut() {
        registerMetricsForDirtyBytesOut(new SimpleCounter());
    }

    /**
     * User can use custom counter that extends from {@link Counter}
     * groupId and streamId and nodeId are label value, user can use it filter metric data when use metric reporter
     * prometheus
     */
    public void registerMetricsForDirtyBytesOut(Counter counter) {
        dirtyBytesOut = registerCounter(DIRTY_BYTES_OUT, counter);
    }

    public Counter getNumRecordsOut() {
        return numRecordsOut;
    }

    public Counter getNumBytesOut() {
        return numBytesOut;
    }

    public Counter getDirtyRecordsOut() {
        return dirtyRecordsOut;
    }

    public Counter getDirtyBytesOut() {
        return dirtyBytesOut;
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
    public Map<String, String> getLabels() {
        return labels;
    }

    public Counter getNumRecordsOutForMeter() {
        return numRecordsOutForMeter;
    }

    public Counter getNumBytesOutForMeter() {
        return numBytesOutForMeter;
    }

    public void invokeWithEstimate(Object o) {
        long size = o.toString().getBytes(StandardCharsets.UTF_8).length;
        invoke(1, size);
    }

    public void invokeDirtyWithEstimate(Object o) {
        long size = o.toString().getBytes(StandardCharsets.UTF_8).length;
        invokeDirty(1, size);
    }

    public void invoke(long rowCount, long rowSize) {
        if (numRecordsOut != null) {
            numRecordsOut.inc(rowCount);
        }

        if (numBytesOut != null) {
            numBytesOut.inc(rowSize);
        }

        if (numRecordsOutForMeter != null) {
            numRecordsOutForMeter.inc(rowCount);
        }

        if (numBytesOutForMeter != null) {
            numBytesOutForMeter.inc(rowSize);
        }

        if (auditOperator != null) {
            auditOperator.add(
                    Constants.AUDIT_SORT_OUTPUT,
                    getGroupId(),
                    getStreamId(),
                    System.currentTimeMillis(),
                    rowCount,
                    rowSize);
        }
    }

    public void invokeDirty(long rowCount, long rowSize) {
        if (dirtyRecordsOut != null) {
            dirtyRecordsOut.inc(rowCount);
        }

        if (dirtyBytesOut != null) {
            dirtyBytesOut.inc(rowSize);
        }
    }

    @Override
    public String toString() {
        switch (registeredMetric) {
            case DIRTY:
                return "SinkMetricData{"
                        + "metricGroup=" + metricGroup
                        + ", labels=" + labels
                        + ", auditOperator=" + auditOperator
                        + ", dirtyRecords=" + dirtyRecordsOut.getCount()
                        + ", dirtyBytes=" + dirtyBytesOut.getCount()
                        + '}';
            case NORMAL:
                return "SinkMetricData{"
                        + "metricGroup=" + metricGroup
                        + ", labels=" + labels
                        + ", auditOperator=" + auditOperator
                        + ", numRecordsOut=" + numRecordsOut.getCount()
                        + ", numBytesOut=" + numBytesOut.getCount()
                        + ", numRecordsOutForMeter=" + numRecordsOutForMeter.getCount()
                        + ", numBytesOutForMeter=" + numBytesOutForMeter.getCount()
                        + ", numRecordsOutPerSecond=" + numRecordsOutPerSecond.getRate()
                        + ", numBytesOutPerSecond=" + numBytesOutPerSecond.getRate()
                        + '}';
            default:
                return "SinkMetricData{"
                        + "metricGroup=" + metricGroup
                        + ", labels=" + labels
                        + ", auditOperator=" + auditOperator
                        + ", numRecordsOut=" + numRecordsOut.getCount()
                        + ", numBytesOut=" + numBytesOut.getCount()
                        + ", numRecordsOutForMeter=" + numRecordsOutForMeter.getCount()
                        + ", numBytesOutForMeter=" + numBytesOutForMeter.getCount()
                        + ", dirtyRecordsOut=" + dirtyRecordsOut.getCount()
                        + ", dirtyBytesOut=" + dirtyBytesOut.getCount()
                        + ", numRecordsOutPerSecond=" + numRecordsOutPerSecond.getRate()
                        + ", numBytesOutPerSecond=" + numBytesOutPerSecond.getRate()
                        + '}';
        }
    }
}
