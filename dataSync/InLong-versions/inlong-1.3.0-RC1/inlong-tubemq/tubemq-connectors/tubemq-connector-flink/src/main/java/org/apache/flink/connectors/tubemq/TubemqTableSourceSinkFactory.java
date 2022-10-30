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

import static org.apache.flink.table.descriptors.ConnectorDescriptorValidator.CONNECTOR_PROPERTY_VERSION;
import static org.apache.flink.table.descriptors.ConnectorDescriptorValidator.CONNECTOR_TYPE;
import static org.apache.flink.table.descriptors.FormatDescriptorValidator.FORMAT;
import static org.apache.flink.table.descriptors.Rowtime.ROWTIME_TIMESTAMPS_CLASS;
import static org.apache.flink.table.descriptors.Rowtime.ROWTIME_TIMESTAMPS_FROM;
import static org.apache.flink.table.descriptors.Rowtime.ROWTIME_TIMESTAMPS_SERIALIZED;
import static org.apache.flink.table.descriptors.Rowtime.ROWTIME_TIMESTAMPS_TYPE;
import static org.apache.flink.table.descriptors.Rowtime.ROWTIME_WATERMARKS_CLASS;
import static org.apache.flink.table.descriptors.Rowtime.ROWTIME_WATERMARKS_DELAY;
import static org.apache.flink.table.descriptors.Rowtime.ROWTIME_WATERMARKS_SERIALIZED;
import static org.apache.flink.table.descriptors.Rowtime.ROWTIME_WATERMARKS_TYPE;
import static org.apache.flink.table.descriptors.Schema.SCHEMA;
import static org.apache.flink.table.descriptors.Schema.SCHEMA_FROM;
import static org.apache.flink.table.descriptors.Schema.SCHEMA_NAME;
import static org.apache.flink.table.descriptors.Schema.SCHEMA_PROCTIME;
import static org.apache.flink.table.descriptors.Schema.SCHEMA_TYPE;
import static org.apache.flink.table.descriptors.StreamTableDescriptorValidator.UPDATE_MODE;
import static org.apache.flink.table.descriptors.StreamTableDescriptorValidator.UPDATE_MODE_VALUE_APPEND;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.descriptors.SchemaValidator;
import org.apache.flink.table.factories.DeserializationSchemaFactory;
import org.apache.flink.table.factories.SerializationSchemaFactory;
import org.apache.flink.table.factories.StreamTableSinkFactory;
import org.apache.flink.table.factories.StreamTableSourceFactory;
import org.apache.flink.table.factories.TableFactoryService;
import org.apache.flink.table.sinks.StreamTableSink;
import org.apache.flink.table.sources.RowtimeAttributeDescriptor;
import org.apache.flink.table.sources.StreamTableSource;
import org.apache.flink.types.Row;

/**
 * Factory for creating configured instances of {@link TubemqTableSource}.
 */
