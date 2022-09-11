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

package org.apache.inlong.manager.common.pojo.sink.ck;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.manager.common.enums.SinkType;
import org.apache.inlong.manager.common.pojo.sink.SinkRequest;
import org.apache.inlong.manager.common.util.JsonTypeDefine;

/**
 * Request of the ClickHouse sink.
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "Request of the ClickHouse sink info")
@JsonTypeDefine(value = SinkType.SINK_CLICKHOUSE)
public class ClickHouseSinkRequest extends SinkRequest {

    @ApiModelProperty("ClickHouse JDBC URL")
    private String jdbcUrl;

    @ApiModelProperty("Username for JDBC URL")
    private String username;

    @ApiModelProperty("User password")
    private String password;

    @ApiModelProperty("Target database name")
    private String dbName;

    @ApiModelProperty("Target table name")
    private String tableName;

    @ApiModelProperty("Flush interval, unit: second, default is 1s")
    private Integer flushInterval = 1;

    @ApiModelProperty("Flush when record number reaches flushRecord")
    private Integer flushRecord;

    @ApiModelProperty("Write max retry times, default is 3")
    private Integer retryTimes = 3;

    @ApiModelProperty("Whether distributed table? 0: no, 1: yes")
    private Integer isDistributed;

    @ApiModelProperty("Partition strategy, support: BALANCE, RANDOM, HASH")
    private String partitionStrategy;

    @ApiModelProperty(value = "Partition files, separate with commas",
            notes = "Necessary when partitionStrategy is HASH, must be one of the field list")
    private String partitionFields;

    @ApiModelProperty("Key field names, separate with commas")
    private String keyFieldNames;

    @ApiModelProperty("Table engine, support MergeTree Mem and so on")
    private String engine;

    @ApiModelProperty("Table partition information")
    private String partitionBy;

    @ApiModelProperty("Table order information")
    private String orderBy;

    @ApiModelProperty("Table primary key")
    private String primaryKey;

}
