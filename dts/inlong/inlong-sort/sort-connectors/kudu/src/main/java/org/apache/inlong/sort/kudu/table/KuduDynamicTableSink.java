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

package org.apache.inlong.sort.kudu.table;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.sink.DataStreamSinkProvider;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.utils.TableConnectorUtils;
import org.apache.inlong.sort.kudu.common.KuduOptions;
import org.apache.inlong.sort.kudu.common.KuduTableInfo;
import org.apache.inlong.sort.kudu.sink.KuduAsyncSinkFunction;
import org.apache.inlong.sort.kudu.sink.KuduSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

import static org.apache.inlong.sort.kudu.common.KuduOptions.IGNORE_ALL_CHANGELOG;

/**
 * The KuduLookupFunction is a standard user-defined table function, it can be
 * used in tableAPI and also useful for temporal table join plan in SQL.
 */
public class KuduDynamicTableSink implements DynamicTableSink {

    private static final Logger LOG = LoggerFactory.getLogger(KuduDynamicTableSink.class);

    /**
     * The configuration for the kudu sink.
     */
    private final Configuration configuration;
    private final String inlongMetric;
    private final String auditHostAndPorts;
    private final KuduTableInfo kuduTableInfo;

    /**
     * True if the data stream consumed by this sink is append-only.
     */
    private boolean isAppendOnly;

    /**
     * The names of the key fields of the upsert stream consumed by this sink.
     */
    @Nullable
    private String[] keyFieldNames;
    private final boolean ignoreAllChangeLog;

    public KuduDynamicTableSink(
            KuduTableInfo kuduTableInfo,
            Configuration configuration,
            String inlongMetric,
            String auditHostAndPorts) {
        this.kuduTableInfo = kuduTableInfo;
        this.configuration = configuration;
        this.inlongMetric = inlongMetric;
        this.auditHostAndPorts = auditHostAndPorts;

        ignoreAllChangeLog = configuration.getBoolean(IGNORE_ALL_CHANGELOG);

        String userKeyFieldsConfig = configuration.getString(KuduOptions.SINK_KEY_FIELD_NAMES);
        if (userKeyFieldsConfig != null) {
            userKeyFieldsConfig = userKeyFieldsConfig.trim();
            if (!userKeyFieldsConfig.isEmpty()) {
                this.keyFieldNames = userKeyFieldsConfig.split("\\s*,\\s*");
            }
        }
    }

    public DataStreamSink<?> consumeStream(DataStream<RowData> dataStream) {

        SinkFunction<RowData> kuduSinkFunction = createSinkFunction();

        return dataStream
                .addSink(kuduSinkFunction)
                .setParallelism(dataStream.getParallelism())
                .name(TableConnectorUtils.generateRuntimeName(this.getClass(), kuduTableInfo.getFieldNames()));
    }

    private SinkFunction<RowData> createSinkFunction() {
        boolean sinkWithAsyncMode = configuration.getBoolean(KuduOptions.SINK_WRITE_WITH_ASYNC_MODE);
        if (sinkWithAsyncMode) {
            return new KuduAsyncSinkFunction(
                    kuduTableInfo,
                    configuration,
                    inlongMetric,
                    auditHostAndPorts);
        } else {
            return new KuduSinkFunction(
                    kuduTableInfo,
                    configuration,
                    inlongMetric,
                    auditHostAndPorts);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KuduDynamicTableSink that = (KuduDynamicTableSink) o;
        return Objects.equals(kuduTableInfo, that.kuduTableInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kuduTableInfo);
    }

    /**
     * Compare key fields given by flink planner and key fields specified by user.
     *
     * @param plannerKeyFields Key fields given by flink planner.
     * @param userKeyFields Key fields specified by user via {@link KuduOptions#SINK_KEY_FIELD_NAMES}.
     */
    private void compareKeyFields(String[] plannerKeyFields, String[] userKeyFields) {
        if (plannerKeyFields == null || plannerKeyFields.length == 0) {
            return;
        }
        if (userKeyFields == null || userKeyFields.length == 0) {
            return;
        }

        Set<String> assumedSet = new HashSet<>(Arrays.asList(plannerKeyFields));
        Set<String> userSet = new HashSet<>(Arrays.asList(userKeyFields));

        if (!assumedSet.equals(userSet)) {
            String errorMsg = String.format(
                    "Key fields provided by flink [%s] are not the same as key fields " +
                            "provided by user [%s]. Please adjust your key fields settings, or " +
                            "set %s to false.",
                    assumedSet, userSet, KuduOptions.ENABLE_KEY_FIELD_CHECK.key());
            throw new ValidationException(errorMsg);
        }
    }

    @Override
    public ChangelogMode getChangelogMode(ChangelogMode requestedMode) {
        if (ignoreAllChangeLog) {
            LOG.warn("Kudu sink receive all changelog record. "
                    + "Regard any other record as insert-only record.");
            return ChangelogMode.all();
        }
        return ChangelogMode.all();
    }

    @Override
    public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {
        return new DataStreamSinkProvider() {

            @Override
            public DataStreamSink<?> consumeDataStream(DataStream<RowData> dataStream) {
                int parallelism = dataStream.getParallelism();
                return consumeStream(dataStream);
            }
        };
    }

    @Override
    public DynamicTableSink copy() {
        return new KuduDynamicTableSink(
                kuduTableInfo,
                configuration,
                inlongMetric,
                auditHostAndPorts);
    }

    @Override
    public String asSummaryString() {
        return "KuduSink";
    }
}
