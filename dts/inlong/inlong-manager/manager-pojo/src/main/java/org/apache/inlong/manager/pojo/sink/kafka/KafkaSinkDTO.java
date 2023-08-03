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

import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

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

    @ApiModelProperty("Kafka bootstrap servers")
    private String bootstrapServers;

    @ApiModelProperty("Kafka topicName")
    private String topicName;

    @ApiModelProperty("Partition number of the topic")
    private Integer partitionNum;

    @ApiModelProperty("Data Serialization, support: json, canal, avro")
    private String serializationType;

    @ApiModelProperty(value = "The strategy of auto offset reset", notes = "including earliest, latest (the default), none")
    private String autoOffsetReset;

    @ApiModelProperty("Primary key is required when serializationType is json, avro")
    private String primaryKey;

    @ApiModelProperty("Properties for kafka")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static KafkaSinkDTO getFromRequest(KafkaSinkRequest request, String extParams) {
        KafkaSinkDTO dto = StringUtils.isNotBlank(extParams) ? KafkaSinkDTO.getFromJson(extParams) : new KafkaSinkDTO();
        return CommonBeanUtils.copyProperties(request, dto, true);
    }

    public static KafkaSinkDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, KafkaSinkDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT,
                    String.format("parse extParams of Kafka SinkDTO failure: %s", e.getMessage()));
        }
    }
}
