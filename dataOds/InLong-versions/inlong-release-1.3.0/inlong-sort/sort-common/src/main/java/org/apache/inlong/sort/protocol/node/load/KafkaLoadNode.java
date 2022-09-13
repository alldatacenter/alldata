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

package org.apache.inlong.sort.protocol.node.load;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.common.enums.MetaField;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.InlongMetric;
import org.apache.inlong.sort.protocol.Metadata;
import org.apache.inlong.sort.protocol.enums.FilterStrategy;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.format.AvroFormat;
import org.apache.inlong.sort.protocol.node.format.CanalJsonFormat;
import org.apache.inlong.sort.protocol.node.format.CsvFormat;
import org.apache.inlong.sort.protocol.node.format.DebeziumJsonFormat;
import org.apache.inlong.sort.protocol.node.format.Format;
import org.apache.inlong.sort.protocol.node.format.JsonFormat;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kafka load node using kafka connectors provided by flink
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("kafkaLoad")
@Data
@NoArgsConstructor
public class KafkaLoadNode extends LoadNode implements InlongMetric, Metadata, Serializable {


    private static final long serialVersionUID = -558158965060708408L;

    @Nonnull
    @JsonProperty("topic")
    private String topic;
    @Nonnull
    @JsonProperty("bootstrapServers")
    private String bootstrapServers;
    @Nonnull
    @JsonProperty("format")
    private Format format;

    @JsonProperty("primaryKey")
    private String primaryKey;

    @JsonCreator
    public KafkaLoadNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @JsonProperty("fieldRelations") List<FieldRelation> fieldRelations,
            @JsonProperty("filters") List<FilterFunction> filters,
            @JsonProperty("filterStrategy") FilterStrategy filterStrategy,
            @Nonnull @JsonProperty("topic") String topic,
            @Nonnull @JsonProperty("bootstrapServers") String bootstrapServers,
            @Nonnull @JsonProperty("format") Format format,
            @Nullable @JsonProperty("sinkParallelism") Integer sinkParallelism,
            @JsonProperty("properties") Map<String, String> properties,
            @JsonProperty("primaryKey") String primaryKey) {
        super(id, name, fields, fieldRelations, filters, filterStrategy, sinkParallelism, properties);
        this.topic = Preconditions.checkNotNull(topic, "topic is null");
        this.bootstrapServers = Preconditions.checkNotNull(bootstrapServers, "bootstrapServers is null");
        this.format = Preconditions.checkNotNull(format, "format is null");
        this.primaryKey = primaryKey;
    }

    @Override
    public String genTableName() {
        return "node_" + super.getId() + "_" + topic;
    }

    /**
     * Generate options for kafka connector
     *
     * @return options
     */
    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> options = super.tableOptions();
        options.put("topic", topic);
        options.put("properties.bootstrap.servers", bootstrapServers);
        if (getSinkParallelism() != null) {
            options.put("sink.parallelism", getSinkParallelism().toString());
        }
        if (format instanceof JsonFormat || format instanceof AvroFormat || format instanceof CsvFormat) {
            if (StringUtils.isEmpty(this.primaryKey)) {
                options.put("connector", "kafka-inlong");
                options.put("sink.ignore.changelog", "true");
                options.putAll(format.generateOptions(false));
            } else {
                options.put("connector", "upsert-kafka-inlong");
                options.putAll(format.generateOptions(true));
            }
        } else if (format instanceof CanalJsonFormat || format instanceof DebeziumJsonFormat) {
            options.put("connector", "kafka-inlong");
            options.putAll(format.generateOptions(false));
        } else {
            throw new IllegalArgumentException("kafka load Node format is IllegalArgument");
        }
        return options;
    }

    @Override
    public String getMetadataKey(MetaField metaField) {
        String metadataKey;
        switch (metaField) {
            case TABLE_NAME:
                metadataKey = "value.table";
                break;
            case DATABASE_NAME:
                metadataKey = "value.database";
                break;
            case SQL_TYPE:
                metadataKey = "value.sql-type";
                break;
            case PK_NAMES:
                metadataKey = "value.pk-names";
                break;
            case TS:
                metadataKey = "value.ingestion-timestamp";
                break;
            case OP_TS:
                metadataKey = "value.event-timestamp";
                break;
            case OP_TYPE:
                metadataKey = "value.op-type";
                break;
            case DATA:
                metadataKey = "value.data";
                break;
            case IS_DDL:
                metadataKey = "value.is-ddl";
                break;
            case MYSQL_TYPE:
                metadataKey = "value.mysql-type";
                break;
            case BATCH_ID:
                metadataKey = "value.batch-id";
                break;
            case UPDATE_BEFORE:
                metadataKey = "value.update-before";
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unsupport meta field for %s: %s",
                        this.getClass().getSimpleName(), metaField));
        }
        return metadataKey;
    }

    @Override
    public boolean isVirtual(MetaField metaField) {
        return false;
    }

    @Override
    public Set<MetaField> supportedMetaFields() {
        return EnumSet.of(MetaField.PROCESS_TIME, MetaField.TABLE_NAME, MetaField.OP_TYPE, MetaField.DATABASE_NAME,
                MetaField.SQL_TYPE, MetaField.PK_NAMES, MetaField.TS, MetaField.OP_TS, MetaField.IS_DDL,
                MetaField.MYSQL_TYPE, MetaField.BATCH_ID, MetaField.UPDATE_BEFORE, MetaField.DATA);
    }
}
