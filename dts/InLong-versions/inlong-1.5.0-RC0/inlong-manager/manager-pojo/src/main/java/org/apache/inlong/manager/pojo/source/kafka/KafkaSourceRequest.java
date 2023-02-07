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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.source.SourceRequest;

import java.nio.charset.StandardCharsets;

/**
 * Kafka source request
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "Kafka source request")
@JsonTypeDefine(value = SourceType.KAFKA)
public class KafkaSourceRequest extends SourceRequest {

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

    @ApiModelProperty(value = "The strategy of auto offset reset", notes = "including earliest, latest (the default), none")
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

    @ApiModelProperty(value = "Data encoding format: UTF-8, GBK")
    private String dataEncoding = StandardCharsets.UTF_8.toString();

    @ApiModelProperty(value = "Data separator")
    private String dataSeparator = String.valueOf((int) '|');

    @ApiModelProperty(value = "Data field escape symbol")
    private String dataEscapeChar;

    public KafkaSourceRequest() {
        this.setSourceType(SourceType.KAFKA);
    }

}
