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

package org.apache.inlong.manager.pojo.sink.redis;

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
 * Redis sink info
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "Redis sink info")
@JsonTypeDefine(value = SinkType.REDIS)
public class RedisSink extends StreamSink {

    @ApiModelProperty("Redis cluster mode")
    private String clusterMode;

    @ApiModelProperty("Redis database id")
    private Integer database;

    @ApiModelProperty("Redis data type")
    private String dataType;

    @ApiModelProperty("Redis schema mapping mode")
    private String schemaMapMode;

    @ApiModelProperty("Password for Redis accessing")
    private String password;

    @ApiModelProperty("Database name")
    private String databaseName;

    @ApiModelProperty("Expire time of Redis row")
    private Integer ttl;

    @ApiModelProperty("The timeout of Redis client")
    private Integer timeout;

    @ApiModelProperty("The socket timeout of redis client")
    private Integer soTimeout;

    @ApiModelProperty("The max total of sink client")
    private Integer maxTotal;

    @ApiModelProperty("The max idle of sink client")
    private Integer maxIdle;

    @ApiModelProperty("The min idle of sink client")
    private Integer minIdle;

    @ApiModelProperty("The max retry time")
    private Integer maxRetries;

    @ApiModelProperty("The host of Redis server")
    private String host;

    @ApiModelProperty("The port of Redis server")
    private Integer port;

    @ApiModelProperty("The master name of Redis sentinel cluster")
    private String masterName;

    @ApiModelProperty("The sentinels info of Redis sentinel cluster")
    private String sentinelsInfo;

    /**
     * The address of redis server, format eg: 127.0.0.1:8080,127.0.0.2:8081 .
     * If server is not cluster mode, server address format eg: 127.0.0.1:8080 .
     */
    @ApiModelProperty("The cluster nodes of Redis cluster")
    private String clusterNodes;

    @ApiModelProperty("The DataEncoding of Redis STATIC_PREFIX_MATCH data-type")
    private String formatDataEncoding;

    @ApiModelProperty("The DataType of Redis STATIC_PREFIX_MATCH data-type")
    private String formatDataType;

    @ApiModelProperty("Whether ignore parse error of Redis STATIC_PREFIX_MATCH data-type")
    private Boolean formatIgnoreParseError;

    @ApiModelProperty("The data separator of Redis STATIC_PREFIX_MATCH data-type")
    private String formatDataSeparator;

    public RedisSink() {
        this.setSinkType(SinkType.REDIS);
    }

    @Override
    public SinkRequest genSinkRequest() {
        return CommonBeanUtils.copyProperties(this, RedisSinkRequest::new);
    }

}
