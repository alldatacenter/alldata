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

import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;

import java.util.Map;

/**
 * Sink info of Redis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisSinkDTO {

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
    @ApiModelProperty("Properties for Redis")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static RedisSinkDTO getFromRequest(RedisSinkRequest request, String extParams) throws Exception {
        RedisSinkDTO dto = StringUtils.isNotBlank(extParams)
                ? RedisSinkDTO.getFromJson(extParams)
                : new RedisSinkDTO();
        return CommonBeanUtils.copyProperties(request, dto, true);
    }

    public static RedisSinkDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, RedisSinkDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT,
                    String.format("parse extParams of Redis SinkDTO failure: %s", e.getMessage()));
        }
    }

}
