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
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.InlongGroupExtEntity;
import org.apache.inlong.manager.dao.entity.InlongStreamExtEntity;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicInfo;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarDTO;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarInfo;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarRequest;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarTopicInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamBriefInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.inlong.common.constant.ClusterSwitch.BACKUP_MQ_RESOURCE;

/**
 * Inlong group operator for Pulsar.
 */
@Service
public class InlongGroupOperator4Pulsar extends AbstractGroupOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongGroupOperator4Pulsar.class);

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
        if (ackQuorum == null) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "Pulsar ackQuorum cannot be empty");
        }
        if (writeQuorum == null) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "Pulsar writeQuorum cannot be empty");
        }
        if (ackQuorum < 0 || writeQuorum < 0) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "Pulsar ackQuorum or writeQuorum must greater than or equal to 0");
        }
        if (!(ackQuorum <= writeQuorum)) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "Pulsar params must meet: ackQuorum <= writeQuorum");
        }
        // The default value of ensemble is writeQuorum
        pulsarRequest.setEnsemble(writeQuorum);
        CommonBeanUtils.copyProperties(pulsarRequest, targetEntity, true);
        try {
            InlongPulsarDTO dto = InlongPulsarDTO.getFromRequest(pulsarRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT,
                    String.format("serialize extParams of Pulsar failure: %s", e.getMessage()));
        }
    }

    @Override
    public InlongGroupTopicInfo getTopic(InlongGroupInfo groupInfo) {
        PulsarClusterInfo pulsarCluster = (PulsarClusterInfo) clusterService.getOne(
                groupInfo.getInlongClusterTag(), null, ClusterType.PULSAR);

        // First get the tenant from the InlongGroup, and then get it from the PulsarCluster.
        String tenant = ((InlongPulsarInfo) groupInfo).getTenant();
        if (StringUtils.isBlank(tenant)) {
            tenant = pulsarCluster.getTenant();
        }
        InlongPulsarTopicInfo topicInfo = new InlongPulsarTopicInfo();
        topicInfo.setTenant(tenant);
        topicInfo.setNamespace(groupInfo.getMqResource());
        // each inlong stream is associated with a Pulsar topic
        List<String> topics = streamService.getTopicList(groupInfo.getInlongGroupId()).stream()
                .map(InlongStreamBriefInfo::getMqResource)
                .collect(Collectors.toList());
        topicInfo.setTopics(topics);

        return topicInfo;
    }

    @Override
    public InlongGroupTopicInfo getBackupTopic(InlongGroupInfo groupInfo) {
        // set backup namespace
        String groupId = groupInfo.getInlongGroupId();
        InlongGroupExtEntity extEntity = groupExtMapper.selectByUniqueKey(groupId, BACKUP_MQ_RESOURCE);
        InlongPulsarTopicInfo topicInfo = new InlongPulsarTopicInfo();
        if (extEntity != null && StringUtils.isNotBlank(extEntity.getKeyValue())) {
            topicInfo.setNamespace(extEntity.getKeyValue());
        } else {
            topicInfo.setNamespace(groupInfo.getMqResource());
        }

        // set backup topics, each inlong stream is associated with a Pulsar topic
        List<InlongStreamBriefInfo> streamTopics = streamService.getTopicList(groupId);
        List<String> topics = streamTopics.stream()
                .map(stream -> {
                    InlongStreamExtEntity streamExtEntity = streamExtMapper.selectByKey(groupId,
                            stream.getInlongStreamId(), BACKUP_MQ_RESOURCE);
                    if (streamExtEntity != null && StringUtils.isNotBlank(streamExtEntity.getKeyValue())) {
                        return streamExtEntity.getKeyValue();
                    }
                    return stream.getMqResource();
                })
                .collect(Collectors.toList());

        topicInfo.setTopics(topics);

        return topicInfo;
    }

}
