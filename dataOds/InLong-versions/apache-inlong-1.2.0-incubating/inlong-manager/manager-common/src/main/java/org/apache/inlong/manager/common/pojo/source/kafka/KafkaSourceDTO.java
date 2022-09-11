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

package org.apache.inlong.manager.common.pojo.source.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;

import javax.validation.constraints.NotNull;

/**
 * kafka source information data transfer object.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KafkaSourceDTO {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ApiModelProperty("Kafka topic")
    private String topic;

    @ApiModelProperty("Kafka consumer group")
    private String groupId;

    @ApiModelProperty("Kafka servers address, such as: 127.0.0.1:9092")
    private String bootstrapServers;

    @ApiModelProperty(value = "Limit the amount of data read per second",
            notes = "Greater than or equal to 0, equal to zero means no limit")
    private String recordSpeedLimit;

    @ApiModelProperty(value = "Limit the number of bytes read per second",
            notes = "Greater than or equal to 0, equal to zero means no limit")
    private String byteSpeedLimit;

    @ApiModelProperty(value = "Topic partition offset",
            notes = "For example, '0#100_1#10' means the offset of partition 0 is 100, the offset of partition 1 is 10")
    private String topicPartitionOffset;

    /**
     * The strategy of auto offset reset.
     *
     * @see <a href="https://docs.confluent.io/platform/current/clients/consumer.html">Kafka_consumer_config</a>
     */
    @ApiModelProperty(value = "The strategy of auto offset reset",
            notes = "including earliest, latest (the default), none")
    private String autoOffsetReset;

    @ApiModelProperty("Data Serialization, support: json, canal, avro, etc")
    private String serializationType;

    @ApiModelProperty("database pattern used for filter in canal format")
    private String databasePattern;

    @ApiModelProperty("table pattern used for filter in canal format")
    private String tablePattern;

    @ApiModelProperty("ignore parse errors, true: ignore parse error; false: not ignore parse error; default true")
    private boolean ignoreParseErrors;

    @ApiModelProperty("Timestamp standard for binlog: SQL, ISO_8601")
    private String timestampFormatStandard;

    @ApiModelProperty("Field needed when serializationType is csv,json,avro")
    private String primaryKey;

    /**
     * Get the dto instance from the request
     */
    public static KafkaSourceDTO getFromRequest(KafkaSourceRequest request) {
        return KafkaSourceDTO.builder()
                .topic(request.getTopic())
                .groupId(request.getGroupId())
                .bootstrapServers(request.getBootstrapServers())
                .recordSpeedLimit(request.getRecordSpeedLimit())
                .byteSpeedLimit(request.getByteSpeedLimit())
                .topicPartitionOffset(request.getTopicPartitionOffset())
                .autoOffsetReset(request.getAutoOffsetReset())
                .serializationType(request.getSerializationType())
                .databasePattern(request.getDatabasePattern())
                .tablePattern(request.getTablePattern())
                .ignoreParseErrors(request.isIgnoreParseErrors())
                .timestampFormatStandard(request.getTimestampFormatStandard())
                .primaryKey(request.getPrimaryKey())
                .build();
    }

    public static KafkaSourceDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, KafkaSourceDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT.getMessage());
        }
    }
}
