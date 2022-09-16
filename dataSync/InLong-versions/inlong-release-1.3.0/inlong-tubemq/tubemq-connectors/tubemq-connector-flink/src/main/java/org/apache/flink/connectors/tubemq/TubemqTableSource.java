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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.Types;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.sources.DefinedFieldMapping;
import org.apache.flink.table.sources.DefinedProctimeAttribute;
import org.apache.flink.table.sources.DefinedRowtimeAttributes;
import org.apache.flink.table.sources.RowtimeAttributeDescriptor;
import org.apache.flink.table.sources.StreamTableSource;
import org.apache.flink.types.Row;

/**
 * TubeMQ {@link StreamTableSource}.
 */
public class TubemqTableSource implements
    StreamTableSource<Row>,
    DefinedProctimeAttribute,
    DefinedRowtimeAttributes,
    DefinedFieldMapping {

    /**
     * Deserialization schema for records from TubeMQ.
     */
    private final DeserializationSchema<Row> deserializationSchema;

    /**
     * The schema of the table.
     */
    private final TableSchema schema;

    /**
     * Field name of the processing time attribute, null if no processing time
     * field is defined.
     */
    private final Optional<String> proctimeAttribute;

    /**
     * Descriptors for rowtime attributes.
     */
    private final List<RowtimeAttributeDescriptor> rowtimeAttributeDescriptors;

    /**
     * Mapping for the fields of the table schema to fields of the physical
     * returned type.
     */
    private final Map<String, String> fieldMapping;

    /**
     * The address of TubeMQ master, format eg: 127.0.0.1:8080,127.0.0.2:8081 .
     */
    private final String masterAddress;

    /**
     * The TubeMQ topic name.
     */
    private final String topic;

    /**
     * The TubeMQ tid filter collection.
     */
    private final TreeSet<String> tidSet;

    /**
     * The TubeMQ consumer group name.
     */
    private final String consumerGroup;

    /**
     * The parameters collection for TubeMQ consumer.
     */
    private final Configuration configuration;

    /**
     * Build TubeMQ table source
     *
     * @param deserializationSchema   the deserialize schema
     * @param schema             the data schema
     * @param proctimeAttribute              the proc time
     * @param rowtimeAttributeDescriptors    the row time attribute descriptor
     * @param fieldMapping        the field map information
     * @param masterAddress       the master address
     * @param topic               the topic name
     * @param tidSet              the topic's filter condition items
     * @param consumerGroup       the consumer group
     * @param configuration       the configure
     */
    public TubemqTableSource(
        DeserializationSchema<Row> deserializationSchema,
        TableSchema schema,
        Optional<String> proctimeAttribute,
        List<RowtimeAttributeDescriptor> rowtimeAttributeDescriptors,
        Map<String, String> fieldMapping,
        String masterAddress,
        String topic,
        TreeSet<String> tidSet,
        String consumerGroup,
        Configuration configuration
    ) {
        checkNotNull(deserializationSchema,
            "The deserialization schema must not be null.");
        checkNotNull(schema,
            "The schema must not be null.");
        checkNotNull(fieldMapping,
            "The field mapping must not be null.");
        checkNotNull(masterAddress,
            "The master address must not be null.");
        checkNotNull(topic,
            "The topic must not be null.");
        checkNotNull(tidSet,
            "The tid set must not be null.");
        checkNotNull(consumerGroup,
            "The consumer group must not be null.");
        checkNotNull(configuration,
            "The configuration must not be null.");

        this.deserializationSchema = deserializationSchema;
        this.schema = schema;
        this.fieldMapping = fieldMapping;
        this.masterAddress = masterAddress;
        this.topic = topic;
        this.tidSet = tidSet;
        this.consumerGroup = consumerGroup;
        this.configuration = configuration;

        this.proctimeAttribute =
            validateProcTimeAttribute(proctimeAttribute);
        this.rowtimeAttributeDescriptors =
            validateRowTimeAttributeDescriptors(rowtimeAttributeDescriptors);
    }

    @Override
    public TableSchema getTableSchema() {
        return schema;
    }

    @Nullable
    @Override
    public String getProctimeAttribute() {
        return proctimeAttribute.orElse(null);
    }

    @Override
    public List<RowtimeAttributeDescriptor> getRowtimeAttributeDescriptors() {
        return rowtimeAttributeDescriptors;
    }

    @Override
    public Map<String, String> getFieldMapping() {
        return fieldMapping;
    }

    @Override
    public DataStream<Row> getDataStream(
        StreamExecutionEnvironment streamExecutionEnvironment
    ) {
        SourceFunction<Row> sourceFunction =
            new TubemqSourceFunction<>(
                masterAddress,
                topic,
                tidSet,
                consumerGroup,
                deserializationSchema,
                configuration
            );

        return streamExecutionEnvironment
            .addSource(sourceFunction)
            .name(explainSource());
    }

    private Optional<String> validateProcTimeAttribute(
        Optional<String> proctimeAttribute
    ) {
        return proctimeAttribute.map((attribute) -> {
            Optional<TypeInformation<?>> tpe = schema.getFieldType(attribute);
            if (!tpe.isPresent()) {
                throw new ValidationException("Proc time attribute '"
                        + attribute + "' isn't present in TableSchema.");
            } else if (tpe.get() != Types.SQL_TIMESTAMP()) {
                throw new ValidationException("Proc time attribute '"
                        + attribute + "' isn't of type SQL_TIMESTAMP.");
            }
            return attribute;
        });
    }

    private List<RowtimeAttributeDescriptor> validateRowTimeAttributeDescriptors(
        List<RowtimeAttributeDescriptor> attributeDescriptors
    ) {
        checkNotNull(attributeDescriptors);

        for (RowtimeAttributeDescriptor desc : attributeDescriptors) {
            String name = desc.getAttributeName();
            Optional<TypeInformation<?>> tpe = schema.getFieldType(name);
            if (!tpe.isPresent()) {
                throw new ValidationException("Row time attribute '"
                        + name + "' is not present.");
            } else if (tpe.get() != Types.SQL_TIMESTAMP()) {
                throw new ValidationException("Row time attribute '"
                        + name + "' is not of type SQL_TIMESTAMP.");
            }
        }

        return attributeDescriptors;
    }
}
