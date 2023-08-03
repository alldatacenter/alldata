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

package org.apache.inlong.manager.service.sink.redis;

import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.redis.RedisClusterMode;
import org.apache.inlong.manager.pojo.sink.redis.RedisDataType;
import org.apache.inlong.manager.pojo.sink.redis.RedisSchemaMapMode;
import org.apache.inlong.manager.pojo.sink.redis.RedisSink;
import org.apache.inlong.manager.pojo.sink.redis.RedisSinkDTO;
import org.apache.inlong.manager.pojo.sink.redis.RedisSinkRequest;
import org.apache.inlong.manager.service.sink.AbstractSinkOperator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import static org.apache.inlong.manager.common.enums.ErrorCodeEnum.IP_EMPTY;
import static org.apache.inlong.manager.common.enums.ErrorCodeEnum.PORT_EMPTY;
import static org.apache.inlong.manager.common.enums.ErrorCodeEnum.SINK_SAVE_FAILED;
import static org.apache.inlong.manager.common.enums.ErrorCodeEnum.SINK_TYPE_NOT_SUPPORT;
import static org.apache.inlong.manager.common.util.Preconditions.expectNotBlank;
import static org.apache.inlong.manager.common.util.Preconditions.expectNotEmpty;
import static org.apache.inlong.manager.common.util.Preconditions.expectNotNull;
import static org.apache.inlong.manager.common.util.Preconditions.expectTrue;

/**
 * Redis sink operator, such as save or update redis field, etc.
 */
@Service
public class RedisSinkOperator extends AbstractSinkOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisSinkOperator.class);
    private static final int PORT_MAX_VALUE = 65535;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.REDIS.equals(sinkType);
    }

    @Override
    protected String getSinkType() {
        return SinkType.REDIS;
    }

    @Override
    protected void setTargetEntity(SinkRequest request, StreamSinkEntity targetEntity) {

        if (!this.getSinkType().equals(request.getSinkType())) {
            throw new BusinessException(ErrorCodeEnum.SINK_SAVE_FAILED,
                    SINK_TYPE_NOT_SUPPORT.getMessage() + ": " + getSinkType());
        }

        RedisSinkRequest sinkRequest = (RedisSinkRequest) request;

        String clusterMode = sinkRequest.getClusterMode();
        RedisClusterMode redisClusterMode = RedisClusterMode.of(clusterMode);

        expectNotNull(redisClusterMode,
                "Redis ClusterMode must in one of " + Arrays.toString(RedisClusterMode.values()) + " !");

        switch (redisClusterMode) {
            case CLUSTER:
                String clusterNodes = sinkRequest.getClusterNodes();
                checkClusterNodes(clusterNodes);
                break;
            case SENTINEL:
                String sentinelMasterName = sinkRequest.getMasterName();
                expectNotEmpty(sentinelMasterName, "Redis MasterName of Sentinel cluster must not null!");
                String sentinelsInfo = sinkRequest.getSentinelsInfo();
                expectNotEmpty(sentinelsInfo, "Redis sentinelsInfo of Sentinel cluster must not null!");
                break;
            case STANDALONE:
                String host = sinkRequest.getHost();
                Integer port = sinkRequest.getPort();

                expectNotEmpty(host, "Redis server host must not null!");
                expectTrue(
                        port != null && port > 1 && port < PORT_MAX_VALUE,
                        "The port of the redis server must be greater than 0 and less than 65535!");
                break;
        }
        RedisDataType dataType = RedisDataType.valueOf(sinkRequest.getDataType());
        expectNotNull(dataType, "Redis DataType must not null");

        RedisSchemaMapMode mapMode = RedisSchemaMapMode.valueOf(sinkRequest.getSchemaMapMode());
        expectTrue(dataType.getMapModes().contains(mapMode),
                "Redis schemaMapMode '" + mapMode + "' is not supported in '" + dataType + "'");

        try {
            RedisSinkDTO dto = RedisSinkDTO.getFromRequest(sinkRequest, targetEntity.getExtParams());
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(SINK_SAVE_FAILED,
                    String.format("serialize extParams of Redis SinkDTO failure: %s", e.getMessage()));
        }
    }

    private void checkClusterNodes(String clusterNodes) {
        expectNotBlank(clusterNodes, "the nodes of Redis cluster must not null");
        String[] nodeArray = clusterNodes.split(",");
        expectNotEmpty(nodeArray, "the nodes of Redis cluster must not null");

        for (String node : nodeArray) {
            expectNotBlank(node, "Redis server host must not null!");
            String[] ipPort = node.split(":");
            expectTrue(ipPort.length == 2, "The ip and port of Redis server must be in form: ip:port");
            expectNotBlank(ipPort[0], IP_EMPTY);
            expectNotBlank(ipPort[1], PORT_EMPTY);
        }
    }

    @Override
    public StreamSink getFromEntity(StreamSinkEntity entity) {
        RedisSink sink = new RedisSink();
        if (entity == null) {
            return sink;
        }

        RedisSinkDTO dto = RedisSinkDTO.getFromJson(entity.getExtParams());

        CommonBeanUtils.copyProperties(entity, sink, true);
        CommonBeanUtils.copyProperties(dto, sink, true);
        List<SinkField> sinkFields = super.getSinkFields(entity.getId());
        sink.setSinkFieldList(sinkFields);
        return sink;
    }

}
