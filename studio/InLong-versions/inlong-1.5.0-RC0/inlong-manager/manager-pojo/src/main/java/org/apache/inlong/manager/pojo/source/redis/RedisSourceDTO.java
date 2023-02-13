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

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.JsonUtils;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * redis source info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisSourceDTO {

    @ApiModelProperty("Username of the redis server")
    private String username;

    @ApiModelProperty("Password of the redis server")
    private String password;

    @ApiModelProperty("Hostname of the redis server")
    private String hostname;

    @ApiModelProperty("Port of the redis server")
    private Integer port;

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

    @ApiModelProperty("Lookup cache max rows")
    private Long lookupCacheMaxRows;

    @ApiModelProperty("Lookup cache ttl")
    private Long lookupCacheTtl;

    @ApiModelProperty("Lookup max retry times")
    private Integer lookupMaxRetries;

    @ApiModelProperty("Lookup Async")
    private Boolean lookupAsync;

    @ApiModelProperty("Properties for redis")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from request
     */
    public static RedisSourceDTO getFromRequest(RedisSourceRequest request) {
        return RedisSourceDTO.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .hostname(request.getHostname())
                .port(request.getPort())
                .primaryKey(request.getPrimaryKey())
                .redisCommand(request.getRedisCommand())
                .redisMode(request.getRedisMode())
                .clusterNodes(request.getClusterNodes())
                .masterName(request.getMasterName())
                .sentinelsInfo(request.getSentinelsInfo())
                .additionalKey(request.getAdditionalKey())
                .database(request.getDatabase())
                .timeout(request.getTimeout())
                .soTimeout(request.getSoTimeout())
                .maxTotal(request.getMaxTotal())
                .maxIdle(request.getMaxIdle())
                .minIdle(request.getMinIdle())
                .lookupCacheMaxRows(request.getLookupCacheMaxRows())
                .lookupCacheTtl(request.getLookupCacheTtl())
                .lookupMaxRetries(request.getLookupMaxRetries())
                .lookupAsync(request.getLookupAsync())
                .properties(request.getProperties())
                .build();
    }

    public static RedisSourceDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, RedisSourceDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT,
                    String.format("parse extParams of RedisSource failure: %s", e.getMessage()));
        }
    }

}
