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
import lombok.AllArgsConstructor;
import lombok.Builder;
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
 * Redis source info
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "Redis source info")
@JsonTypeDefine(value = SourceType.REDIS)
public class RedisSource extends StreamSource {

    @ApiModelProperty("Username of the redis server")
    private String username;

    @ApiModelProperty("Password of the redis server")
    private String password;

    @ApiModelProperty("Hostname of the redis server")
    private String hostname;

    @ApiModelProperty("Port of the redis server")
    @Builder.Default
    private Integer port = 6379;

    @ApiModelProperty("Primary key")
    private String primaryKey;

    @ApiModelProperty("Redis command, supports: hget, get, zscore, zrevrank")
    private String redisCommand;

    @ApiModelProperty("Redis deploy mode, supports: standalone, cluster, sentinel")
    private String redisMode;

    @ApiModelProperty("Cluster node infos only used for redis cluster deploy mode")
    private String clusterNodes;

    @ApiModelProperty("Master name only used for redis sentinel deploy mode")
    private String masterName;

    @ApiModelProperty("Sentinels info only used for redis sentinel deploy mode")
    private String sentinelsInfo;

    @ApiModelProperty("Additional key only used for hash/Sorted-set data type")
    private String additionalKey;

    @ApiModelProperty("Database number connect to redis for redis standalone/sentinel deploy modes")
    private Integer database;

    @ApiModelProperty("Timeout value of connect to redis")
    private Integer timeout;

    @ApiModelProperty("Timeout value of read data from redis")
    private Integer soTimeout;

    @ApiModelProperty("Max connection number to redis")
    private Integer maxTotal;

    @ApiModelProperty("Max free connection number")
    private Integer maxIdle;

    @ApiModelProperty("Min free connection number")
    private Integer minIdle;

    @ApiModelProperty("Lookup Async")
    private Boolean lookupAsync;

    @ApiModelProperty("Lookup cache max rows")
    private Long lookupCacheMaxRows;

    @ApiModelProperty("Lookup cache ttl")
    private Long lookupCacheTtl;

    @ApiModelProperty("Lookup max retry times")
    private Integer lookupMaxRetries;

    public RedisSource() {
        this.setSourceType(SourceType.REDIS);
    }

    @Override
    public SourceRequest genSourceRequest() {
        return CommonBeanUtils.copyProperties(this, RedisSourceRequest::new);
    }
}