public class TubemqTableSourceSinkFactory implements StreamTableSourceFactory<Row>,
    StreamTableSinkFactory<Row> {

    private static final String SPLIT_COMMA = ",";

    public TubemqTableSourceSinkFactory() {
    }

    @Override
    public Map<String, String> requiredContext() {

        Map<String, String> context = new HashMap<>();
        context.put(UPDATE_MODE, UPDATE_MODE_VALUE_APPEND);
        context.put(CONNECTOR_TYPE, TubemqValidator.CONNECTOR_TYPE_VALUE_TUBEMQ);
        context.put(CONNECTOR_PROPERTY_VERSION, "1");

        return context;
    }

    @Override
    public List<String> supportedProperties() {
        List<String> properties = new ArrayList<>();

        // tubemq
        properties.add(TubemqValidator.CONNECTOR_TOPIC);
        properties.add(TubemqValidator.CONNECTOR_MASTER);
        properties.add(TubemqValidator.CONNECTOR_GROUP);
        properties.add(TubemqValidator.CONNECTOR_TIDS);
        properties.add(TubemqValidator.CONNECTOR_PROPERTIES + ".*");

        // schema
        properties.add(SCHEMA + ".#." + SCHEMA_TYPE);
        properties.add(SCHEMA + ".#." + SCHEMA_NAME);
        properties.add(SCHEMA + ".#." + SCHEMA_FROM);

        // time attributes
        properties.add(SCHEMA + ".#." + SCHEMA_PROCTIME);
        properties.add(SCHEMA + ".#." + ROWTIME_TIMESTAMPS_TYPE);
        properties.add(SCHEMA + ".#." + ROWTIME_TIMESTAMPS_FROM);
        properties.add(SCHEMA + ".#." + ROWTIME_TIMESTAMPS_CLASS);
        properties.add(SCHEMA + ".#." + ROWTIME_TIMESTAMPS_SERIALIZED);
        properties.add(SCHEMA + ".#." + ROWTIME_WATERMARKS_TYPE);
        properties.add(SCHEMA + ".#." + ROWTIME_WATERMARKS_CLASS);
        properties.add(SCHEMA + ".#." + ROWTIME_WATERMARKS_SERIALIZED);
        properties.add(SCHEMA + ".#." + ROWTIME_WATERMARKS_DELAY);

        // format wildcard
        properties.add(FORMAT + ".*");

        return properties;
    }

    @Override
    public StreamTableSource<Row> createStreamTableSource(
        Map<String, String> properties
    ) {
        final DeserializationSchema<Row> deserializationSchema =
            getDeserializationSchema(properties);

        final DescriptorProperties descriptorProperties =
            new DescriptorProperties(true);
        descriptorProperties.putProperties(properties);

        validateProperties(descriptorProperties);

        final TableSchema schema =
            descriptorProperties.getTableSchema(SCHEMA);
        final Optional<String> proctimeAttribute =
            SchemaValidator.deriveProctimeAttribute(descriptorProperties);
        final List<RowtimeAttributeDescriptor> rowtimeAttributeDescriptors =
            SchemaValidator.deriveRowtimeAttributes(descriptorProperties);
        final Map<String, String> fieldMapping =
            SchemaValidator.deriveFieldMapping(
                descriptorProperties,
                Optional.of(deserializationSchema.getProducedType()));
        final String topic =
            descriptorProperties.getString(TubemqValidator.CONNECTOR_TOPIC);
        final String masterAddress =
            descriptorProperties.getString(TubemqValidator.CONNECTOR_MASTER);
        final String consumerGroup =
            descriptorProperties.getString(TubemqValidator.CONNECTOR_GROUP);
        final String tids =
            descriptorProperties
                .getOptionalString(TubemqValidator.CONNECTOR_TIDS)
                .orElse(null);
        final Configuration configuration =
            getConfiguration(descriptorProperties);

        TreeSet<String> tidSet = new TreeSet<>();
        if (tids != null) {
            tidSet.addAll(Arrays.asList(tids.split(SPLIT_COMMA)));
        }

        return new TubemqTableSource(
            deserializationSchema,
            schema,
            proctimeAttribute,
            rowtimeAttributeDescriptors,
            fieldMapping,
            masterAddress,
            topic,
            tidSet,
            consumerGroup,
            configuration
        );
    }

    @Override
    public StreamTableSink<Row> createStreamTableSink(
        Map<String, String> properties
    ) {
        final SerializationSchema<Row> serializationSchema =
            getSerializationSchema(properties);

        final DescriptorProperties descriptorProperties =
            new DescriptorProperties(true);
        descriptorProperties.putProperties(properties);

        validateProperties(descriptorProperties);

        final TableSchema tableSchema =
            descriptorProperties.getTableSchema(SCHEMA);
        final String topic =
            descriptorProperties.getString(TubemqValidator.CONNECTOR_TOPIC);
        final String masterAddress =
            descriptorProperties.getString(TubemqValidator.CONNECTOR_MASTER);

        final Configuration configuration =
            getConfiguration(descriptorProperties);

        return new TubemqTableSink(
            serializationSchema,
            tableSchema,
            topic,
            masterAddress,
            configuration
        );
    }

    private SerializationSchema<Row> getSerializationSchema(
        Map<String, String> properties
    ) {
        @SuppressWarnings("unchecked")
        final SerializationSchemaFactory<Row> formatFactory =
            TableFactoryService.find(
                SerializationSchemaFactory.class,
                properties,
                this.getClass().getClassLoader()
            );

        return formatFactory.createSerializationSchema(properties);
    }

    private void validateProperties(DescriptorProperties descriptorProperties) {
        new SchemaValidator(true, false, false).validate(descriptorProperties);
        new TubemqValidator().validate(descriptorProperties);
    }

    private DeserializationSchema<Row> getDeserializationSchema(
        Map<String, String> properties
    ) {
        @SuppressWarnings("unchecked") final DeserializationSchemaFactory<Row> formatFactory =
            TableFactoryService.find(
                DeserializationSchemaFactory.class,
                properties,
                this.getClass().getClassLoader()
            );

        return formatFactory.createDeserializationSchema(properties);
    }

    private Configuration getConfiguration(
        DescriptorProperties descriptorProperties
    ) {
        Map<String, String> properties =
            descriptorProperties.getPropertiesWithPrefix(TubemqValidator.CONNECTOR_PROPERTIES);

        Configuration configuration = new Configuration();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            configuration.setString(property.getKey(), property.getValue());
        }

        return configuration;
    }
}
