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
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicInfo;
import org.apache.inlong.manager.pojo.group.none.InlongNoneMqInfo;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Inlong group operator without MQ.
 */
@Service
public class InlongGroupOperator4NoneMQ extends AbstractGroupOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongGroupOperator4NoneMQ.class);

    @Override
    public Boolean accept(String mqType) {
        return getMQType().equals(mqType);
    }

    @Override
    public String getMQType() {
        return MQType.NONE;
    }

    @Override
    protected void setTargetEntity(InlongGroupRequest request, InlongGroupEntity targetEntity) {
        // do nothing without mq
    }

    @Override
    public InlongGroupInfo getFromEntity(InlongGroupEntity entity) {
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }

        InlongNoneMqInfo groupInfo = new InlongNoneMqInfo();
        CommonBeanUtils.copyProperties(entity, groupInfo);
        return groupInfo;
    }

    @Override
    public InlongGroupTopicInfo getTopic(InlongGroupInfo groupInfo) {
        LOGGER.info("return null topic for inlong group without MQ");
        return null;
    }

}
