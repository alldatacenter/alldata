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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.source.SourceRequest;
import org.apache.inlong.manager.pojo.source.StreamSource;

/**
 * Kafka source info
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "Kafka source info")
@JsonTypeDefine(value = SourceType.KAFKA)
public class KafkaSource extends StreamSource {

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

    @ApiModelProperty(value = "Topic partition offset", notes = "For example,'partition:0,offset:42;partition:1,offset:300' "
            + "indicates offset 42 for partition 0 and offset 300 for partition 1.")
    private String partitionOffsets;

    @ApiModelProperty(value = "timestamp is millis")
    private String timestampMillis;

    @ApiModelProperty(value = "The strategy of auto offset reset", notes = "including earliest, specific, latest (the default), none")
    private String autoOffsetReset;

    @ApiModelProperty("database pattern used for filter in canal format")
    private String databasePattern;

    @ApiModelProperty("table pattern used for filter in canal format")
    private String tablePattern;

    @ApiModelProperty("ignore parse errors, true: ignore parse error; false: not ignore parse error; default true")
    private boolean ignoreParseErrors;

    @ApiModelProperty("Timestamp standard for binlog: SQL, ISO_8601")
    private String timestampFormatStandard;

    @ApiModelProperty("Primary key, needed when serialization type is csv, json, avro")
    private String primaryKey;

    @ApiModelProperty(value = "Data encoding format: UTF-8, GBK")
    private String dataEncoding;

    @ApiModelProperty(value = "Data separator")
    private String dataSeparator;

    @ApiModelProperty(value = "Data field escape symbol")
    private String dataEscapeChar;

    @ApiModelProperty("Whether wrap content with InlongMsg")
    private boolean wrapWithInlongMsg = true;

    public KafkaSource() {
        this.setSourceType(SourceType.KAFKA);
    }

    @Override
    public SourceRequest genSourceRequest() {
        return CommonBeanUtils.copyProperties(this, KafkaSourceRequest::new);
    }

}
