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

package org.apache.inlong.manager.pojo.node.redis;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;

/**
 * Redis data node info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Redis data node info")
public class RedisDataNodeDTO {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDataNodeDTO.class);
    /**
     * Redis cluster mode
     */
    @ApiModelProperty(value = "Redis cluster mode")
    private String clusterMode;

    /**
     * Redis host
     */
    @ApiModelProperty(value = "Redis host")
    private String host;

    /**
     * Redis port
     */
    @ApiModelProperty(value = "Redis port")
    private Integer port;

    /**
     * Redis sentinel master name
     */
    @ApiModelProperty(value = "Redis sentinel master name")
    private String masterName;

    /**
     * Redis sentinel info
     */
    @ApiModelProperty(value = "Redis sentinel info")
    private String sentinelsInfo;

    /**
     * Redis cluster nodes
     */
    @ApiModelProperty(value = "Redis cluster nodes")
    private String clusterNodes;
    /**
     * Get the dto instance from the request
     */
    public static RedisDataNodeDTO getFromRequest(RedisDataNodeRequest request) throws Exception {
        return RedisDataNodeDTO.builder()
                .clusterMode(request.getClusterMode())
                .host(request.getHost())
                .port(request.getPort())
                .masterName(request.getMasterName())
                .sentinelsInfo(request.getSentinelsInfo())
                .clusterNodes(request.getClusterNodes())
                .build();
    }

    /**
     * Get the dto instance from the JSON string.
     */
    public static RedisDataNodeDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, RedisDataNodeDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.GROUP_INFO_INCORRECT,
                    String.format("Failed to parse extParams for Redis node: %s", e.getMessage()));
        }
    }

}
