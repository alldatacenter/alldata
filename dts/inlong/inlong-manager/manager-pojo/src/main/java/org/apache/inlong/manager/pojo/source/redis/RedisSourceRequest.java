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

package org.apache.inlong.manager.pojo.source.redis;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.source.SourceRequest;

/**
 * Redis source request
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "Redis source request")
@JsonTypeDefine(value = SourceType.REDIS)
public class RedisSourceRequest extends SourceRequest {

    @ApiModelProperty("Redis primaryKey")
    private String primaryKey;

    @ApiModelProperty("Redis host")
    private String host;

    @ApiModelProperty("Redis port")
    private Integer port;

    @ApiModelProperty("Redis username")
    private String username;

    @ApiModelProperty("Redis password")
    private String password;

    @ApiModelProperty("Redis database")
    private Integer database;

    @ApiModelProperty("Redis deploy Mode(standalone/cluster/sentinel)")
    private String redisMode;

    @ApiModelProperty("supportted in Sort-connector-redis(hget/get/zscore/zrevrank)")
    private String command;

    @ApiModelProperty("The additional key connect to redis only used for [Hash|Sorted-Set] data type")
    private String additionalKey;

    @ApiModelProperty("The timeout connect to redis")
    private Integer timeout;

    @ApiModelProperty("The soTimeout connect to redis")
    private Integer soTimeout;

    @ApiModelProperty("The maxTotal connect to redis")
    private Integer maxTotal;

    @ApiModelProperty("The maxIdle connect to redis")
    private Integer maxIdle;

    @ApiModelProperty("The minIdle connect to redis")
    private Integer minIdle;

    @ApiModelProperty("The lookup options for connector redis")
    private RedisLookupOptions lookupOptions;

    @ApiModelProperty("The masterName for connector redis")
    private String masterName;

    @ApiModelProperty("The sentinelsInfo for connector redis")
    private String sentinelsInfo;

    @ApiModelProperty("The clusterNodes for connector redis")
    private String clusterNodes;

    public RedisSourceRequest() {
        this.setSourceType(SourceType.REDIS);
    }

}
