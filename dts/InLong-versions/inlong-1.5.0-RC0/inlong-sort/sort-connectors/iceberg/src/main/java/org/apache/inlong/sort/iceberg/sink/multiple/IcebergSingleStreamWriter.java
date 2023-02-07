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

package org.apache.inlong.sort.iceberg.sink.multiple;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.table.types.logical.RowType;
import org.apache.iceberg.flink.sink.TaskWriterFactory;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;
import org.apache.inlong.sort.base.dirty.DirtyData;
import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.base.metric.MetricOption.RegisteredMetric;
import org.apache.inlong.sort.base.metric.MetricState;
import org.apache.inlong.sort.base.metric.SinkMetricData;
import org.apache.inlong.sort.base.util.MetricStateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;

import static org.apache.inlong.sort.base.Constants.DIRTY_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.DIRTY_RECORDS_OUT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC_STATE_NAME;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_OUT;

public class IcebergSingleStreamWriter<T> extends IcebergProcessFunction<T, WriteResult>
        implements
            CheckpointedFunction,
            SchemaEvolutionFunction<TaskWriterFactory<T>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcebergSingleStreamWriter.class);

    private static final long serialVersionUID = 1L;

    private final String fullTableName;
    private final String inlongMetric;
    private final String auditHostAndPorts;
    private TaskWriterFactory<T> taskWriterFactory;

    private transient TaskWriter<T> writer;
    private transient int subTaskId;
    private transient int attemptId;
    private @Nullable transient SinkMetricData metricData;
    private transient ListState<MetricState> metricStateListState;
    private transient MetricState metricState;
    private @Nullable RowType flinkRowType;
    private final DirtyOptions dirtyOptions;
    private @Nullable final DirtySink<Object> dirtySink;

    public IcebergSingleStreamWriter(
            String fullTableName,
            TaskWriterFactory<T> taskWriterFactory,
            String inlongMetric,
            String auditHostAndPorts,
            @Nullable RowType flinkRowType,
            DirtyOptions dirtyOptions,
            @Nullable DirtySink<Object> dirtySink) {
        this.fullTableName = fullTableName;
        this.taskWriterFactory = taskWriterFactory;
        this.inlongMetric = inlongMetric;
        this.auditHostAndPorts = auditHostAndPorts;
        this.flinkRowType = flinkRowType;
        this.dirtyOptions = dirtyOptions;
        this.dirtySink = dirtySink;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        this.subTaskId = getRuntimeContext().getIndexOfThisSubtask();
        this.attemptId = getRuntimeContext().getAttemptNumber();

        // Initialize the task writer factory.
        this.taskWriterFactory.initialize(subTaskId, attemptId);
        // Initialize the task writer.
        this.writer = taskWriterFactory.create();

        // Initialize metric
        MetricOption metricOption = MetricOption.builder()
                .withInlongLabels(inlongMetric)
                .withInlongAudit(auditHostAndPorts)
                .withInitRecords(metricState != null ? metricState.getMetricValue(NUM_RECORDS_OUT) : 0L)
                .withInitBytes(metricState != null ? metricState.getMetricValue(NUM_BYTES_OUT) : 0L)
                .withInitDirtyRecords(metricState != null ? metricState.getMetricValue(DIRTY_RECORDS_OUT) : 0L)
                .withInitDirtyBytes(metricState != null ? metricState.getMetricValue(DIRTY_BYTES_OUT) : 0L)
                .withRegisterMetric(RegisteredMetric.ALL)
                .build();
        if (metricOption != null) {
            metricData = new SinkMetricData(metricOption, getRuntimeContext().getMetricGroup());
        }
    }

    @Override
    public void prepareSnapshotPreBarrier(long checkpointId) throws Exception {
        // close all open files and emit files to downstream committer operator
        emit(writer.complete());
        this.writer = taskWriterFactory.create();
    }

    @Override
    public void processElement(T value) throws Exception {
        try {
            writer.write(value);
        } catch (Exception e) {
            LOGGER.error(String.format("write error, raw data: %s", value), e);
            if (!dirtyOptions.ignoreDirty()) {
                throw e;
            }
            if (dirtySink != null) {
                DirtyData.Builder<Object> builder = DirtyData.builder();
                try {
                    builder.setData(value)
                            .setLabels(dirtyOptions.getLabels())
                            .setLogTag(dirtyOptions.getLogTag())
                            .setIdentifier(dirtyOptions.getIdentifier())
                            .setRowType(flinkRowType)
                            .setDirtyMessage(e.getMessage());
                    dirtySink.invoke(builder.build());
                    if (metricData != null) {
                        metricData.invokeDirtyWithEstimate(value);
                    }
                } catch (Exception ex) {
                    if (!dirtyOptions.ignoreSideOutputErrors()) {
                        throw new RuntimeException(ex);
                    }
                    LOGGER.warn("Dirty sink failed", ex);
                }
            }
        }
        if (metricData != null) {
            metricData.invokeWithEstimate(value == null ? "" : value);
        }
    }

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        // init metric state
        if (this.inlongMetric != null) {
            this.metricStateListState = context.getOperatorStateStore().getUnionListState(
                    new ListStateDescriptor<>(
                            String.format("Iceberg(%s)-" + INLONG_METRIC_STATE_NAME, fullTableName),
                            TypeInformation.of(new TypeHint<MetricState>() {
                            })));
        }
        if (context.isRestored()) {
            metricState = MetricStateUtils.restoreMetricState(metricStateListState,
                    getRuntimeContext().getIndexOfThisSubtask(), getRuntimeContext().getNumberOfParallelSubtasks());
        }
    }

    public void setFlinkRowType(@Nullable RowType flinkRowType) {
        this.flinkRowType = flinkRowType;
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        if (metricData != null && metricStateListState != null) {
            MetricStateUtils.snapshotMetricStateForSinkMetricData(metricStateListState, metricData,
                    getRuntimeContext().getIndexOfThisSubtask());
        }
    }

    @Override
    public void dispose() throws Exception {
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
    public void schemaEvolution(TaskWriterFactory<T> schema) throws IOException {
        emit(writer.complete());

        taskWriterFactory = schema;
        taskWriterFactory.initialize(subTaskId, attemptId);
        writer = taskWriterFactory.create();
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
        collector.collect(result);
    }
}
