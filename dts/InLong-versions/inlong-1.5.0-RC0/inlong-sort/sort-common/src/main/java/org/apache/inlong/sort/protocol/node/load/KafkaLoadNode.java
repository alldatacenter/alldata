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
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude.Include;
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
import org.apache.inlong.sort.protocol.node.format.RawFormat;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.apache.inlong.sort.protocol.constant.KafkaConstant.CONNECTOR;
import static org.apache.inlong.sort.protocol.constant.KafkaConstant.KAFKA;
import static org.apache.inlong.sort.protocol.constant.KafkaConstant.PROPERTIES_BOOTSTRAP_SERVERS;
import static org.apache.inlong.sort.protocol.constant.KafkaConstant.RAW_HASH;
import static org.apache.inlong.sort.protocol.constant.KafkaConstant.SINK_IGNORE_CHANGELOG;
import static org.apache.inlong.sort.protocol.constant.KafkaConstant.SINK_MULTIPLE_FORMAT;
import static org.apache.inlong.sort.protocol.constant.KafkaConstant.SINK_MULTIPLE_PARTITION_PATTERN;
import static org.apache.inlong.sort.protocol.constant.KafkaConstant.SINK_PARALLELISM;
import static org.apache.inlong.sort.protocol.constant.KafkaConstant.SINK_PARTITIONER;
import static org.apache.inlong.sort.protocol.constant.KafkaConstant.TOPIC;
import static org.apache.inlong.sort.protocol.constant.KafkaConstant.TOPIC_PATTERN;
import static org.apache.inlong.sort.protocol.constant.KafkaConstant.UPSERT_KAFKA;

/**
 * Kafka load node using kafka connectors provided by flink
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("kafkaLoad")
@JsonInclude(Include.NON_NULL)
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
    @Nullable
    @JsonProperty("topicPattern")
    private String topicPattern;
    @Nullable
    @JsonProperty("sinkMultipleFormat")
    private Format sinkMultipleFormat;
    @Nullable
    @JsonProperty("sinkPartitioner")
    private String sinkPartitioner;
    @Nullable
    @JsonProperty("partitionPattern")
    private String partitionPattern;

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
        this(id, name, fields, fieldRelations, filters, filterStrategy, topic, bootstrapServers, format,
                sinkParallelism, properties, primaryKey, null, null,
                null, null);
    }

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
            @JsonProperty("primaryKey") String primaryKey,
            @Nullable @JsonProperty("sinkMultipleFormat") Format sinkMultipleFormat,
            @Nullable @JsonProperty("topicPattern") String topicPattern,
            @Nullable @JsonProperty("sinkPartitioner") String sinkPartitioner,
            @Nullable @JsonProperty("partitionPattern") String partitionPattern) {
        super(id, name, fields, fieldRelations, filters, filterStrategy, sinkParallelism, properties);
        this.topic = Preconditions.checkNotNull(topic, "topic is null");
        this.bootstrapServers = Preconditions.checkNotNull(bootstrapServers, "bootstrapServers is null");
        this.format = Preconditions.checkNotNull(format, "format is null");
        this.primaryKey = primaryKey;
        this.sinkMultipleFormat = sinkMultipleFormat;
        this.topicPattern = topicPattern;
        this.sinkPartitioner = sinkPartitioner;
        if (RAW_HASH.equals(sinkPartitioner)) {
            this.partitionPattern = Preconditions.checkNotNull(partitionPattern,
                    "partitionPattern is null when the sinkPartitioner is 'raw-hash'");
        } else {
            this.partitionPattern = partitionPattern;
        }
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
        options.put(TOPIC, topic);
        options.put(PROPERTIES_BOOTSTRAP_SERVERS, bootstrapServers);
        if (getSinkParallelism() != null) {
            options.put(SINK_PARALLELISM, getSinkParallelism().toString());
        }
        if (format instanceof JsonFormat || format instanceof AvroFormat
                || format instanceof CsvFormat || format instanceof RawFormat) {
            if (StringUtils.isEmpty(this.primaryKey)) {
                options.put(CONNECTOR, KAFKA);
                options.put(SINK_IGNORE_CHANGELOG, "true");
                options.putAll(format.generateOptions(false));
            } else {
                options.put(CONNECTOR, UPSERT_KAFKA);
                options.putAll(format.generateOptions(true));
            }
            if (format instanceof RawFormat) {
                if (sinkMultipleFormat != null) {
                    options.put(SINK_MULTIPLE_FORMAT, sinkMultipleFormat.identifier());
                }
                if (StringUtils.isNotBlank(topicPattern)) {
                    options.put(TOPIC_PATTERN, topicPattern);
                }
                if (StringUtils.isNotBlank(sinkPartitioner)) {
                    options.put(SINK_PARTITIONER, sinkPartitioner);
                }
                if (StringUtils.isNotBlank(partitionPattern)) {
                    options.put(SINK_MULTIPLE_PARTITION_PATTERN, partitionPattern);
                }
            }
        } else if (format instanceof CanalJsonFormat || format instanceof DebeziumJsonFormat) {
            options.put(CONNECTOR, KAFKA);
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
                metadataKey = "value.type";
                break;
            case DATA:
            case DATA_CANAL:
                metadataKey = "value.data_canal";
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
        return EnumSet.of(MetaField.PROCESS_TIME, MetaField.TABLE_NAME, MetaField.OP_TYPE,
                MetaField.DATABASE_NAME, MetaField.SQL_TYPE, MetaField.PK_NAMES, MetaField.TS,
                MetaField.OP_TS, MetaField.IS_DDL, MetaField.MYSQL_TYPE, MetaField.BATCH_ID,
                MetaField.UPDATE_BEFORE, MetaField.DATA_CANAL, MetaField.DATA);
    }
}
