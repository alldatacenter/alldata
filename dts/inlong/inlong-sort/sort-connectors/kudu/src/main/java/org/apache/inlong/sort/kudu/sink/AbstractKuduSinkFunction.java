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

package org.apache.inlong.sort.kudu.sink;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.table.data.RowData;
import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.base.metric.MetricState;
import org.apache.inlong.sort.base.metric.SinkMetricData;
import org.apache.inlong.sort.base.util.MetricStateUtils;
import org.apache.inlong.sort.kudu.common.KuduTableInfo;
import org.apache.kudu.client.KuduClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.apache.inlong.sort.base.Constants.DIRTY_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.DIRTY_RECORDS_OUT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC_STATE_NAME;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_OUT;
import static org.apache.inlong.sort.kudu.common.KuduOptions.DEFAULT_OPERATION_TIMEOUT_IN_MS;
import static org.apache.inlong.sort.kudu.common.KuduOptions.DEFAULT_SOCKET_READ_TIMEOUT_IN_MS;
import static org.apache.inlong.sort.kudu.common.KuduOptions.MAX_BUFFER_SIZE;
import static org.apache.inlong.sort.kudu.common.KuduOptions.DEFAULT_ADMIN_OPERATION_TIMEOUT_IN_MS;
import static org.apache.inlong.sort.kudu.common.KuduOptions.MAX_RETRIES;
import static org.apache.inlong.sort.kudu.common.KuduOptions.DISABLED_STATISTICS;

/**
 * The base for all kudu sinks.
 */
@PublicEvolving
public abstract class AbstractKuduSinkFunction
        extends
            RichSinkFunction<RowData>
        implements
            CheckpointedFunction {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractKuduSinkFunction.class);

    protected final KuduTableInfo kuduTableInfo;

    /**
     * The configuration of kudu sinkFunction.
     */
    protected final Configuration configuration;

    /**
     * The maximum number of buffered records.
     */
    protected final int maxBufferSize;

    /**
     * The maximum number of retries.
     */
    protected final int maxRetries;

    /**
     * True if the sink is running.
     */
    protected volatile boolean running;

    /**
     * The exception thrown in asynchronous tasks.
     */
    private transient Throwable flushThrowable;

    private SinkMetricData sinkMetricData;

    private transient ListState<MetricState> metricStateListState;
    private transient MetricState metricState;

    private final String auditHostAndPorts;

    private final String inLongMetric;

    public AbstractKuduSinkFunction(
            KuduTableInfo kuduTableInfo,
            Configuration configuration,
            String inLongMetric,
            String auditHostAndPorts) {
        this.kuduTableInfo = kuduTableInfo;
        this.configuration = configuration;
        this.maxRetries = configuration.getInteger(MAX_RETRIES);
        this.maxBufferSize = configuration.getInteger(MAX_BUFFER_SIZE);
        this.inLongMetric = inLongMetric;
        this.auditHostAndPorts = auditHostAndPorts;

    }

    @Override
    public void open(Configuration parameters) throws Exception {
        this.running = true;
        MetricOption metricOption = MetricOption.builder()
                .withInlongLabels(inLongMetric)
                .withAuditAddress(auditHostAndPorts)
                .withInitRecords(metricState != null ? metricState.getMetricValue(NUM_RECORDS_OUT) : 0L)
                .withInitBytes(metricState != null ? metricState.getMetricValue(NUM_BYTES_OUT) : 0L)
                .withInitDirtyRecords(metricState != null ? metricState.getMetricValue(DIRTY_RECORDS_OUT) : 0L)
                .withInitDirtyBytes(metricState != null ? metricState.getMetricValue(DIRTY_BYTES_OUT) : 0L)
                .withRegisterMetric(MetricOption.RegisteredMetric.ALL)
                .build();
        if (metricOption != null) {
            sinkMetricData = new SinkMetricData(metricOption, getRuntimeContext().getMetricGroup());
        }
    }
    protected KuduClient buildKuduClient() {
        KuduClient.KuduClientBuilder builder = new KuduClient.KuduClientBuilder(kuduTableInfo.getMasters());
        if (configuration.getBoolean(DISABLED_STATISTICS)) {
            builder.disableStatistics();
        }
        builder.defaultAdminOperationTimeoutMs(configuration.getLong(DEFAULT_ADMIN_OPERATION_TIMEOUT_IN_MS));
        builder.defaultOperationTimeoutMs(configuration.getLong(DEFAULT_OPERATION_TIMEOUT_IN_MS));
        builder.defaultSocketReadTimeoutMs(configuration.getLong(DEFAULT_SOCKET_READ_TIMEOUT_IN_MS));

        return builder
                .build();
    }
    @Override
    public void invoke(RowData row, Context context) throws Exception {
        addBatch(row);
        sendMetrics(row.toString().getBytes());
    }

    /**
     * Adds a record in the buffer.
     */
    protected abstract void addBatch(RowData in) throws Exception;

    @Override
    public void close() throws Exception {
        this.running = false;
    }

    @Override
    public void snapshotState(FunctionSnapshotContext functionSnapshotContext) throws Exception {
        if (sinkMetricData != null && metricStateListState != null) {
            MetricStateUtils.snapshotMetricStateForSinkMetricData(metricStateListState, sinkMetricData,
                    getRuntimeContext().getIndexOfThisSubtask());
        }
        checkError();
        // We must store the exception caught in the flushing so that the
        // task thread can be aware of the failure.
        try {
            flush();
        } catch (IOException e) {
            this.flushThrowable = e;
        }
        checkError();
    }

    protected abstract void flush() throws IOException;

    protected abstract void checkError();

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        if (this.inLongMetric != null) {
            this.metricStateListState = context.getOperatorStateStore().getUnionListState(
                    new ListStateDescriptor<>(
                            INLONG_METRIC_STATE_NAME, TypeInformation.of(new TypeHint<MetricState>() {
                            })));
        }

        if (context.isRestored()) {
            metricState = MetricStateUtils.restoreMetricState(metricStateListState,
                    getRuntimeContext().getIndexOfThisSubtask(), getRuntimeContext().getNumberOfParallelSubtasks());
        }
    }

    protected void sendMetrics(byte[] document) {
        if (sinkMetricData != null) {
            sinkMetricData.invoke(1, document.length);
        }
    }
}
