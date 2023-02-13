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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.manager.common.enums.DataFormat;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.common.util.JsonTypeDefine;

/**
 * Kafka sink request.
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "Kafka sink request")
@JsonTypeDefine(value = SinkType.KAFKA)
public class KafkaSinkRequest extends SinkRequest {

    @ApiModelProperty("Kafka bootstrap servers")
    private String bootstrapServers;

    @ApiModelProperty("Kafka topicName")
    private String topicName;

    @ApiModelProperty("Partition number of the topic")
    private String partitionNum;

    @ApiModelProperty("Data Serialization, support: json, canal, avro")
    private String serializationType = DataFormat.CANAL.getName();

    @ApiModelProperty(value = "The strategy of auto offset reset", notes = "including earliest, latest (the default), none")
    private String autoOffsetReset;

    @ApiModelProperty("Primary key is required when serializationType is json, avro")
    private String primaryKey;

}
