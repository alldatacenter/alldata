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

package org.apache.inlong.manager.service.source.autopush;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.pojo.source.SourceRequest;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.source.autopush.AutoPushSource;
import org.apache.inlong.manager.pojo.source.autopush.AutoPushSourceDTO;
import org.apache.inlong.manager.pojo.source.autopush.AutoPushSourceRequest;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.service.source.AbstractSourceOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DataProxy SDK source operator
 */
@Service
public class AutoPushSourceOperator extends AbstractSourceOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoPushSourceOperator.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String sourceType) {
        return SourceType.AUTO_PUSH.equals(sourceType);
    }

    @Override
    protected String getSourceType() {
        return SourceType.AUTO_PUSH;
    }

    @Override
    protected void setTargetEntity(SourceRequest request, StreamSourceEntity targetEntity) {
        AutoPushSourceRequest sourceRequest = (AutoPushSourceRequest) request;
        CommonBeanUtils.copyProperties(sourceRequest, targetEntity, true);
        try {
            AutoPushSourceDTO dto = AutoPushSourceDTO.getFromRequest(sourceRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            LOGGER.error("parsing json string to source info failed", e);
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

    @Override
    public StreamSource getFromEntity(StreamSourceEntity entity) {
        AutoPushSource source = new AutoPushSource();
        if (entity == null) {
            return source;
        }

        AutoPushSourceDTO dto = AutoPushSourceDTO.getFromJson(entity.getExtParams());
        CommonBeanUtils.copyProperties(entity, source, true);
        CommonBeanUtils.copyProperties(dto, source, true);

        List<StreamField> sourceFields = super.getSourceFields(entity.getId());
        source.setFieldList(sourceFields);
        return source;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ)
    public void restartOpt(SourceRequest request, String operator) {
        StreamSourceEntity existEntity = sourceMapper.selectByIdForUpdate(request.getId());
        SourceStatus curState = SourceStatus.forCode(existEntity.getStatus());
        SourceStatus nextState = SourceStatus.SOURCE_NORMAL;
        StreamSourceEntity curEntity = CommonBeanUtils.copyProperties(request, StreamSourceEntity::new);
        curEntity.setPreviousStatus(curState.getCode());
        curEntity.setStatus(nextState.getCode());
        sourceMapper.updateByPrimaryKeySelective(curEntity);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ)
    public void stopOpt(SourceRequest request, String operator) {
        StreamSourceEntity existEntity = sourceMapper.selectByIdForUpdate(request.getId());
        SourceStatus curState = SourceStatus.forCode(existEntity.getStatus());
        SourceStatus nextState = SourceStatus.SOURCE_FROZEN;
        StreamSourceEntity curEntity = CommonBeanUtils.copyProperties(request, StreamSourceEntity::new);
        curEntity.setPreviousStatus(curState.getCode());
        curEntity.setStatus(nextState.getCode());
        sourceMapper.updateByPrimaryKeySelective(curEntity);
    }

}
