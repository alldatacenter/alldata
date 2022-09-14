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

package org.apache.flink.connectors.tubemq;

import static org.apache.flink.util.Preconditions.checkNotNull;
import java.util.Arrays;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.sinks.AppendStreamTableSink;
import org.apache.flink.table.utils.TableConnectorUtils;
import org.apache.flink.types.Row;

/**
 * Tubemq {@link org.apache.flink.table.sinks.StreamTableSink}.
 */
public class TubemqTableSink implements AppendStreamTableSink<Row> {

    /**
     * Serialization schema for records to tubemq.
     */
    private final SerializationSchema<Row> serializationSchema;

    /**
     * The schema of the table.
     */
    private final TableSchema schema;

    /**
     * The tubemq topic name.
     */
    private final String topic;

    /**
     * The address of tubemq master, format eg: 127.0.0.1:8080,127.0.0.2:8081 .
     */
    private final String masterAddress;

    /**
     * The parameters collection for tubemq producer.
     */
    private final Configuration configuration;

    public TubemqTableSink(
        SerializationSchema<Row> serializationSchema,
        TableSchema schema,
        String topic,
        String masterAddress,
        Configuration configuration
    ) {
        this.serializationSchema = checkNotNull(serializationSchema,
            "The deserialization schema must not be null.");
        this.schema = checkNotNull(schema,
            "The schema must not be null.");
        this.topic = checkNotNull(topic,
            "Topic must not be null.");
        this.masterAddress = checkNotNull(masterAddress,
            "Master address must not be null.");
        this.configuration = checkNotNull(configuration,
            "The configuration must not be null.");
    }

    @Override
    public DataStreamSink<?> consumeDataStream(DataStream<Row> dataStream) {

        final SinkFunction<Row> tubemqSinkFunction =
            new TubemqSinkFunction<>(
                topic,
                masterAddress,
                serializationSchema,
                configuration
            );

        return dataStream
            .addSink(tubemqSinkFunction)
            .name(
                TableConnectorUtils.generateRuntimeName(
                    getClass(),
                    getFieldNames()
                )
            );
    }

    @Override
    public TypeInformation<Row> getOutputType() {
        return schema.toRowType();
    }

    @Override
    public String[] getFieldNames() {
        return schema.getFieldNames();
    }

    @Override
    public TypeInformation<?>[] getFieldTypes() {
        return schema.getFieldTypes();
    }

    @Override
    public TubemqTableSink configure(String[] fieldNames, TypeInformation<?>[] fieldTypes) {
        if (!Arrays.equals(getFieldNames(), fieldNames)
            || !Arrays.equals(getFieldTypes(), fieldTypes)) {
            throw new ValidationException("Reconfiguration with different fields is not allowed. "
                    + "Expected: " + Arrays.toString(getFieldNames())
                    + " / " + Arrays.toString(getFieldTypes()) + ". "
                    + "But was: " + Arrays.toString(fieldNames) + " / "
                    + Arrays.toString(fieldTypes));
        }

        return this;
    }
}
