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

package org.apache.inlong.manager.service.node.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.DataNodeType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.DataNodeEntity;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;
import org.apache.inlong.manager.pojo.node.redis.RedisDataNodeDTO;
import org.apache.inlong.manager.pojo.node.redis.RedisDataNodeInfo;
import org.apache.inlong.manager.pojo.node.redis.RedisDataNodeRequest;
import org.apache.inlong.manager.pojo.sink.redis.RedisClusterMode;
import org.apache.inlong.manager.service.node.AbstractDataNodeOperator;
import org.apache.inlong.manager.service.resource.sink.redis.RedisResourceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.apache.inlong.manager.common.util.Preconditions.expectTrue;

@Service
public class RedisDataNodeOperator extends AbstractDataNodeOperator {

    private static final int PORT_MAX_VALUE = 65535;

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDataNodeOperator.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String dataNodeType) {
        return getDataNodeType().equals(dataNodeType);
    }

    @Override
    public String getDataNodeType() {
        return DataNodeType.REDIS;
    }

    @Override
    public DataNodeInfo getFromEntity(DataNodeEntity entity) {
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NODE_NOT_FOUND);
        }

        RedisDataNodeInfo redisDataNodeInfo = new RedisDataNodeInfo();
        CommonBeanUtils.copyProperties(entity, redisDataNodeInfo);
        if (StringUtils.isNotBlank(entity.getExtParams())) {
            RedisDataNodeDTO dto = RedisDataNodeDTO.getFromJson(entity.getExtParams());
            CommonBeanUtils.copyProperties(dto, redisDataNodeInfo);
        }
        return redisDataNodeInfo;
    }

    @Override
    protected void setTargetEntity(DataNodeRequest request, DataNodeEntity targetEntity) {
        RedisDataNodeRequest redisDataNodeRequest = (RedisDataNodeRequest) request;

        RedisClusterMode clusterMode = RedisClusterMode.of(redisDataNodeRequest.getClusterMode());

        switch (clusterMode) {
            case STANDALONE:
                String host = redisDataNodeRequest.getHost();
                Preconditions.expectNotBlank(host, "Redis host cannot be empty");
                Integer port = redisDataNodeRequest.getPort();
                expectTrue(
                        port != null && port > 1 && port < PORT_MAX_VALUE,
                        "The port of the redis server must be greater than 1 and less than " + PORT_MAX_VALUE +
                                "!");

                break;
            case SENTINEL:
                String sentinelMasterName = redisDataNodeRequest.getMasterName();
                Preconditions.expectNotBlank(sentinelMasterName, "Redis sentinel masterName cannot be empty");
                String sentinelsInfo = redisDataNodeRequest.getSentinelsInfo();
                Preconditions.expectNotBlank(sentinelsInfo, "Redis sentinelsInfo cannot be empty");
                break;
            case CLUSTER:
                String clusterNodes = redisDataNodeRequest.getClusterNodes();
                Preconditions.expectNotBlank(clusterNodes, "Redis clusterNodes cannot be empty");
                break;

            default:
                throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT, "Unknown Redis cluster mode");
        }

        CommonBeanUtils.copyProperties(redisDataNodeRequest, targetEntity, true);
        try {
            RedisDataNodeDTO dto = RedisDataNodeDTO.getFromRequest(redisDataNodeRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT,
                    String.format("Failed to build extParams for Redis node: %s", e.getMessage()));
        }
    }

    @Override
    public Boolean testConnection(DataNodeRequest request) {
        RedisDataNodeRequest redisDataNodeRequest = (RedisDataNodeRequest) request;
        try {
            return RedisResourceClient.testConnection(redisDataNodeRequest);
        } catch (Exception e) {
            String errMsg = String.format("redis connection failed: %s ", e.getMessage());

            throw new BusinessException(errMsg);
        }
    }

}
