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

import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.consts.MQType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicInfo;
import org.apache.inlong.manager.pojo.group.tubemq.InlongTubeMQInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Inlong group operator for TubeMQ.
 */
@Service
public class InlongTubeOperator extends AbstractGroupOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongTubeOperator.class);

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

        // TODO get the cluster
        // groupInfo.setTubeMaster();
        return groupInfo;
    }

    @Override
    protected void setTargetEntity(InlongGroupRequest request, InlongGroupEntity targetEntity) {
        LOGGER.info("do nothing for inlong group with TubeMQ");
    }

    @Override
    public InlongGroupTopicInfo getTopic(InlongGroupInfo groupInfo) {
        // TODO add cache for cluster info
        // topicInfo.setTubeMasterUrl(groupInfo.getMqType());
        return super.getTopic(groupInfo);
    }

}
