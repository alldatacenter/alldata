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

import com.github.pagehelper.Page;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.dao.entity.StreamSourceFieldEntity;
import org.apache.inlong.manager.dao.mapper.StreamSourceEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSourceFieldEntityMapper;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.source.SourceRequest;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Default operator of stream source.
 */
public abstract class AbstractSourceOperator implements StreamSourceOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSourceOperator.class);

    @Autowired
    protected StreamSourceEntityMapper sourceMapper;
    @Autowired
    protected StreamSourceFieldEntityMapper sourceFieldMapper;

    /**
     * Getting the source type.
     *
     * @return source type string.
     */
    protected abstract String getSourceType();

    /**
     * Setting the parameters of the latest entity.
     *
     * @param request source request
     * @param targetEntity entity object which will set the new parameters.
     */
    protected abstract void setTargetEntity(SourceRequest request, StreamSourceEntity targetEntity);

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Integer saveOpt(SourceRequest request, Integer groupStatus, String operator) {
        StreamSourceEntity entity = CommonBeanUtils.copyProperties(request, StreamSourceEntity::new);
        if (SourceType.AUTO_PUSH.equals(request.getSourceType())) {
            // auto push task needs not be issued to agent
            entity.setStatus(SourceStatus.SOURCE_NORMAL.getCode());
        } else if (GroupStatus.forCode(groupStatus).equals(GroupStatus.CONFIG_SUCCESSFUL)) {
            entity.setStatus(SourceStatus.TO_BE_ISSUED_ADD.getCode());
        } else {
            entity.setStatus(SourceStatus.SOURCE_NEW.getCode());
        }
        entity.setCreator(operator);
        entity.setModifier(operator);

        // get the ext params
        setTargetEntity(request, entity);
        sourceMapper.insert(entity);
        saveFieldOpt(entity, request.getFieldList());
        return entity.getId();
    }

    @Override
    public List<StreamField> getSourceFields(Integer sourceId) {
        List<StreamSourceFieldEntity> sourceFieldEntities = sourceFieldMapper.selectBySourceId(sourceId);
        return CommonBeanUtils.copyListProperties(sourceFieldEntities, StreamField::new);
    }

    @Override
    public PageResult<? extends StreamSource> getPageInfo(Page<StreamSourceEntity> entityPage) {
        if (CollectionUtils.isEmpty(entityPage)) {
            return PageResult.empty();
        }

        List<StreamSource> streamSources = entityPage.stream()
                .map(this::getFromEntity)
                .collect(Collectors.toList());
        return new PageResult<>(streamSources, entityPage.getTotal(), entityPage.getPageNum(), entityPage.size());
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ)
    public void updateOpt(SourceRequest request, Integer groupStatus, String operator) {
        StreamSourceEntity entity = sourceMapper.selectByIdForUpdate(request.getId());
        Preconditions.checkNotNull(entity, ErrorCodeEnum.SOURCE_INFO_NOT_FOUND.getMessage());

        if (SourceType.AUTO_PUSH.equals(entity.getSourceType())) {
            LOGGER.warn("auto push source {} can not be updated", entity.getSourceName());
            return;
        }
        if (!SourceStatus.ALLOWED_UPDATE.contains(entity.getStatus())) {
            throw new BusinessException(String.format("source=%s is not allowed to update, "
                    + "please wait until its changed to final status or stop / frozen / delete it firstly", entity));
        }
        String errMsg = String.format("source has already updated with groupId=%s, streamId=%s, name=%s, curVersion=%s",
                request.getInlongGroupId(), request.getInlongStreamId(), request.getSourceName(), request.getVersion());
        if (!Objects.equals(entity.getVersion(), request.getVersion())) {
            LOGGER.error(errMsg);
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }

        // source type cannot be changed
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

        // setting updated parameters of stream source entity.
        setTargetEntity(request, entity);
        entity.setModifier(operator);

        // re-issue task if necessary
        entity.setPreviousStatus(entity.getStatus());
        if (GroupStatus.forCode(groupStatus).equals(GroupStatus.CONFIG_SUCCESSFUL)) {
            entity.setStatus(SourceStatus.TO_BE_ISSUED_ADD.getCode());
        } else {
            switch (SourceStatus.forCode(entity.getStatus())) {
                case SOURCE_NORMAL:
                    entity.setStatus(SourceStatus.TO_BE_ISSUED_ADD.getCode());
                    break;
                case SOURCE_FAILED:
                    entity.setStatus(SourceStatus.SOURCE_NEW.getCode());
                    break;
                default:
                    // others leave it be
                    break;
            }
        }

        int rowCount = sourceMapper.updateByPrimaryKeySelective(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            LOGGER.warn(errMsg);
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
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
        curEntity.setPreviousStatus(curState.getCode());
        curEntity.setStatus(nextState.getCode());
        int rowCount = sourceMapper.updateByPrimaryKeySelective(curEntity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            LOGGER.error("source has already updated with groupId={}, streamId={}, name={}, curVersion={}",
                    curEntity.getInlongGroupId(), curEntity.getInlongStreamId(), curEntity.getSourceName(),
                    curEntity.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
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
        curEntity.setPreviousStatus(curState.getCode());
        curEntity.setStatus(nextState.getCode());
        int rowCount = sourceMapper.updateByPrimaryKeySelective(curEntity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            LOGGER.error("source has already updated with groupId={}, streamId={}, name={}, curVersion={}",
                    curEntity.getInlongGroupId(), curEntity.getInlongStreamId(), curEntity.getSourceName(),
                    curEntity.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
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

        LOGGER.info("success to update source fields");
    }

    private void saveFieldOpt(StreamSourceEntity entity, List<StreamField> fieldInfos) {
        LOGGER.info("begin to save source fields={}", fieldInfos);
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
            fieldEntity.setIsDeleted(InlongConstants.UN_DELETED);
            entityList.add(fieldEntity);
        }

        sourceFieldMapper.insertAll(entityList);
        LOGGER.info("success to save source fields");
    }
}
