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

package org.apache.inlong.manager.pojo.sort.node.provider;

import org.apache.inlong.common.enums.DataTypeEnum;
import org.apache.inlong.manager.common.consts.StreamType;
import org.apache.inlong.manager.pojo.sink.kafka.KafkaSink;
import org.apache.inlong.manager.pojo.sort.node.base.ExtractNodeProvider;
import org.apache.inlong.manager.pojo.sort.node.base.LoadNodeProvider;
import org.apache.inlong.manager.pojo.source.kafka.KafkaOffset;
import org.apache.inlong.manager.pojo.source.kafka.KafkaSource;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.enums.KafkaScanStartupMode;
import org.apache.inlong.sort.protocol.node.ExtractNode;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.extract.KafkaExtractNode;
import org.apache.inlong.sort.protocol.node.format.AvroFormat;
import org.apache.inlong.sort.protocol.node.format.CanalJsonFormat;
import org.apache.inlong.sort.protocol.node.format.CsvFormat;
import org.apache.inlong.sort.protocol.node.format.DebeziumJsonFormat;
import org.apache.inlong.sort.protocol.node.format.Format;
import org.apache.inlong.sort.protocol.node.format.JsonFormat;
import org.apache.inlong.sort.protocol.node.format.RawFormat;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

/**
 * The Provider for creating Kafka extract or load nodes.
 */
public class KafkaProvider implements ExtractNodeProvider, LoadNodeProvider {

    @Override
    public Boolean accept(String streamType) {
        return StreamType.KAFKA.equals(streamType);
    }

    @Override
    public ExtractNode createExtractNode(StreamNode streamNodeInfo) {
        KafkaSource kafkaSource = (KafkaSource) streamNodeInfo;
        List<FieldInfo> fieldInfos = parseStreamFieldInfos(kafkaSource.getFieldList(), kafkaSource.getSourceName());
        Map<String, String> properties = parseProperties(kafkaSource.getProperties());

        Format format = parsingFormat(
                kafkaSource.getSerializationType(),
                kafkaSource.isWrapWithInlongMsg(),
                kafkaSource.getDataSeparator(),
                kafkaSource.isIgnoreParseErrors());

        KafkaScanStartupMode startupMode = parseStartupMode(kafkaSource.getAutoOffsetReset());
        String topic = kafkaSource.getTopic();
        String bootstrapServers = kafkaSource.getBootstrapServers();

        final String primaryKey = kafkaSource.getPrimaryKey();
        String groupId = kafkaSource.getGroupId();
        String partitionOffset = kafkaSource.getPartitionOffsets();
        String scanTimestampMillis = kafkaSource.getTimestampMillis();
        return new KafkaExtractNode(kafkaSource.getSourceName(),
                kafkaSource.getSourceName(),
                fieldInfos,
                null,
                properties,
                topic,
                bootstrapServers,
                format,
                startupMode,
                primaryKey,
                groupId,
                partitionOffset,
                scanTimestampMillis);
    }

    @Override
    public LoadNode createLoadNode(StreamNode nodeInfo, Map<String, StreamField> constantFieldMap) {
        KafkaSink kafkaSink = (KafkaSink) nodeInfo;
        Map<String, String> properties = parseProperties(kafkaSink.getProperties());
        List<FieldInfo> fieldInfos = parseSinkFieldInfos(kafkaSink.getSinkFieldList(), kafkaSink.getSinkName());
        List<FieldRelation> fieldRelations = parseSinkFields(kafkaSink.getSinkFieldList(), constantFieldMap);

        Integer sinkParallelism = kafkaSink.getPartitionNum();
        Format format = parseFormat(kafkaSink.getSerializationType());

        return new KafkaLoadNode(
                kafkaSink.getSinkName(),
                kafkaSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                Lists.newArrayList(),
                null,
                kafkaSink.getTopicName(),
                kafkaSink.getBootstrapServers(),
                format,
                sinkParallelism,
                properties,
                kafkaSink.getPrimaryKey());
    }

    /**
     * parse Startup Mode
     *
     * @param autoOffsetReset The strategy of auto offset reset, including earliest, specific, latest (the
     *         default), none
     * @return kafka scan startup mode
     */
    private KafkaScanStartupMode parseStartupMode(String autoOffsetReset) {
        KafkaOffset kafkaOffset = KafkaOffset.forName(autoOffsetReset);
        KafkaScanStartupMode startupMode;
        switch (kafkaOffset) {
            case EARLIEST:
                startupMode = KafkaScanStartupMode.EARLIEST_OFFSET;
                break;
            case SPECIFIC:
                startupMode = KafkaScanStartupMode.SPECIFIC_OFFSETS;
                break;
            case TIMESTAMP_MILLIS:
                startupMode = KafkaScanStartupMode.TIMESTAMP_MILLIS;
                break;
            case LATEST:
            default:
                startupMode = KafkaScanStartupMode.LATEST_OFFSET;
        }
        return startupMode;
    }

    /**
     * parse Format
     *
     * @param serializationType data serialization, support: json, canal, avro
     * @return the format for serialized content
     */
    private Format parseFormat(String serializationType) {
        DataTypeEnum dataType = DataTypeEnum.forType(serializationType);
        Format format;
        switch (dataType) {
            case CSV:
                format = new CsvFormat();
                break;
            case AVRO:
                format = new AvroFormat();
                break;
            case JSON:
                format = new JsonFormat();
                break;
            case CANAL:
                format = new CanalJsonFormat();
                break;
            case DEBEZIUM_JSON:
                format = new DebeziumJsonFormat();
                break;
            case RAW:
                format = new RawFormat();
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported dataType=%s for Kafka", dataType));
        }
        return format;
    }
}