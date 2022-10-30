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

package org.apache.inlong.manager.service.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.consts.MQType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicInfo;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarDTO;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarInfo;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarRequest;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Inlong group operator for Pulsar.
 */
@Service
public class InlongPulsarOperator extends AbstractGroupOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongPulsarOperator.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String mqType) {
        return getMQType().equals(mqType) || MQType.TDMQ_PULSAR.equals(mqType);
    }

    @Override
    public String getMQType() {
        return MQType.PULSAR;
    }

    @Override
    public InlongGroupInfo getFromEntity(InlongGroupEntity entity) {
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }

        InlongPulsarInfo groupInfo = new InlongPulsarInfo();
        CommonBeanUtils.copyProperties(entity, groupInfo);

        if (StringUtils.isNotBlank(entity.getExtParams())) {
            InlongPulsarDTO dto = InlongPulsarDTO.getFromJson(entity.getExtParams());
            CommonBeanUtils.copyProperties(dto, groupInfo);
        }

        // TODO get the cluster from inlong_cluster by entity.getInlongClusterTag()
        // groupInfo.setTenant();
        // groupInfo.setAdminUrl();
        // groupInfo.setServiceUrl();
        return groupInfo;
    }

    @Override
    protected void setTargetEntity(InlongGroupRequest request, InlongGroupEntity targetEntity) {
        InlongPulsarRequest pulsarRequest = (InlongPulsarRequest) request;

        // Pulsar params must meet: ackQuorum <= writeQuorum <= ensemble
        Integer ackQuorum = pulsarRequest.getAckQuorum();
        Integer writeQuorum = pulsarRequest.getWriteQuorum();
        Preconditions.checkNotNull(ackQuorum, "Pulsar ackQuorum cannot be empty");
        Preconditions.checkNotNull(writeQuorum, "Pulsar writeQuorum cannot be empty");
        if (!(ackQuorum <= writeQuorum)) {
            throw new BusinessException(ErrorCodeEnum.GROUP_SAVE_FAILED,
                    "Pulsar params must meet: ackQuorum <= writeQuorum");
        }
        // The default value of ensemble is writeQuorum
        pulsarRequest.setEnsemble(writeQuorum);

        CommonBeanUtils.copyProperties(pulsarRequest, targetEntity, true);
        try {
            InlongPulsarDTO dto = InlongPulsarDTO.getFromRequest(pulsarRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
        LOGGER.info("success set entity for inlong group with Pulsar");
    }

    @Override
    public InlongGroupTopicInfo getTopic(InlongGroupInfo groupInfo) {
        InlongGroupTopicInfo topicInfo = super.getTopic(groupInfo);
        // TODO add cache for cluster info
        // pulsar topic corresponds to the inlong stream one-to-one
        // topicInfo.setDsTopicList(streamService.getTopicList(groupInfo.getInlongGroupId()));
        // commonOperateService.getSpecifiedParam(InlongConstants.TUBE_MASTER_URL);
        // groupInfo.setTenant();
        // groupInfo.setAdminUrl();
        // groupInfo.setServiceUrl();
        return topicInfo;
    }

}
