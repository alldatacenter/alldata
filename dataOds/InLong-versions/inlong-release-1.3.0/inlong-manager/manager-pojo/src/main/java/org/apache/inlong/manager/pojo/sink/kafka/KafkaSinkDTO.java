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

package org.apache.inlong.manager.pojo.sink.kafka;

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
import java.util.Map;

/**
 * Kafka sink info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaSinkDTO {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ApiModelProperty("Kafka bootstrap servers")
    private String bootstrapServers;

    @ApiModelProperty("Kafka topicName")
    private String topicName;

    @ApiModelProperty("Partition number of the topic")
    private String partitionNum;

    @ApiModelProperty("Data Serialization, support: json, canal, avro")
    private String serializationType;

    @ApiModelProperty(value = "The strategy of auto offset reset",
            notes = "including earliest, latest (the default), none")
    private String autoOffsetReset;

    @ApiModelProperty("Primary key is required when serializationType is json, avro")
    private String primaryKey;

    @ApiModelProperty("Properties for kafka")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static KafkaSinkDTO getFromRequest(KafkaSinkRequest request) {
        return KafkaSinkDTO.builder()
                .bootstrapServers(request.getBootstrapServers())
                .topicName(request.getTopicName())
                .partitionNum(request.getPartitionNum())
                .serializationType(request.getSerializationType())
                .autoOffsetReset(request.getAutoOffsetReset())
                .primaryKey(request.getPrimaryKey())
                .properties(request.getProperties())
                .build();
    }

    public static KafkaSinkDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, KafkaSinkDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }
}
