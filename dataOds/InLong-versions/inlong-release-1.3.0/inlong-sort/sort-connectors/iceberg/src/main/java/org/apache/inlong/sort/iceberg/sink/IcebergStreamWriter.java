/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.inlong.sort.iceberg.sink;

import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.BoundedOneInput;
import org.apache.flink.streaming.api.operators.ChainingStrategy;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.iceberg.flink.sink.TaskWriterFactory;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;
import org.apache.inlong.sort.base.metric.SinkMetricData;
import org.apache.inlong.sort.base.metric.ThreadSafeCounter;

import javax.annotation.Nullable;
import java.io.IOException;

import static org.apache.inlong.sort.base.Constants.DELIMITER;

/**
 * Copy from iceberg-flink:iceberg-flink-1.13:0.13.2
 */
class IcebergStreamWriter<T> extends AbstractStreamOperator<WriteResult>
        implements OneInputStreamOperator<T, WriteResult>, BoundedOneInput {

    private static final long serialVersionUID = 1L;

    private final String fullTableName;
    private final TaskWriterFactory<T> taskWriterFactory;
    private final String inlongMetric;
    private final String auditHostAndPorts;

    private transient TaskWriter<T> writer;
    private transient int subTaskId;
    private transient int attemptId;
    @Nullable
    private transient SinkMetricData metricData;

    IcebergStreamWriter(
            String fullTableName,
            TaskWriterFactory<T> taskWriterFactory,
            String inlongMetric,
            String auditHostAndPorts) {
        this.fullTableName = fullTableName;
        this.taskWriterFactory = taskWriterFactory;
        this.inlongMetric = inlongMetric;
        this.auditHostAndPorts = auditHostAndPorts;
        setChainingStrategy(ChainingStrategy.ALWAYS);
    }

    @Override
    public void open() {
        this.subTaskId = getRuntimeContext().getIndexOfThisSubtask();
        this.attemptId = getRuntimeContext().getAttemptNumber();

        // Initialize the task writer factory.
        this.taskWriterFactory.initialize(subTaskId, attemptId);

        // Initialize the task writer.
        this.writer = taskWriterFactory.create();

        // Initialize metric
        if (inlongMetric != null) {
            String[] inlongMetricArray = inlongMetric.split(DELIMITER);
            String inlongGroupId = inlongMetricArray[0];
            String inlongStreamId = inlongMetricArray[1];
            String nodeId = inlongMetricArray[2];
            metricData = new SinkMetricData(
                    inlongGroupId, inlongStreamId, nodeId, getRuntimeContext().getMetricGroup(), auditHostAndPorts);
            metricData.registerMetricsForDirtyBytes(new ThreadSafeCounter());
            metricData.registerMetricsForDirtyRecords(new ThreadSafeCounter());
            metricData.registerMetricsForNumBytesOut(new ThreadSafeCounter());
            metricData.registerMetricsForNumRecordsOut(new ThreadSafeCounter());
            metricData.registerMetricsForNumBytesOutPerSecond();
            metricData.registerMetricsForNumRecordsOutPerSecond();
        }
    }

    @Override
    public void prepareSnapshotPreBarrier(long checkpointId) throws Exception {
        // close all open files and emit files to downstream committer operator
        emit(writer.complete());

        this.writer = taskWriterFactory.create();
    }

    @Override
    public void processElement(StreamRecord<T> element) throws Exception {
        writer.write(element.getValue());

        if (metricData != null) {
            metricData.invokeWithEstimate(element.getValue());
        }
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }

    @Override
    public void endInput() throws IOException {
        // For bounded stream, it may don't enable the checkpoint mechanism so we'd better to emit the remaining
        // completed files to downstream before closing the writer so that we won't miss any of them.
        emit(writer.complete());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("table_name", fullTableName)
                .add("subtask_id", subTaskId)
                .add("attempt_id", attemptId)
                .toString();
    }

    private void emit(WriteResult result) {
        output.collect(new StreamRecord<>(result));
    }
}
