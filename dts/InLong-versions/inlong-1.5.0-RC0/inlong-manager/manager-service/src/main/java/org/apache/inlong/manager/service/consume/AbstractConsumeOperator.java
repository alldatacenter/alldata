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
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ConsumeStatus;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.InlongConsumeEntity;
import org.apache.inlong.manager.dao.mapper.InlongConsumeEntityMapper;
import org.apache.inlong.manager.pojo.consume.InlongConsumeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default operator of inlong consume.
 */
public abstract class AbstractConsumeOperator implements InlongConsumeOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConsumeOperator.class);

    @Autowired
    private InlongConsumeEntityMapper consumeMapper;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Integer saveOpt(InlongConsumeRequest request, String operator) {
        // firstly check the topic info
        this.checkTopicInfo(request);

        // set the ext params, init status, and other info
        InlongConsumeEntity entity = CommonBeanUtils.copyProperties(request, InlongConsumeEntity::new);
        this.setTargetEntity(request, entity);
        entity.setStatus(ConsumeStatus.TO_BE_SUBMIT.getCode());
        entity.setCreator(operator);
        entity.setModifier(operator);

        consumeMapper.insert(entity);
        return entity.getId();
    }

    /**
     * Set the parameters of the target entity.
     *
     * @param request inlong consume request
     * @param targetEntity targetEntity which will set the new parameters
     */
    protected abstract void setTargetEntity(InlongConsumeRequest request, InlongConsumeEntity targetEntity);

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ)
    public void updateOpt(InlongConsumeRequest request, String operator) {
        // firstly check the topic info
        if (StringUtils.isNotBlank(request.getTopic())) {
            this.checkTopicInfo(request);
        }
        // get the entity from request
        InlongConsumeEntity entity = CommonBeanUtils.copyProperties(request, InlongConsumeEntity::new);
        // set the ext params
        this.setTargetEntity(request, entity);
        entity.setModifier(operator);

        int rowCount = consumeMapper.updateByIdSelective(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            LOGGER.error("inlong consume has already updated with id={}, expire version={}",
                    request.getId(), request.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
    }

}
