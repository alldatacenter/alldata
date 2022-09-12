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
import org.apache.inlong.manager.common.enums.GlobalConstants;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.common.pojo.group.InlongGroupTopicInfo;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Default operator of inlong group.
 */
public abstract class AbstractGroupOperator implements InlongGroupOperator {

    @Autowired
    protected InlongGroupEntityMapper groupMapper;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public String saveOpt(InlongGroupRequest request, String operator) {
        String groupId = request.getInlongGroupId();
        InlongGroupEntity entity = CommonBeanUtils.copyProperties(request, InlongGroupEntity::new);
        // if the mqResource was empty, fill with inlongGroupId
        if (StringUtils.isEmpty(entity.getMqResource())) {
            entity.setMqResource(groupId);
        }
        // set the ext params
        setTargetEntity(request, entity);

        // after saving, the status is set to [GROUP_WAIT_SUBMIT]
        entity.setStatus(GroupStatus.TO_BE_SUBMIT.getCode());
        entity.setIsDeleted(GlobalConstants.UN_DELETED);
        entity.setCreator(operator);
        entity.setCreateTime(new Date());

        groupMapper.insert(entity);
        return groupId;
    }

    /**
     * Set the parameters of the target entity.
     *
     * @param request inlong group request
     * @param targetEntity entity which will set the new parameters
     */
    protected abstract void setTargetEntity(InlongGroupRequest request, InlongGroupEntity targetEntity);

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ)
    public void updateOpt(InlongGroupRequest request, String operator) {
        InlongGroupEntity entity = CommonBeanUtils.copyProperties(request, InlongGroupEntity::new);

        // set the ext params
        setTargetEntity(request, entity);

        entity.setModifier(operator);
        entity.setModifyTime(new Date());
        groupMapper.updateByIdentifierSelective(entity);
    }

    @Override
    public InlongGroupTopicInfo getTopic(InlongGroupInfo groupInfo) {
        InlongGroupTopicInfo topicInfo = new InlongGroupTopicInfo();
        topicInfo.setInlongGroupId(groupInfo.getInlongGroupId());
        topicInfo.setMqType(groupInfo.getMqType());
        topicInfo.setMqResource(groupInfo.getMqResource());
        return topicInfo;
    }

}
