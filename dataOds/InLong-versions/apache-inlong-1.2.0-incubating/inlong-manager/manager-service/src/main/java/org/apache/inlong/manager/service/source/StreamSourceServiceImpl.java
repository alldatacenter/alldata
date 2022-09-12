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
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.common.enums.SourceType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.source.SourceListResponse;
import org.apache.inlong.manager.common.pojo.source.SourcePageRequest;
import org.apache.inlong.manager.common.pojo.source.SourceRequest;
import org.apache.inlong.manager.common.pojo.source.StreamSource;
import org.apache.inlong.manager.common.pojo.stream.StreamField;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.dao.mapper.StreamSourceEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSourceFieldEntityMapper;
import org.apache.inlong.manager.service.group.GroupCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Implementation of source service interface
 */
@Service
public class StreamSourceServiceImpl implements StreamSourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamSourceServiceImpl.class);

    @Autowired
    private SourceOperationFactory operationFactory;
    @Autowired
    private StreamSourceEntityMapper sourceMapper;
    @Autowired
    private StreamSourceFieldEntityMapper sourceFieldMapper;
    @Autowired
    private GroupCheckService groupCheckService;

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public Integer save(SourceRequest request, String operator) {
        LOGGER.info("begin to save source info: {}", request);
        this.checkParams(request);

        // Check if it can be added
        String groupId = request.getInlongGroupId();
        InlongGroupEntity groupEntity = groupCheckService.checkGroupStatus(groupId, operator);

        // According to the source type, save source information
        String sourceType = request.getSourceType();
        StreamSourceOperation operation = operationFactory.getInstance(SourceType.forType(sourceType));
        // Remove id in sourceField when save
        List<StreamField> streamFields = request.getFieldList();
        if (CollectionUtils.isNotEmpty(streamFields)) {
            streamFields.stream().forEach(streamField -> streamField.setId(null));
        }
        int id = operation.saveOpt(request, groupEntity.getStatus(), operator);

        LOGGER.info("success to save source info: {}", request);
        return id;
    }

    @Override
    public StreamSource get(Integer id) {
        Preconditions.checkNotNull(id, "source id is empty");
        StreamSourceEntity entity = sourceMapper.selectById(id);
        if (entity == null) {
            LOGGER.error("source not found by id={}", id);
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_NOT_FOUND);
        }
        StreamSourceOperation operation = operationFactory.getInstance(SourceType.forType(entity.getSourceType()));
        StreamSource streamSource = operation.getByEntity(entity);
        LOGGER.debug("success to get source by id={}", id);
        return streamSource;
    }

    @Override
    public Integer getCount(String groupId, String streamId) {
        Integer count = sourceMapper.selectCount(groupId, streamId);
        LOGGER.debug("source count={} with groupId={}, streamId={}", count, groupId, streamId);
        return count;
    }

    @Override
    public List<StreamSource> listSource(String groupId, String streamId) {
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        List<StreamSourceEntity> entityList = sourceMapper.selectByRelatedId(groupId, streamId, null);
        if (CollectionUtils.isEmpty(entityList)) {
            return Collections.emptyList();
        }
        List<StreamSource> responseList = new ArrayList<>();
        entityList.forEach(entity -> responseList.add(this.get(entity.getId())));

        LOGGER.debug("success to list source by groupId={}, streamId={}", groupId, streamId);
        return responseList;
    }

    @Override
    public PageInfo<? extends SourceListResponse> listByCondition(SourcePageRequest request) {
        Preconditions.checkNotNull(request.getInlongGroupId(), ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());

        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<StreamSourceEntity> entityList = sourceMapper.selectByCondition(request);

        // Encapsulate the paging query results into the PageInfo object to obtain related paging information
        Map<SourceType, Page<StreamSourceEntity>> sourceMap = Maps.newHashMap();
        for (StreamSourceEntity entity : entityList) {
            SourceType sourceType = SourceType.forType(entity.getSourceType());
            sourceMap.computeIfAbsent(sourceType, k -> new Page<>()).add(entity);
        }
        List<SourceListResponse> responseList = Lists.newArrayList();
        for (Map.Entry<SourceType, Page<StreamSourceEntity>> entry : sourceMap.entrySet()) {
            SourceType sourceType = entry.getKey();
            StreamSourceOperation operation = operationFactory.getInstance(sourceType);
            PageInfo<? extends SourceListResponse> pageInfo = operation.getPageInfo(entry.getValue());
            if (null != pageInfo && CollectionUtils.isNotEmpty(pageInfo.getList())) {
                responseList.addAll(pageInfo.getList());
            }
        }
        PageInfo<? extends SourceListResponse> pageInfo = PageInfo.of(responseList);

        LOGGER.debug("success to list source page, result size {}", pageInfo.getSize());
        return pageInfo;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED)
    public boolean update(SourceRequest request, String operator) {
        LOGGER.info("begin to update source info: {}", request);
        this.checkParams(request);
        Preconditions.checkNotNull(request.getId(), ErrorCodeEnum.ID_IS_EMPTY.getMessage());

        // Check if it can be modified
        String groupId = request.getInlongGroupId();
        InlongGroupEntity groupEntity = groupCheckService.checkGroupStatus(groupId, operator);

        String sourceType = request.getSourceType();
        StreamSourceOperation operation = operationFactory.getInstance(SourceType.forType(sourceType));
        // Remove id in sourceField when save
        List<StreamField> streamFields = request.getFieldList();
        if (CollectionUtils.isNotEmpty(streamFields)) {
            streamFields.stream().forEach(streamField -> streamField.setId(null));
        }
        operation.updateOpt(request, groupEntity.getStatus(), operator);

        LOGGER.info("success to update source info: {}", request);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED)
    public boolean updateStatus(String groupId, String streamId, Integer targetStatus, String operator) {
        sourceMapper.updateStatusByRelatedId(groupId, streamId, targetStatus);
        LOGGER.info("success to update source status={} for groupId={}, streamId={} by {}",
                targetStatus, groupId, streamId, operator);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED)
    public boolean delete(Integer id, String operator) {
        LOGGER.info("begin to delete source for id={} by user={}", id, operator);
        Preconditions.checkNotNull(id, ErrorCodeEnum.ID_IS_EMPTY.getMessage());

        StreamSourceEntity entity = sourceMapper.selectByIdForUpdate(id);
        Preconditions.checkNotNull(entity, ErrorCodeEnum.SOURCE_INFO_NOT_FOUND.getMessage());
        groupCheckService.checkGroupStatus(entity.getInlongGroupId(), operator);

        SourceStatus curStatus = SourceStatus.forCode(entity.getStatus());
        SourceStatus nextStatus = SourceStatus.TO_BE_ISSUED_DELETE;
        // if source is frozen|failed|new , delete directly
        if (curStatus == SourceStatus.SOURCE_FROZEN || curStatus == SourceStatus.SOURCE_FAILED
                || curStatus == SourceStatus.SOURCE_NEW) {
            nextStatus = SourceStatus.SOURCE_DISABLE;
        }
        if (!SourceStatus.isAllowedTransition(curStatus, nextStatus)) {
            throw new BusinessException(String.format("Source=%s is not allowed to delete", entity));
        }

        entity.setVersion(entity.getVersion() + 1);
        entity.setPreviousStatus(curStatus.getCode());
        entity.setStatus(nextStatus.getCode());
        entity.setIsDeleted(id);
        entity.setModifyTime(new Date());
        sourceMapper.updateByPrimaryKeySelective(entity);
        sourceFieldMapper.deleteAll(id);

        LOGGER.info("success to delete source for id={} by user={}", id, operator);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED)
    public boolean restart(Integer id, String operator) {
        LOGGER.info("begin to restart source by id={}", id);
        StreamSourceEntity entity = sourceMapper.selectByIdForUpdate(id);
        Preconditions.checkNotNull(entity, ErrorCodeEnum.SOURCE_INFO_NOT_FOUND.getMessage());
        groupCheckService.checkGroupStatus(entity.getInlongGroupId(), operator);

        StreamSourceOperation operation = operationFactory.getInstance(SourceType.forType(entity.getSourceType()));
        SourceRequest sourceRequest = new SourceRequest();
        CommonBeanUtils.copyProperties(entity, sourceRequest, true);
        operation.restartOpt(sourceRequest, operator);

        LOGGER.info("success to restart source info: {}", entity);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED)
    public boolean stop(Integer id, String operator) {
        LOGGER.info("begin to stop source by id={}", id);
        StreamSourceEntity entity = sourceMapper.selectByIdForUpdate(id);
        Preconditions.checkNotNull(entity, ErrorCodeEnum.SOURCE_INFO_NOT_FOUND.getMessage());
        groupCheckService.checkGroupStatus(entity.getInlongGroupId(), operator);

        StreamSourceOperation operation = operationFactory.getInstance(SourceType.forType(entity.getSourceType()));
        SourceRequest sourceRequest = new SourceRequest();
        CommonBeanUtils.copyProperties(entity, sourceRequest, true);
        operation.stopOpt(sourceRequest, operator);

        LOGGER.info("success to stop source info: {}", entity);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED)
    public boolean logicDeleteAll(String groupId, String streamId, String operator) {
        LOGGER.info("begin to logic delete all source info by groupId={}, streamId={}", groupId, streamId);
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());

        // Check if it can be deleted
        InlongGroupEntity groupEntity = groupCheckService.checkGroupStatus(groupId, operator);
        Integer nextStatus;
        if (GroupStatus.CONFIG_SUCCESSFUL.getCode().equals(groupEntity.getStatus())) {
            nextStatus = SourceStatus.TO_BE_ISSUED_DELETE.getCode();
        } else {
            nextStatus = SourceStatus.SOURCE_DISABLE.getCode();
        }
        Date now = new Date();
        List<StreamSourceEntity> entityList = sourceMapper.selectByRelatedId(groupId, streamId, null);
        if (CollectionUtils.isNotEmpty(entityList)) {
            for (StreamSourceEntity entity : entityList) {
                Integer id = entity.getId();
                entity.setVersion(entity.getVersion() + 1);
                entity.setPreviousStatus(entity.getStatus());
                entity.setStatus(nextStatus);
                entity.setIsDeleted(id);
                entity.setModifier(operator);
                entity.setModifyTime(now);
                sourceMapper.updateByPrimaryKeySelective(entity);
            }
        }

        LOGGER.info("success to logic delete all source by groupId={}, streamId={}", groupId, streamId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED)
    public boolean deleteAll(String groupId, String streamId, String operator) {
        LOGGER.info("begin to delete all source by groupId={}, streamId={}", groupId, streamId);
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        Preconditions.checkNotNull(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());

        // Check if it can be deleted
        groupCheckService.checkGroupStatus(groupId, operator);
        sourceMapper.deleteByRelatedId(groupId, streamId);
        LOGGER.info("success to delete all source by groupId={}, streamId={}", groupId, streamId);
        return true;
    }

    @Override
    public List<String> getSourceTypeList(String groupId, String streamId) {
        LOGGER.debug("begin to get source type list by groupId={}, streamId={}", groupId, streamId);
        if (StringUtils.isEmpty(streamId)) {
            return Collections.emptyList();
        }

        List<String> resultList = sourceMapper.selectSourceType(groupId, streamId);
        LOGGER.debug("success to get source type list, result sourceType={}", resultList);
        return resultList;
    }

    private void checkParams(SourceRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        String groupId = request.getInlongGroupId();
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        String streamId = request.getInlongStreamId();
        Preconditions.checkNotNull(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());
        String sourceType = request.getSourceType();
        Preconditions.checkNotNull(sourceType, ErrorCodeEnum.SOURCE_TYPE_IS_NULL.getMessage());
        String sourceName = request.getSourceName();
        Preconditions.checkNotNull(sourceName, ErrorCodeEnum.SOURCE_NAME_IS_NULL.getMessage());
    }

}
