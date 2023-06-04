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

package org.apache.inlong.manager.pojo.source.kafka;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.JsonUtils;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * kafka source information data transfer object.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KafkaSourceDTO {

    @ApiModelProperty("Kafka topic")
    private String topic;

    @ApiModelProperty("Kafka consumer group")
    private String groupId;

    @ApiModelProperty("Kafka servers address, such as: 127.0.0.1:9092")
    private String bootstrapServers;

    @ApiModelProperty(value = "Limit the amount of data read per second", notes = "Greater than or equal to 0, equal to zero means no limit")
    private String recordSpeedLimit;

    @ApiModelProperty(value = "Limit the number of bytes read per second", notes = "Greater than or equal to 0, equal to zero means no limit")
    private String byteSpeedLimit;

    @ApiModelProperty(value = "Topic partition offset", notes = "For example,'partition:0,offset:42;partition:1,offset:300' "
            + "indicates offset 42 for partition 0 and offset 300 for partition 1.")
    private String partitionOffsets;

    @ApiModelProperty(value = "timestamp is millis")
    private String timestampMillis;

    /**
     * The strategy of auto offset reset.
     *
     * @see <a href="https://docs.confluent.io/platform/current/clients/consumer.html">Kafka_consumer_config</a>
     */
    @ApiModelProperty(value = "The strategy of auto offset reset", notes = "including earliest, latest (the default), none")
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

    @ApiModelProperty(value = "Data encoding format: UTF-8, GBK")
    private String dataEncoding;

    @ApiModelProperty(value = "Data separator")
    private String dataSeparator;

    @ApiModelProperty(value = "Data field escape symbol")
    private String dataEscapeChar;

    @ApiModelProperty("Properties for Kafka")
    private Map<String, Object> properties;

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
                .partitionOffsets(request.getPartitionOffsets())
                .timestampMillis(request.getTimestampMillis())
                .autoOffsetReset(request.getAutoOffsetReset())
                .serializationType(request.getSerializationType())
                .databasePattern(request.getDatabasePattern())
                .tablePattern(request.getTablePattern())
                .ignoreParseErrors(request.isIgnoreParseErrors())
                .timestampFormatStandard(request.getTimestampFormatStandard())
                .primaryKey(request.getPrimaryKey())
                .properties(request.getProperties())
                .build();
    }

    public static KafkaSourceDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, KafkaSourceDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT,
                    String.format("parse extParams of KafkaSource failure: %s", e.getMessage()));
        }
    }
}
