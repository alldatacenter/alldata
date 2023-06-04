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

package org.apache.inlong.sort.cdc.mysql.source;

import io.debezium.jdbc.JdbcConnection;
import io.debezium.relational.TableId;
import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.flink.connector.base.source.reader.synchronization.FutureCompletingBlockingQueue;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.inlong.sort.cdc.base.debezium.DebeziumDeserializationSchema;
import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.base.metric.MetricOption.RegisteredMetric;
import org.apache.inlong.sort.cdc.mysql.MySqlValidator;
import org.apache.inlong.sort.cdc.mysql.debezium.DebeziumUtils;
import org.apache.inlong.sort.cdc.mysql.source.assigners.MySqlBinlogSplitAssigner;
import org.apache.inlong.sort.cdc.mysql.source.assigners.MySqlHybridSplitAssigner;
import org.apache.inlong.sort.cdc.mysql.source.assigners.MySqlSplitAssigner;
import org.apache.inlong.sort.cdc.mysql.source.assigners.state.BinlogPendingSplitsState;
import org.apache.inlong.sort.cdc.mysql.source.assigners.state.HybridPendingSplitsState;
import org.apache.inlong.sort.cdc.mysql.source.assigners.state.PendingSplitsState;
import org.apache.inlong.sort.cdc.mysql.source.assigners.state.PendingSplitsStateSerializer;
import org.apache.inlong.sort.cdc.mysql.source.config.MySqlSourceConfig;
import org.apache.inlong.sort.cdc.mysql.source.config.MySqlSourceConfigFactory;
import org.apache.inlong.sort.cdc.mysql.source.enumerator.MySqlSourceEnumerator;
import org.apache.inlong.sort.cdc.mysql.source.metrics.MySqlSourceReaderMetrics;
import org.apache.inlong.sort.cdc.mysql.source.reader.MySqlRecordEmitter;
import org.apache.inlong.sort.cdc.mysql.source.reader.MySqlSourceReader;
import org.apache.inlong.sort.cdc.mysql.source.reader.MySqlSourceReaderContext;
import org.apache.inlong.sort.cdc.mysql.source.reader.MySqlSplitReader;
import org.apache.inlong.sort.cdc.mysql.source.split.MySqlSplit;
import org.apache.inlong.sort.cdc.mysql.source.split.MySqlSplitSerializer;
import org.apache.inlong.sort.cdc.mysql.table.StartupMode;
import org.apache.kafka.connect.source.SourceRecord;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

import static org.apache.inlong.sort.cdc.mysql.debezium.DebeziumUtils.discoverCapturedTables;
import static org.apache.inlong.sort.cdc.mysql.debezium.DebeziumUtils.openJdbcConnection;

/**
 * The MySQL CDC Source based on FLIP-27 and Watermark Signal Algorithm which supports parallel
 * reading snapshot of table and then continue to capture data change from binlog.
 *
 * <pre>
 *     1. The source supports parallel capturing table change.
 *     2. The source supports checkpoint in split level when read snapshot data.
 *     3. The source doesn't need apply any lock of MySQL.
 * </pre>
 *
 * <pre>{@code
 * MySqlSource
 *     .<String>builder()
 *     .hostname("localhost")
 *     .port(3306)
 *     .databaseList("mydb")
 *     .tableList("mydb.users")
 *     .username(username)
 *     .password(password)
 *     .serverId(5400)
 *     .deserializer(new JsonDebeziumDeserializationSchema())
 *     .build();
 * }</pre>
 *
 * <p>See {@link MySqlSourceBuilder} for more details.</p>
 *
 * @param <T> the output type of the source.
 */
