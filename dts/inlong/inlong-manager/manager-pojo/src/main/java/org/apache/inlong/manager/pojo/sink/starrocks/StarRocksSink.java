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

package org.apache.inlong.manager.pojo.sink.starrocks;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;

/**
 * StarRocks sink info
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "StarRocks sink info")
@JsonTypeDefine(value = SinkType.STARROCKS)
public class StarRocksSink extends StreamSink {

    @ApiModelProperty("StarRocks jdbc url")
    private String jdbcUrl;

    @ApiModelProperty("StarRocks FE http address")
    private String loadUrl;

    @ApiModelProperty("Username for StarRocks accessing")
    private String username;

    @ApiModelProperty("Password for StarRocks accessing")
    private String password;

    @ApiModelProperty("Database name")
    private String databaseName;

    @ApiModelProperty("Table name")
    private String tableName;

    @ApiModelProperty("The primary key of sink table")
    private String primaryKey;

    @ApiModelProperty("The multiple enable of sink")
    private Boolean sinkMultipleEnable = false;

    @ApiModelProperty("The multiple format of sink")
    private String sinkMultipleFormat;

    @ApiModelProperty("The multiple database-pattern of sink")
    private String databasePattern;

    @ApiModelProperty("The multiple table-pattern of sink")
    private String tablePattern;

    @ApiModelProperty("The table engine,  like: OLAP, MYSQL, ELASTICSEARCH, etc, default is OLAP")
    private String tableEngine;

    @ApiModelProperty("The table replication num")
    private Integer replicationNum;

    @ApiModelProperty("The table barrel size")
    private Integer barrelSize;

    public StarRocksSink() {
        this.setSinkType(SinkType.STARROCKS);
    }

    @Override
    public SinkRequest genSinkRequest() {
        return CommonBeanUtils.copyProperties(this, StarRocksSinkRequest::new);
    }

}
