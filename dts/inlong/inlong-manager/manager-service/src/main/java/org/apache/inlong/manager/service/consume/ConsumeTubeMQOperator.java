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

package org.apache.inlong.manager.service.consume;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongConsumeEntity;
import org.apache.inlong.manager.pojo.consume.InlongConsumeInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeRequest;
import org.apache.inlong.manager.pojo.consume.pulsar.ConsumePulsarInfo;
import org.apache.inlong.manager.pojo.consume.tubemq.ConsumeTubeMQDTO;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicInfo;
import org.apache.inlong.manager.pojo.group.tubemq.InlongTubeMQTopicInfo;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Inlong consume operator for TubeMQ.
 */
@Service
public class ConsumeTubeMQOperator extends AbstractConsumeOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumeTubeMQOperator.class);

    @Autowired
    private InlongGroupService groupService;

    @Override
    public Boolean accept(String mqType) {
        return getMQType().equals(mqType);
    }

    @Override
    public String getMQType() {
        return MQType.TUBEMQ;
    }

    @Override
    public void checkTopicInfo(InlongConsumeRequest request) {
        String groupId = request.getInlongGroupId();
        InlongGroupTopicInfo topicInfo = groupService.getTopic(groupId);
        Preconditions.expectNotNull(topicInfo, "inlong group not exist: " + groupId);

        // one inlong group only has one TubeMQ topic
        InlongTubeMQTopicInfo tubeMQTopic = (InlongTubeMQTopicInfo) topicInfo;
        Preconditions.expectTrue(Objects.equals(tubeMQTopic.getTopic(), request.getTopic()),
                String.format("topic %s for consume not belongs to inlong group %s", request.getTopic(), groupId));
    }

    @Override
    public InlongConsumeInfo getFromEntity(InlongConsumeEntity entity) {
        Preconditions.expectNotNull(entity, ErrorCodeEnum.CONSUME_NOT_FOUND.getMessage());

        ConsumePulsarInfo consumeInfo = new ConsumePulsarInfo();
        CommonBeanUtils.copyProperties(entity, consumeInfo);
        if (StringUtils.isNotBlank(entity.getExtParams())) {
            ConsumeTubeMQDTO dto = ConsumeTubeMQDTO.getFromJson(entity.getExtParams());
            CommonBeanUtils.copyProperties(dto, consumeInfo);
        }

        return consumeInfo;
    }

    @Override
    protected void setTargetEntity(InlongConsumeRequest request, InlongConsumeEntity targetEntity) {
        LOGGER.info("do nothing for inlong consume with TubeMQ");
    }

}