@Internal
public class MySqlSource<T>
        implements
            Source<T, MySqlSplit, PendingSplitsState>,
            ResultTypeQueryable<T> {

    private static final long serialVersionUID = 1L;

    private final MySqlSourceConfigFactory configFactory;
    private final DebeziumDeserializationSchema<T> deserializationSchema;

    MySqlSource(
            MySqlSourceConfigFactory configFactory,
            DebeziumDeserializationSchema<T> deserializationSchema) {
        this.configFactory = configFactory;
        this.deserializationSchema = deserializationSchema;
    }

    /**
     * Get a MySqlParallelSourceBuilder to build a {@link MySqlSource}.
     *
     * @return a MySql parallel source builder.
     */
    @PublicEvolving
    public static <T> MySqlSourceBuilder<T> builder() {
        return new MySqlSourceBuilder<>();
    }

    public MySqlSourceConfigFactory getConfigFactory() {
        return configFactory;
    }

    @Override
    public Boundedness getBoundedness() {
        return Boundedness.CONTINUOUS_UNBOUNDED;
    }

    @Override
    public SourceReader<T, MySqlSplit> createReader(SourceReaderContext readerContext)
            throws Exception {
        final Method metricGroupMethod = readerContext.getClass().getMethod("metricGroup");
        metricGroupMethod.setAccessible(true);
        final MetricGroup metricGroup = (MetricGroup) metricGroupMethod.invoke(readerContext);
        final MySqlSourceReaderMetrics sourceReaderMetrics =
                new MySqlSourceReaderMetrics(metricGroup);
        // create source config for the given subtask (e.g. unique server id)
        MySqlSourceConfig sourceConfig =
                configFactory.createConfig(readerContext.getIndexOfSubtask());
        MetricOption metricOption = MetricOption.builder()
                .withInlongLabels(sourceConfig.getInlongMetric())
                .withAuditAddress(sourceConfig.getInlongAudit())
                .withRegisterMetric(RegisteredMetric.ALL)
                .build();
        sourceReaderMetrics.registerMetrics(metricOption);
        MySqlSourceReaderContext mySqlSourceReaderContext =
                new MySqlSourceReaderContext(readerContext);

        FutureCompletingBlockingQueue<RecordsWithSplitIds<SourceRecord>> elementsQueue =
                new FutureCompletingBlockingQueue<>();
        Supplier<MySqlSplitReader> splitReaderSupplier =
                () -> new MySqlSplitReader(
                        sourceConfig,
                        readerContext.getIndexOfSubtask(),
                        mySqlSourceReaderContext);
        return new MySqlSourceReader<>(
                elementsQueue,
                splitReaderSupplier,
                new MySqlRecordEmitter<>(
                        deserializationSchema,
                        sourceReaderMetrics,
                        sourceConfig),
                readerContext.getConfiguration(),
                mySqlSourceReaderContext,
                sourceConfig, sourceReaderMetrics);
    }

    @Override
    public SplitEnumerator<MySqlSplit, PendingSplitsState> createEnumerator(
            SplitEnumeratorContext<MySqlSplit> enumContext) {
        MySqlSourceConfig sourceConfig = configFactory.createConfig(0);

        final MySqlValidator validator = new MySqlValidator(sourceConfig);
        validator.validate();

        final MySqlSplitAssigner splitAssigner;
        if (sourceConfig.getStartupOptions().startupMode == StartupMode.INITIAL) {
            try (JdbcConnection jdbc = openJdbcConnection(sourceConfig)) {
                final List<TableId> remainingTables = discoverCapturedTables(jdbc, sourceConfig);
                boolean isTableIdCaseSensitive = DebeziumUtils.isTableIdCaseSensitive(jdbc);
                splitAssigner =
                        new MySqlHybridSplitAssigner(
                                sourceConfig,
                                enumContext.currentParallelism(),
                                remainingTables,
                                isTableIdCaseSensitive);
            } catch (Exception e) {
                throw new FlinkRuntimeException(
                        "Failed to discover captured tables for enumerator", e);
            }
        } else {
            splitAssigner = new MySqlBinlogSplitAssigner(sourceConfig);
        }

        return new MySqlSourceEnumerator(enumContext, sourceConfig, splitAssigner);
    }

    @Override
    public SplitEnumerator<MySqlSplit, PendingSplitsState> restoreEnumerator(
            SplitEnumeratorContext<MySqlSplit> enumContext, PendingSplitsState checkpoint) {
        MySqlSourceConfig sourceConfig = configFactory.createConfig(0);

        final MySqlSplitAssigner splitAssigner;
        if (checkpoint instanceof HybridPendingSplitsState) {
            splitAssigner =
                    new MySqlHybridSplitAssigner(
                            sourceConfig,
                            enumContext.currentParallelism(),
                            (HybridPendingSplitsState) checkpoint);
        } else if (checkpoint instanceof BinlogPendingSplitsState) {
            splitAssigner =
                    new MySqlBinlogSplitAssigner(
                            sourceConfig, (BinlogPendingSplitsState) checkpoint);
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported restored PendingSplitsState: " + checkpoint);
        }
        return new MySqlSourceEnumerator(enumContext, sourceConfig, splitAssigner);
    }

    @Override
    public SimpleVersionedSerializer<MySqlSplit> getSplitSerializer() {
        return MySqlSplitSerializer.INSTANCE;
    }

    @Override
    public SimpleVersionedSerializer<PendingSplitsState> getEnumeratorCheckpointSerializer() {
        return new PendingSplitsStateSerializer(getSplitSerializer());
    }

    @Override
    public TypeInformation<T> getProducedType() {
        return deserializationSchema.getProducedType();
    }
}
