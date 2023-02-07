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

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.InlongGroupExtEntity;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicInfo;
import org.apache.inlong.manager.pojo.group.tubemq.InlongTubeMQDTO;
import org.apache.inlong.manager.pojo.group.tubemq.InlongTubeMQInfo;
import org.apache.inlong.manager.pojo.group.tubemq.InlongTubeMQRequest;
import org.apache.inlong.manager.pojo.group.tubemq.InlongTubeMQTopicInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static org.apache.inlong.common.constant.ClusterSwitch.BACKUP_MQ_RESOURCE;

/**
 * Inlong group operator for TubeMQ.
 */
@Service
public class InlongGroupOperator4TubeMQ extends AbstractGroupOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongGroupOperator4TubeMQ.class);

    @Override
    public Boolean accept(String mqType) {
        return getMQType().equals(mqType);
    }

    @Override
    public String getMQType() {
        return MQType.TUBEMQ;
    }

    @Override
    public InlongTubeMQInfo getFromEntity(InlongGroupEntity entity) {
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }

        InlongTubeMQInfo groupInfo = new InlongTubeMQInfo();
        CommonBeanUtils.copyProperties(entity, groupInfo);
        if (StringUtils.isNotBlank(entity.getExtParams())) {
            InlongTubeMQDTO dto = InlongTubeMQDTO.getFromJson(entity.getExtParams());
            CommonBeanUtils.copyProperties(dto, groupInfo);
        }
        // TODO get the cluster
        // groupInfo.setTubeMaster();
        return groupInfo;
    }

    @Override
    protected void setTargetEntity(InlongGroupRequest request, InlongGroupEntity targetEntity) {
        InlongTubeMQRequest tubeMQRequest = (InlongTubeMQRequest) request;
        try {
            InlongTubeMQDTO dto = InlongTubeMQDTO.getFromRequest(tubeMQRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT,
                    String.format("serialize extParams of TubeMQ failure: %s", e.getMessage()));
        }
    }

    @Override
    public InlongGroupTopicInfo getTopic(InlongGroupInfo groupInfo) {
        InlongTubeMQTopicInfo topicInfo = new InlongTubeMQTopicInfo();
        // each inlong group is associated with a TubeMQ topic
        topicInfo.setTopic(groupInfo.getMqResource());
        return topicInfo;
    }

    @Override
    public InlongGroupTopicInfo getBackupTopic(InlongGroupInfo groupInfo) {
        // set backup topic, each inlong group is associated with a TubeMQ topic
        InlongTubeMQTopicInfo topicInfo = new InlongTubeMQTopicInfo();
        InlongGroupExtEntity extEntity = groupExtMapper.selectByUniqueKey(groupInfo.getInlongGroupId(),
                BACKUP_MQ_RESOURCE);
        if (extEntity != null && StringUtils.isNotBlank(extEntity.getKeyValue())) {
            topicInfo.setTopic(extEntity.getKeyValue());
        } else {
            topicInfo.setTopic(groupInfo.getMqResource());
        }

        return topicInfo;
    }

}
