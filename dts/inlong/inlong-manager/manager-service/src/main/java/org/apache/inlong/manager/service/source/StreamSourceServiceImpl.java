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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.InlongStreamEntity;
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSourceEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSourceFieldEntityMapper;
import org.apache.inlong.manager.pojo.common.OrderFieldEnum;
import org.apache.inlong.manager.pojo.common.OrderTypeEnum;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.source.SourcePageRequest;
import org.apache.inlong.manager.pojo.source.SourceRequest;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.service.group.GroupCheckService;
import org.apache.inlong.manager.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of source service interface
 */
@Service
public class StreamSourceServiceImpl implements StreamSourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamSourceServiceImpl.class);

    @Autowired
    private SourceOperatorFactory operatorFactory;
    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private InlongStreamEntityMapper streamMapper;
    @Autowired
    private StreamSourceEntityMapper sourceMapper;
    @Autowired
    private StreamSourceFieldEntityMapper sourceFieldMapper;
    @Autowired
    private GroupCheckService groupCheckService;
    @Autowired
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public Integer save(SourceRequest request, String operator) {
        LOGGER.info("begin to save source info: {}", request);
        this.checkParams(request);

        // Check if it can be added
        String groupId = request.getInlongGroupId();
        InlongGroupEntity groupEntity = groupCheckService.checkGroupStatus(groupId, operator);
        // only the person in charges can query
        userService.checkUser(groupEntity.getInCharges(), operator, ErrorCodeEnum.GROUP_PERMISSION_DENIED.getMessage());
        String streamId = request.getInlongStreamId();
        String sourceName = request.getSourceName();
        List<StreamSourceEntity> existList = sourceMapper.selectByRelatedId(groupId, streamId, sourceName);
        if (CollectionUtils.isNotEmpty(existList)) {
            String err = "source name=%s already exists with groupId=%s streamId=%s";
            throw new BusinessException(String.format(err, sourceName, groupId, streamId));
        }

        // According to the source type, save source information
        StreamSourceOperator sourceOperator = operatorFactory.getInstance(request.getSourceType());
        // Remove id in sourceField when save
        List<StreamField> streamFields = request.getFieldList();
        if (CollectionUtils.isNotEmpty(streamFields)) {
            streamFields.forEach(streamField -> streamField.setId(null));
        }
        int id = sourceOperator.saveOpt(request, groupEntity.getStatus(), operator);

        LOGGER.info("success to save source info: {}", request);
        return id;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public Integer save(SourceRequest request, UserInfo opInfo) {
        // Check if it can be added
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(request.getInlongGroupId());
        if (groupEntity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND,
                    String.format("InlongGroup does not exist with InlongGroupId=%s", request.getInlongGroupId()));
        }
        // only the person in charges can query
        userService.checkUser(groupEntity.getInCharges(), opInfo.getName(),
                ErrorCodeEnum.GROUP_PERMISSION_DENIED.getMessage());
        // get stream information
        InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(
                request.getInlongGroupId(), request.getInlongStreamId());
        if (streamEntity == null) {
            throw new BusinessException(ErrorCodeEnum.STREAM_NOT_FOUND,
                    String.format("InlongStream does not exist with InlongGroupId=%s, InLongStreamId=%s",
                            request.getInlongGroupId(), request.getInlongStreamId()));
        }
        // Check if the record to be added exists
        List<StreamSourceEntity> existList = sourceMapper.selectByRelatedId(
                request.getInlongGroupId(), request.getInlongStreamId(), request.getSourceName());
        if (CollectionUtils.isNotEmpty(existList)) {
            throw new BusinessException(ErrorCodeEnum.RECORD_DUPLICATE,
                    String.format("source name=%s already exists with groupId=%s streamId=%s",
                            request.getSourceName(), request.getInlongGroupId(), request.getInlongStreamId()));
        }
        // check inlong group status
        GroupStatus status = GroupStatus.forCode(groupEntity.getStatus());
        if (GroupStatus.notAllowedUpdate(status)) {
            throw new BusinessException(ErrorCodeEnum.OPT_NOT_ALLOWED_BY_STATUS,
                    String.format(ErrorCodeEnum.OPT_NOT_ALLOWED_BY_STATUS.getMessage(), status));
        }
        // According to the source type, save source information
        StreamSourceOperator sourceOperator = operatorFactory.getInstance(request.getSourceType());
        // Remove id in sourceField when save
        List<StreamField> streamFields = request.getFieldList();
        if (CollectionUtils.isNotEmpty(streamFields)) {
            streamFields.forEach(streamField -> streamField.setId(null));
        }
        return sourceOperator.saveOpt(request, groupEntity.getStatus(), opInfo.getName());
    }

    @Override
    public StreamSource get(Integer id) {
        if (id == null) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "source id is empty");
        }
        StreamSourceEntity entity = sourceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_NOT_FOUND,
                    String.format("source not found by id=%s", id));
        }
        StreamSourceOperator sourceOperator = operatorFactory.getInstance(entity.getSourceType());
        StreamSource streamSource = sourceOperator.getFromEntity(entity);
        LOGGER.debug("success to get source by id={}", id);
        return streamSource;
    }

    @Override
    public StreamSource get(Integer id, UserInfo opInfo) {
        StreamSourceEntity entity = sourceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_NOT_FOUND,
                    String.format("source not found by id=%s", id));
        }
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(entity.getInlongGroupId());
        if (groupEntity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }
        // only the person in charges can query
        userService.checkUser(groupEntity.getInCharges(), opInfo.getName(),
                ErrorCodeEnum.GROUP_PERMISSION_DENIED.getMessage());
        StreamSourceOperator sourceOperator = operatorFactory.getInstance(entity.getSourceType());
        return sourceOperator.getFromEntity(entity);
    }

    @Override
    public Integer getCount(String groupId, String streamId) {
        Integer count = sourceMapper.selectCount(groupId, streamId);
        LOGGER.debug("source count={} with groupId={}, streamId={}", count, groupId, streamId);
        return count;
    }

    @Override
    public List<StreamSource> listSource(String groupId, String streamId) {
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);
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
    public Map<String, List<StreamSource>> getSourcesMap(InlongGroupInfo groupInfo,
            List<InlongStreamInfo> streamInfos) {
        String groupId = groupInfo.getInlongGroupId();
        LOGGER.debug("begin to get source map for groupId={}", groupId);
        Map<String, List<StreamSource>> result;

        // if the group mode is LIGHTWEIGHT, just get all related stream sources
        List<StreamSource> streamSources = this.listSource(groupId, null);
        if (InlongConstants.LIGHTWEIGHT_MODE.equals(groupInfo.getLightweight())) {
            result = streamSources.stream()
                    .collect(Collectors.groupingBy(StreamSource::getInlongStreamId, HashMap::new,
                            Collectors.toCollection(ArrayList::new)));
        } else {
            // if the group mode is STANDARD, needs to get the cached MQ sources
            StreamSourceOperator sourceOperator = operatorFactory.getInstance(groupInfo.getMqType());
            result = sourceOperator.getSourcesMap(groupInfo, streamInfos, streamSources);
        }

        LOGGER.debug("success to get source map, size={}, groupInfo={}", result.size(), groupInfo);
        return result;
    }

    @Override
    public PageResult<? extends StreamSource> listByCondition(SourcePageRequest request) {
        Preconditions.expectNotBlank(request.getInlongGroupId(), ErrorCodeEnum.GROUP_ID_IS_EMPTY);

        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        OrderFieldEnum.checkOrderField(request);
        OrderTypeEnum.checkOrderType(request);
        List<StreamSourceEntity> entityList = sourceMapper.selectByCondition(request);

        // Encapsulate the paging query results into the PageInfo object to obtain related paging information
        Map<String, Page<StreamSourceEntity>> sourceMap = Maps.newHashMap();
        for (StreamSourceEntity entity : entityList) {
            sourceMap.computeIfAbsent(entity.getSourceType(), k -> new Page<>()).add(entity);
        }
        List<StreamSource> responseList = Lists.newArrayList();
        for (Map.Entry<String, Page<StreamSourceEntity>> entry : sourceMap.entrySet()) {
            StreamSourceOperator sourceOperator = operatorFactory.getInstance(entry.getKey());
            PageResult<? extends StreamSource> pageInfo = sourceOperator.getPageInfo(entry.getValue());
            if (null != pageInfo && CollectionUtils.isNotEmpty(pageInfo.getList())) {
                responseList.addAll(pageInfo.getList());
            }
        }

        PageResult<? extends StreamSource> pageResult = new PageResult<>(responseList);

        LOGGER.debug("success to list source page, result size {}", pageResult.getList().size());
        return pageResult;
    }

    @Override
    public PageResult<? extends StreamSource> listByCondition(SourcePageRequest request, UserInfo opInfo) {
        if (StringUtils.isBlank(request.getInlongGroupId())) {
            throw new BusinessException(ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        }
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        OrderFieldEnum.checkOrderField(request);
        OrderTypeEnum.checkOrderType(request);
        List<StreamSourceEntity> entityList = sourceMapper.selectByCondition(request);
        List<StreamSourceEntity> filteredEntitys = Lists.newArrayList();
        if (opInfo.getAccountType().equals(UserTypeEnum.ADMIN.getCode())) {
            filteredEntitys.addAll(entityList);
        } else {
            Set<String> totalGroupIds = new HashSet<>();
            Set<String> allowedGroupIds = new HashSet<>();
            for (StreamSourceEntity sourceEntity : entityList) {
                totalGroupIds.add(sourceEntity.getInlongGroupId());
            }
            for (String groupId : totalGroupIds) {
                InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
                if (entity == null) {
                    continue;
                }
                // only the person in charges can query
                List<String> inCharges = Arrays.asList(entity.getInCharges().split(InlongConstants.COMMA));
                if (!inCharges.contains(opInfo.getName())) {
                    continue;
                }
                allowedGroupIds.add(groupId);
            }
            for (StreamSourceEntity sourceEntity : entityList) {
                if (allowedGroupIds.contains(sourceEntity.getInlongGroupId())) {
                    filteredEntitys.add(sourceEntity);
                }
            }
        }
        // Encapsulate the paging query results into the PageInfo object to obtain related paging information
        Map<String, Page<StreamSourceEntity>> sourceMap = Maps.newHashMap();
        for (StreamSourceEntity entity : filteredEntitys) {
            sourceMap.computeIfAbsent(entity.getSourceType(), k -> new Page<>()).add(entity);
        }
        List<StreamSource> responseList = Lists.newArrayList();
        for (Map.Entry<String, Page<StreamSourceEntity>> entry : sourceMap.entrySet()) {
            StreamSourceOperator sourceOperator = operatorFactory.getInstance(entry.getKey());
            PageResult<? extends StreamSource> pageInfo = sourceOperator.getPageInfo(entry.getValue());
            if (null != pageInfo && CollectionUtils.isNotEmpty(pageInfo.getList())) {
                responseList.addAll(pageInfo.getList());
            }
        }
        return new PageResult<>(responseList);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Boolean update(SourceRequest request, String operator) {
        LOGGER.info("begin to update source info: {}", request);
        // check request parameter
        chkUnmodifiableParams(request);

        // Check if it can be modified
        String groupId = request.getInlongGroupId();
        InlongGroupEntity groupEntity = groupCheckService.checkGroupStatus(groupId, operator);
        if (groupEntity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND,
                    String.format("InlongGroup does not exist with InlongGroupId=%s", groupEntity.getInlongGroupId()));
        }
        // only the person in charges can query
        userService.checkUser(groupEntity.getInCharges(), operator, ErrorCodeEnum.GROUP_PERMISSION_DENIED.getMessage());
        StreamSourceOperator sourceOperator = operatorFactory.getInstance(request.getSourceType());
        // Remove id in sourceField when save
        List<StreamField> streamFields = request.getFieldList();
        if (CollectionUtils.isNotEmpty(streamFields)) {
            streamFields.forEach(streamField -> streamField.setId(null));
        }
        sourceOperator.updateOpt(request, groupEntity.getStatus(), groupEntity.getLightweight(), operator);

        LOGGER.info("success to update source info: {}", request);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Boolean update(SourceRequest request, UserInfo opInfo) {
        // check request parameter
        chkUnmodifiableParams(request);
        // Check if it can be update
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(request.getInlongGroupId());
        if (groupEntity == null) {
            throw new BusinessException(ErrorCodeEnum.ILLEGAL_RECORD_FIELD_VALUE,
                    String.format("InlongGroup does not exist with InlongGroupId=%s", request.getInlongGroupId()));
        }
        // only the person in charges can query
        userService.checkUser(groupEntity.getInCharges(), opInfo.getName(),
                ErrorCodeEnum.GROUP_PERMISSION_DENIED.getMessage());
        // check inlong group status
        GroupStatus status = GroupStatus.forCode(groupEntity.getStatus());
        if (GroupStatus.notAllowedUpdate(status)) {
            throw new BusinessException(ErrorCodeEnum.OPT_NOT_ALLOWED_BY_STATUS,
                    String.format(ErrorCodeEnum.OPT_NOT_ALLOWED_BY_STATUS.getMessage(), status));
        }
        StreamSourceOperator sourceOperator = operatorFactory.getInstance(request.getSourceType());
        // Remove id in sourceField when save
        List<StreamField> streamFields = request.getFieldList();
        if (CollectionUtils.isNotEmpty(streamFields)) {
            streamFields.forEach(streamField -> streamField.setId(null));
        }
        sourceOperator.updateOpt(request, groupEntity.getStatus(), groupEntity.getLightweight(), opInfo.getName());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Boolean updateStatus(String groupId, String streamId, Integer targetStatus, String operator) {
        sourceMapper.updateStatusByRelatedId(groupId, streamId, targetStatus);
        LOGGER.info("success to update source status={} for groupId={}, streamId={} by {}",
                targetStatus, groupId, streamId, operator);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Boolean delete(Integer id, String operator) {
        LOGGER.info("begin to delete source for id={} by user={}", id, operator);
        Preconditions.expectNotNull(id, ErrorCodeEnum.ID_IS_EMPTY.getMessage());

        StreamSourceEntity entity = sourceMapper.selectByIdForUpdate(id);
        Preconditions.expectNotNull(entity, ErrorCodeEnum.SOURCE_INFO_NOT_FOUND,
                ErrorCodeEnum.SOURCE_INFO_NOT_FOUND.getMessage());
        boolean isTemplateSource = CollectionUtils.isNotEmpty(sourceMapper.selectByTemplateId(id));

        // Check if it can be delete
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(entity.getInlongGroupId());
        if (groupEntity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND,
                    String.format("InlongGroup does not exist with InlongGroupId=%s", entity.getInlongGroupId()));
        }
        // only the person in charges can query
        userService.checkUser(groupEntity.getInCharges(), operator, ErrorCodeEnum.GROUP_PERMISSION_DENIED.getMessage());

        SourceStatus curStatus = SourceStatus.forCode(entity.getStatus());
        SourceStatus nextStatus = SourceStatus.TO_BE_ISSUED_DELETE;
        // if source is frozen|failed|new, or if it is a template source or auto push source, delete directly
        if (curStatus == SourceStatus.SOURCE_STOP || curStatus == SourceStatus.SOURCE_FAILED
                || curStatus == SourceStatus.SOURCE_NEW || isTemplateSource
                || SourceType.AUTO_PUSH.equals(entity.getSourceType())) {
            nextStatus = SourceStatus.SOURCE_DISABLE;
        }
        if (!SourceStatus.isAllowedTransition(curStatus, nextStatus)) {
            throw new BusinessException(
                    String.format("current source status=%s for id=%s is not allowed to delete", entity.getStatus(),
                            entity.getId()));
        }

        entity.setPreviousStatus(curStatus.getCode());
        entity.setStatus(nextStatus.getCode());
        entity.setIsDeleted(id);
        int rowCount = sourceMapper.updateByPrimaryKeySelective(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            LOGGER.error("source has already updated with groupId={}, streamId={}, name={}, curVersion={}",
                    entity.getInlongGroupId(), entity.getInlongStreamId(), entity.getSourceName(), entity.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        sourceFieldMapper.deleteAll(id);

        LOGGER.info("success to delete source for id={} by user={}", id, operator);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Boolean delete(Integer id, UserInfo opInfo) {
        StreamSourceEntity entity = sourceMapper.selectByIdForUpdate(id);
        Preconditions.expectNotNull(entity, ErrorCodeEnum.SOURCE_INFO_NOT_FOUND,
                ErrorCodeEnum.SOURCE_INFO_NOT_FOUND.getMessage());

        // Check if it can be delete
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(entity.getInlongGroupId());
        if (groupEntity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND,
                    String.format("InlongGroup does not exist with InlongGroupId=%s", entity.getInlongGroupId()));
        }
        // only the person in charges can query
        userService.checkUser(groupEntity.getInCharges(), opInfo.getName(),
                ErrorCodeEnum.GROUP_PERMISSION_DENIED.getMessage());
        // check record status
        boolean isTemplateSource = CollectionUtils.isNotEmpty(sourceMapper.selectByTemplateId(id));
        SourceStatus curStatus = SourceStatus.forCode(entity.getStatus());
        SourceStatus nextStatus = SourceStatus.TO_BE_ISSUED_DELETE;
        // if source is frozen|failed|new, or if it is a template source or auto push source, delete directly
        if (curStatus == SourceStatus.SOURCE_STOP || curStatus == SourceStatus.SOURCE_FAILED
                || curStatus == SourceStatus.SOURCE_NEW || isTemplateSource
                || SourceType.AUTO_PUSH.equals(entity.getSourceType())) {
            nextStatus = SourceStatus.SOURCE_DISABLE;
        }
        if (!SourceStatus.isAllowedTransition(curStatus, nextStatus)) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_OPT_NOT_ALLOWED,
                    String.format("current source status=%s for id=%s is not allowed to delete", entity.getStatus(),
                            entity.getId()));
        }
        // delete record
        entity.setPreviousStatus(curStatus.getCode());
        entity.setStatus(nextStatus.getCode());
        entity.setIsDeleted(id);
        entity.setModifier(opInfo.getName());
        int rowCount = sourceMapper.updateByPrimaryKeySelective(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED,
                    String.format("source has already updated with groupId=%s, streamId=%s, name=%s, curVersion=%s",
                            entity.getInlongGroupId(), entity.getInlongStreamId(), entity.getSourceName(),
                            entity.getVersion()));
        }
        sourceFieldMapper.deleteAll(id);
        return true;
    }

    @Override
    public Boolean forceDelete(String groupId, String streamId, String operator) {
        LOGGER.info("begin to force delete source for groupId={} and streamId={} by user={}",
                groupId, streamId, operator);
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        Preconditions.expectNotBlank(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY);

        int sourceCount = sourceMapper.updateByRelatedId(groupId, streamId, SourceStatus.TO_BE_ISSUED_DELETE.getCode());
        int fieldCount = sourceFieldMapper.updateByRelatedId(groupId, streamId);
        LOGGER.info("success to force delete source for groupId={} and streamId={} by user={},"
                + " update {} sources and {} fields", groupId, streamId, operator, sourceCount, fieldCount);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Boolean restart(Integer id, String operator) {
        LOGGER.info("begin to restart source by id={}", id);
        StreamSourceEntity entity = sourceMapper.selectByIdForUpdate(id);
        Preconditions.expectNotNull(entity, ErrorCodeEnum.SOURCE_INFO_NOT_FOUND.getMessage());
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(entity.getInlongGroupId());
        if (groupEntity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND,
                    String.format("InlongGroup does not exist with InlongGroupId=%s", entity.getInlongGroupId()));
        }
        // only the person in charges can query
        userService.checkUser(groupEntity.getInCharges(), operator, ErrorCodeEnum.GROUP_PERMISSION_DENIED.getMessage());

        StreamSourceOperator sourceOperator = operatorFactory.getInstance(entity.getSourceType());
        SourceRequest sourceRequest = new SourceRequest();
        CommonBeanUtils.copyProperties(entity, sourceRequest, true);
        sourceOperator.restartOpt(sourceRequest, operator);

        LOGGER.info("success to restart source info: {}", entity);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Boolean stop(Integer id, String operator) {
        LOGGER.info("begin to stop source by id={}", id);
        StreamSourceEntity entity = sourceMapper.selectByIdForUpdate(id);
        Preconditions.expectNotNull(entity, ErrorCodeEnum.SOURCE_INFO_NOT_FOUND.getMessage());
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(entity.getInlongGroupId());
        if (groupEntity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND,
                    String.format("InlongGroup does not exist with InlongGroupId=%s", entity.getInlongGroupId()));
        }
        // only the person in charges can query
        userService.checkUser(groupEntity.getInCharges(), operator, ErrorCodeEnum.GROUP_PERMISSION_DENIED.getMessage());

        StreamSourceOperator sourceOperator = operatorFactory.getInstance(entity.getSourceType());
        SourceRequest sourceRequest = new SourceRequest();
        CommonBeanUtils.copyProperties(entity, sourceRequest, true);
        sourceOperator.stopOpt(sourceRequest, operator);

        LOGGER.info("success to stop source info: {}", entity);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Boolean logicDeleteAll(String groupId, String streamId, String operator) {
        LOGGER.info("begin to logic delete all source info by groupId={}, streamId={}", groupId, streamId);
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);

        // Check if it can be deleted
        Integer nextStatus = SourceStatus.TO_BE_ISSUED_DELETE.getCode();
        List<StreamSourceEntity> entityList = sourceMapper.selectByRelatedId(groupId, streamId, null);
        if (CollectionUtils.isNotEmpty(entityList)) {
            for (StreamSourceEntity entity : entityList) {
                Integer id = entity.getId();
                entity.setPreviousStatus(entity.getStatus());
                entity.setStatus(nextStatus);
                entity.setIsDeleted(id);
                entity.setModifier(operator);
                int rowCount = sourceMapper.updateByPrimaryKeySelective(entity);
                if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
                    LOGGER.error("source has already updated with groupId={}, streamId={}, name={}, curVersion={}",
                            entity.getInlongGroupId(), entity.getInlongStreamId(), entity.getSourceName(),
                            entity.getVersion());
                    throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
                }
            }
        }

        LOGGER.info("success to logic delete all source by groupId={}, streamId={}", groupId, streamId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Boolean deleteAll(String groupId, String streamId, String operator) {
        LOGGER.info("begin to delete all source by groupId={}, streamId={}", groupId, streamId);
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        Preconditions.expectNotBlank(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY);

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
        Preconditions.expectNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        String groupId = request.getInlongGroupId();
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        String streamId = request.getInlongStreamId();
        Preconditions.expectNotBlank(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY);
        String sourceType = request.getSourceType();
        Preconditions.expectNotBlank(sourceType, ErrorCodeEnum.SOURCE_TYPE_IS_NULL);
        String sourceName = request.getSourceName();
        Preconditions.expectNotBlank(sourceName, ErrorCodeEnum.SOURCE_NAME_IS_NULL);
    }

    private void chkUnmodifiableParams(SourceRequest request) {
        // check record exists
        StreamSourceEntity entity = sourceMapper.selectById(request.getId());
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_NOT_FOUND,
                    String.format("not found source record by id=%d", request.getId()));
        }
        // check whether modify sourceType
        Preconditions.expectEquals(entity.getSourceType(), request.getSourceType(),
                ErrorCodeEnum.INVALID_PARAMETER, "sourceType not allowed modify");
        // check record version
        Preconditions.expectEquals(entity.getVersion(), request.getVersion(),
                ErrorCodeEnum.CONFIG_EXPIRED,
                String.format("record has expired with record version=%d, request version=%d",
                        entity.getVersion(), request.getVersion()));
        // check whether modify groupId
        if (StringUtils.isNotBlank(request.getInlongGroupId())
                && !entity.getInlongGroupId().equals(request.getInlongGroupId())) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "InlongGroupId not allowed modify");
        }
        // check whether modify streamId
        if (StringUtils.isNotBlank(request.getInlongStreamId())
                && !entity.getInlongStreamId().equals(request.getInlongStreamId())) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "InlongStreamId not allowed modify");
        }
        // check whether modify sourceName
        if (StringUtils.isNotBlank(request.getSourceName())
                && !entity.getSourceName().equals(request.getSourceName())) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "sourceName not allowed modify");
        }
        // set primary field value
        request.setInlongGroupId(entity.getInlongGroupId());
        request.setInlongStreamId(entity.getInlongStreamId());
        request.setSourceName(entity.getSourceName());
    }
}
