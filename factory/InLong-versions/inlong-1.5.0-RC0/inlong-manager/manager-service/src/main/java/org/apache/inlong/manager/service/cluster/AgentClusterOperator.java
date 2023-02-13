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

package org.apache.inlong.manager.service.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.ClusterRequest;
import org.apache.inlong.manager.pojo.cluster.agent.AgentClusterDTO;
import org.apache.inlong.manager.pojo.cluster.agent.AgentClusterInfo;
import org.apache.inlong.manager.pojo.cluster.agent.AgentClusterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Agent cluster operator.
 */
@Slf4j
@Service
public class AgentClusterOperator extends AbstractClusterOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentClusterOperator.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String clusterType) {
        return getClusterType().equals(clusterType);
    }

    @Override
    public String getClusterType() {
        return ClusterType.AGENT;
    }

    @Override
    public ClusterInfo getFromEntity(InlongClusterEntity entity) {
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }

        AgentClusterInfo agentInfo = new AgentClusterInfo();
        CommonBeanUtils.copyProperties(entity, agentInfo);
        if (StringUtils.isNotBlank(entity.getExtParams())) {
            AgentClusterDTO dto = AgentClusterDTO.getFromJson(entity.getExtParams());
            CommonBeanUtils.copyProperties(dto, agentInfo);
        }

        LOGGER.info("success to get agent cluster info from entity");
        return agentInfo;
    }

    @Override
    protected void setTargetEntity(ClusterRequest request, InlongClusterEntity targetEntity) {
        AgentClusterRequest agentRequest = (AgentClusterRequest) request;
        CommonBeanUtils.copyProperties(agentRequest, targetEntity, true);
        try {
            AgentClusterDTO dto = AgentClusterDTO.getFromRequest(agentRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
            LOGGER.info("success to set entity for agent cluster");
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.CLUSTER_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

}
