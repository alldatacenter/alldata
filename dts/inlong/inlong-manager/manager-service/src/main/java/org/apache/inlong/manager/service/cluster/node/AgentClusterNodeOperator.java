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

package org.apache.inlong.manager.service.cluster.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.InlongClusterNodeEntity;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeResponse;
import org.apache.inlong.manager.pojo.cluster.agent.AgentClusterNodeDTO;
import org.apache.inlong.manager.pojo.cluster.agent.AgentClusterNodeRequest;
import org.apache.inlong.manager.pojo.cluster.agent.AgentClusterNodeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Agent cluster node operator.
 */
@Slf4j
@Service
public class AgentClusterNodeOperator extends AbstractClusterNodeOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentClusterNodeOperator.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String clusterNodeType) {
        return getClusterNodeType().equals(clusterNodeType);
    }

    @Override
    public String getClusterNodeType() {
        return ClusterType.AGENT;
    }

    @Override
    public ClusterNodeResponse getFromEntity(InlongClusterNodeEntity entity) {
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }

        AgentClusterNodeResponse agentClusterNodeResponse = new AgentClusterNodeResponse();
        CommonBeanUtils.copyProperties(entity, agentClusterNodeResponse);
        if (StringUtils.isNotBlank(entity.getExtParams())) {
            AgentClusterNodeDTO dto = AgentClusterNodeDTO.getFromJson(entity.getExtParams());
            CommonBeanUtils.copyProperties(dto, agentClusterNodeResponse);
        }

        LOGGER.debug("success to get agent cluster node info from entity");
        return agentClusterNodeResponse;
    }

    @Override
    protected void setTargetEntity(ClusterNodeRequest request, InlongClusterNodeEntity targetEntity) {
        AgentClusterNodeRequest agentNodeRequest = (AgentClusterNodeRequest) request;
        CommonBeanUtils.copyProperties(agentNodeRequest, targetEntity, true);
        try {
            AgentClusterNodeDTO dto = AgentClusterNodeDTO.getFromRequest(agentNodeRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
            LOGGER.debug("success to set entity for agent cluster node");
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.CLUSTER_INFO_INCORRECT,
                    String.format("serialize extParams of Agent ClusterNode failure: %s", e.getMessage()));
        }
    }
}
