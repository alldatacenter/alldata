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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.manager.common.enums.SourceType;
import org.apache.inlong.manager.common.pojo.source.SourceRequest;
import org.apache.inlong.manager.common.util.JsonTypeDefine;

/**
 * Request of kafka source info
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "Request of the kafka source info")
@JsonTypeDefine(value = SourceType.SOURCE_KAFKA)
public class KafkaSourceRequest extends SourceRequest {

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

    @ApiModelProperty(value = "The strategy of auto offset reset",
            notes = "including earliest, latest (the default), none")
    private String autoOffsetReset;

    @ApiModelProperty("database pattern used for filter in canal format")
    private String databasePattern;

    @ApiModelProperty("table pattern used for filter in canal format")
    private String tablePattern;

    @ApiModelProperty("ignore parse errors, true: ignore parse error; false: not ignore parse error; default true")
    private boolean ignoreParseErrors = true;

    @ApiModelProperty("Timestamp standard for binlog: SQL, ISO_8601")
    private String timestampFormatStandard = "SQL";

    @ApiModelProperty("Primary key, needed when serialization type is csv, json, avro")
    private String primaryKey;

    public KafkaSourceRequest() {
        this.setSourceType(SourceType.KAFKA.toString());
    }

}
