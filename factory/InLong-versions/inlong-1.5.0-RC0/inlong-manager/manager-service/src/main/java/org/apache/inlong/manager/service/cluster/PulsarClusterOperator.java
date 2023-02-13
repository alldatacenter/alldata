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
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.ClusterRequest;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterDTO;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterInfo;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterRequest;
import org.apache.inlong.manager.service.resource.queue.pulsar.PulsarUtils;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Pulsar cluster operator.
 */
@Service
public class PulsarClusterOperator extends AbstractClusterOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PulsarClusterOperator.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String clusterType) {
        return getClusterType().equals(clusterType);
    }

    @Override
    public String getClusterType() {
        return ClusterType.PULSAR;
    }

    @Override
    public ClusterInfo getFromEntity(InlongClusterEntity entity) {
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }

        PulsarClusterInfo pulsarInfo = new PulsarClusterInfo();
        CommonBeanUtils.copyProperties(entity, pulsarInfo);
        if (StringUtils.isNotBlank(entity.getExtParams())) {
            PulsarClusterDTO dto = PulsarClusterDTO.getFromJson(entity.getExtParams());
            CommonBeanUtils.copyProperties(dto, pulsarInfo);
        }

        return pulsarInfo;
    }

    @Override
    protected void setTargetEntity(ClusterRequest request, InlongClusterEntity targetEntity) {
        PulsarClusterRequest pulsarRequest = (PulsarClusterRequest) request;
        CommonBeanUtils.copyProperties(pulsarRequest, targetEntity, true);
        try {
            PulsarClusterDTO dto = PulsarClusterDTO.getFromRequest(pulsarRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
            LOGGER.info("success to set entity for pulsar cluster");
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

    @Override
    public Boolean testConnection(ClusterRequest request) {
        PulsarClusterRequest pulsarRequest = (PulsarClusterRequest) request;
        PulsarClusterInfo pulsarInfo = new PulsarClusterInfo();
        CommonBeanUtils.copyProperties(pulsarRequest, pulsarInfo);
        try (PulsarAdmin ignored = PulsarUtils.getPulsarAdmin(pulsarInfo)) {
            LOGGER.info("pulsar connection not null - connection success for adminUrl={}", pulsarInfo.getAdminUrl());
            return true;
        } catch (Exception e) {
            String errMsg = String.format("pulsar connection failed for adminUrl=%s, password=%s",
                    pulsarInfo.getAdminUrl(), pulsarInfo.getToken());
            LOGGER.error(errMsg, e);
            throw new BusinessException(errMsg);
        }
    }

}
