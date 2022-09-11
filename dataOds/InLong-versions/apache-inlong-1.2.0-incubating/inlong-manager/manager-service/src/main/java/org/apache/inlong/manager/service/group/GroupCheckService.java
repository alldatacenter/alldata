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
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Service of inlong group check.
 */
@Service
public class GroupCheckService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupCheckService.class);

    @Autowired
    private InlongGroupEntityMapper groupMapper;

    /**
     * Check whether the inlong group status is temporary
     *
     * @param groupId Inlong group id
     * @return Inlong group entity, for caller reuse
     */
    public InlongGroupEntity checkGroupStatus(String groupId, String operator) {
        InlongGroupEntity inlongGroupEntity = groupMapper.selectByGroupId(groupId);
        Preconditions.checkNotNull(inlongGroupEntity, "groupId is invalid");

        List<String> managers = Arrays.asList(inlongGroupEntity.getInCharges().split(","));
        Preconditions.checkTrue(managers.contains(operator),
                String.format(ErrorCodeEnum.USER_IS_NOT_MANAGER.getMessage(), operator, managers));

        GroupStatus state = GroupStatus.forCode(inlongGroupEntity.getStatus());
        // Add/modify/delete is not allowed under certain group state
        if (GroupStatus.notAllowedUpdate(state)) {
            LOGGER.error("inlong group status was not allowed to add/update/delete related info");
            throw new BusinessException(ErrorCodeEnum.OPT_NOT_ALLOWED_BY_STATUS);
        }

        return inlongGroupEntity;
    }

}
