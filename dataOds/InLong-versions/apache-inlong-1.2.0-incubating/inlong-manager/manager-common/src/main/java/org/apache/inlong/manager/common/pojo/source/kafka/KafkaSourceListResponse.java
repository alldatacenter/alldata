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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.apache.inlong.manager.common.enums.SourceType;
import org.apache.inlong.manager.common.pojo.source.SourceListResponse;
import org.apache.inlong.manager.common.util.JsonTypeDefine;

/**
 * Response of kafka source list
 */
@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ApiModel("Response of kafka source paging list")
@JsonTypeDefine(value = SourceType.SOURCE_KAFKA)
public class KafkaSourceListResponse extends SourceListResponse {

    @ApiModelProperty("Kafka topic")
    private String topic;

    @ApiModelProperty("Kafka consumer group")
    private String groupId;

    @ApiModelProperty("Kafka servers address")
    private String bootstrapServers;

    @ApiModelProperty("Limit the amount of data read per second")
    private String recordSpeedLimit;

    @ApiModelProperty("Limit the number of bytes read per second")
    private String byteSpeedLimit;

    @ApiModelProperty(value = "Topic partition offset",
            notes = "For example, '0#100_1#10' means the offset of partition 0 is 100, the offset of partition 1 is 10")
    private String topicPartitionOffset;

    @ApiModelProperty(value = "The strategy of auto offset reset",
            notes = "including earliest, latest (the default), none")
    private String autoOffsetReset;

    @ApiModelProperty("Primary key, needed when serialization type is csv, json, avro")
    private String primaryKey;

    public KafkaSourceListResponse() {
        this.setSourceType(SourceType.KAFKA.getType());
    }

}
