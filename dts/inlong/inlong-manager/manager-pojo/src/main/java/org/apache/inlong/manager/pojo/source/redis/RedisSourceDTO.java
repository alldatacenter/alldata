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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;

import javax.validation.constraints.NotNull;

/**
 * redis source info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisSourceDTO {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    /**
     * Get the dto instance from the request
     */
    public static RedisSourceDTO getFromRequest(RedisSourceRequest request) {
        return RedisSourceDTO.builder()
                .primaryKey(request.getPrimaryKey())
                .host(request.getHost())
                .port(request.getPort())
                .username(request.getUsername())
                .password(request.getPassword())
                .database(request.getDatabase())
                .redisMode(request.getRedisMode())
                .command(request.getCommand())
                .additionalKey(request.getAdditionalKey())
                .timeout(request.getTimeout())
                .soTimeout(request.getSoTimeout())
                .maxTotal(request.getMaxTotal())
                .maxIdle(request.getMaxIdle())
                .minIdle(request.getMinIdle())
                .lookupOptions(request.getLookupOptions())
                .masterName(request.getMasterName())
                .sentinelsInfo(request.getSentinelsInfo())
                .clusterNodes(request.getClusterNodes())
                .build();
    }

    /**
     * Get the dto instance from the JSON string
     */
    public static RedisSourceDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, RedisSourceDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT.getMessage());
        }
    }

}
