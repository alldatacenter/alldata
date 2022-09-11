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

package org.apache.inlong.manager.service.source;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GlobalConstants;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.source.SourceRequest;
import org.apache.inlong.manager.common.pojo.source.StreamSource;
import org.apache.inlong.manager.common.pojo.stream.StreamField;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.dao.entity.StreamSourceFieldEntity;
import org.apache.inlong.manager.dao.mapper.StreamSourceEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSourceFieldEntityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Default operation of stream source.
 */
public abstract class AbstractSourceOperation implements StreamSourceOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSourceOperation.class);
    @Autowired
    protected StreamSourceEntityMapper sourceMapper;
    @Autowired
    protected StreamSourceFieldEntityMapper sourceFieldMapper;

    /**
     * Setting the parameters of the latest entity.
     *
     * @param request source request
     * @param targetEntity entity object which will set the new parameters.
     */
    protected abstract void setTargetEntity(SourceRequest request, StreamSourceEntity targetEntity);

    /**
     * Getting the source type.
     *
     * @return source type string.
     */
    protected abstract String getSourceType();

    /**
     * Creating source object.
     *
     * @return source object
     */
    protected abstract StreamSource getSource();

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Integer saveOpt(SourceRequest request, Integer groupStatus, String operator) {
        String groupId = request.getInlongGroupId();
        String streamId = request.getInlongStreamId();
        String sourceName = request.getSourceName();
        List<StreamSourceEntity> existList = sourceMapper.selectByRelatedId(groupId, streamId, sourceName);
        if (CollectionUtils.isNotEmpty(existList)) {
            String err = "source name=%s already exists with groupId=%s streamId=%s";
            throw new BusinessException(String.format(err, sourceName, groupId, streamId));
        }

        StreamSourceEntity entity = CommonBeanUtils.copyProperties(request, StreamSourceEntity::new);
        entity.setVersion(1);
        if (GroupStatus.forCode(groupStatus).equals(GroupStatus.CONFIG_SUCCESSFUL)) {
            entity.setStatus(SourceStatus.TO_BE_ISSUED_ADD.getCode());
        } else {
            entity.setStatus(SourceStatus.SOURCE_NEW.getCode());
        }
        entity.setCreator(operator);
        entity.setModifier(operator);
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setModifyTime(now);
        entity.setIsDeleted(GlobalConstants.UN_DELETED);
        // get the ext params
        setTargetEntity(request, entity);
        sourceMapper.insert(entity);
        saveFieldOpt(entity, request.getFieldList());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.NOT_SUPPORTED)
    public StreamSource getByEntity(@NotNull StreamSourceEntity entity) {
        Preconditions.checkNotNull(entity, ErrorCodeEnum.SOURCE_INFO_NOT_FOUND.getMessage());
        String existType = entity.getSourceType();
        Preconditions.checkTrue(getSourceType().equals(existType),
                String.format(ErrorCodeEnum.SOURCE_TYPE_NOT_SAME.getMessage(), getSourceType(), existType));

        StreamSource source = this.getFromEntity(entity, this::getSource);
        List<StreamSourceFieldEntity> sourceFieldEntities = sourceFieldMapper.selectBySourceId(entity.getId());
        List<StreamField> fieldInfos = CommonBeanUtils.copyListProperties(sourceFieldEntities,
                StreamField::new);
        source.setFieldList(fieldInfos);
        return source;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ)
    public void updateOpt(SourceRequest request, Integer groupStatus, String operator) {
        StreamSourceEntity entity = sourceMapper.selectByIdForUpdate(request.getId());
        Preconditions.checkNotNull(entity, ErrorCodeEnum.SOURCE_INFO_NOT_FOUND.getMessage());
        if (!SourceStatus.ALLOWED_UPDATE.contains(entity.getStatus())) {
            throw new BusinessException(String.format("source=%s is not allowed to update, "
                    + "please wait until its changed to final status or stop / frozen / delete it firstly", entity));
        }

        // Source type cannot be changed
        if (!Objects.equals(entity.getSourceType(), request.getSourceType())) {
            throw new BusinessException(String.format("source type=%s cannot change to %s",
                    entity.getSourceType(), request.getSourceType()));
        }

        String groupId = request.getInlongGroupId();
        String streamId = request.getInlongStreamId();
        String sourceName = request.getSourceName();
        List<StreamSourceEntity> sourceList = sourceMapper.selectByRelatedId(groupId, streamId, sourceName);
        for (StreamSourceEntity sourceEntity : sourceList) {
            Integer sourceId = sourceEntity.getId();
            if (!Objects.equals(sourceId, request.getId())) {
                String err = "source name=%s already exists with the groupId=%s streamId=%s";
                throw new BusinessException(String.format(err, sourceName, groupId, streamId));
            }
        }

        // Setting updated parameters of stream source entity.
        setTargetEntity(request, entity);
        entity.setVersion(entity.getVersion() + 1);
        entity.setModifier(operator);
        entity.setModifyTime(new Date());
        sourceMapper.updateByPrimaryKeySelective(entity);
        updateFieldOpt(entity, request.getFieldList());
        LOGGER.info("success to update source of type={}", request.getSourceType());
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ)
    public void stopOpt(SourceRequest request, String operator) {
        StreamSourceEntity existEntity = sourceMapper.selectByIdForUpdate(request.getId());
        SourceStatus curState = SourceStatus.forCode(existEntity.getStatus());
        SourceStatus nextState = SourceStatus.TO_BE_ISSUED_FROZEN;
        if (!SourceStatus.isAllowedTransition(curState, nextState)) {
            throw new BusinessException(String.format("source=%s is not allowed to stop", existEntity));
        }
        StreamSourceEntity curEntity = CommonBeanUtils.copyProperties(request, StreamSourceEntity::new);
        curEntity.setVersion(existEntity.getVersion() + 1);
        curEntity.setModifyTime(new Date());
        curEntity.setPreviousStatus(curState.getCode());
        curEntity.setStatus(nextState.getCode());
        sourceMapper.updateByPrimaryKeySelective(curEntity);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ)
    public void restartOpt(SourceRequest request, String operator) {
        StreamSourceEntity existEntity = sourceMapper.selectByIdForUpdate(request.getId());
        SourceStatus curState = SourceStatus.forCode(existEntity.getStatus());
        SourceStatus nextState = SourceStatus.TO_BE_ISSUED_ACTIVE;
        if (!SourceStatus.isAllowedTransition(curState, nextState)) {
            throw new BusinessException(String.format("Source=%s is not allowed to restart", existEntity));
        }
        StreamSourceEntity curEntity = CommonBeanUtils.copyProperties(request, StreamSourceEntity::new);
        curEntity.setVersion(existEntity.getVersion() + 1);
        curEntity.setModifyTime(new Date());
        curEntity.setPreviousStatus(curState.getCode());
        curEntity.setStatus(nextState.getCode());

        sourceMapper.updateByPrimaryKeySelective(curEntity);
    }

    private void updateFieldOpt(StreamSourceEntity entity, List<StreamField> fieldInfos) {
        Integer sourceId = entity.getId();
        if (CollectionUtils.isEmpty(fieldInfos)) {
            return;
        }

        // First physically delete the existing fields
        sourceFieldMapper.deleteAll(sourceId);
        // Then batch save the source fields
        this.saveFieldOpt(entity, fieldInfos);

        LOGGER.info("success to update field");
    }

    private void saveFieldOpt(StreamSourceEntity entity, List<StreamField> fieldInfos) {
        LOGGER.info("begin to save source field={}", fieldInfos);
        if (CollectionUtils.isEmpty(fieldInfos)) {
            return;
        }

        int size = fieldInfos.size();
        List<StreamSourceFieldEntity> entityList = new ArrayList<>(size);
        String groupId = entity.getInlongGroupId();
        String streamId = entity.getInlongStreamId();
        String sourceType = entity.getSourceType();
        Integer sourceId = entity.getId();
        for (StreamField fieldInfo : fieldInfos) {
            StreamSourceFieldEntity fieldEntity = CommonBeanUtils.copyProperties(fieldInfo,
                    StreamSourceFieldEntity::new);
            if (StringUtils.isEmpty(fieldEntity.getFieldComment())) {
                fieldEntity.setFieldComment(fieldEntity.getFieldName());
            }
            fieldEntity.setInlongGroupId(groupId);
            fieldEntity.setInlongStreamId(streamId);
            fieldEntity.setSourceId(sourceId);
            fieldEntity.setSourceType(sourceType);
            fieldEntity.setIsDeleted(GlobalConstants.UN_DELETED);
            entityList.add(fieldEntity);
        }

        sourceFieldMapper.insertAll(entityList);
        LOGGER.info("success to save source fields");
    }
}
