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

package org.apache.inlong.manager.service.transform;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.transform.DeleteTransformRequest;
import org.apache.inlong.manager.pojo.transform.TransformRequest;
import org.apache.inlong.manager.pojo.transform.TransformResponse;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.StreamTransformEntity;
import org.apache.inlong.manager.dao.entity.StreamTransformFieldEntity;
import org.apache.inlong.manager.dao.mapper.StreamTransformEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamTransformFieldEntityMapper;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.service.group.GroupCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of transform service interface
 */
@Service
public class StreamTransformServiceImpl implements StreamTransformService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamTransformServiceImpl.class);

    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    protected StreamTransformEntityMapper transformMapper;
    @Autowired
    protected StreamTransformFieldEntityMapper transformFieldMapper;
    @Autowired
    protected GroupCheckService groupCheckService;

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public Integer save(TransformRequest request, String operator) {
        LOGGER.info("begin to save transform info: {}", request);
        this.checkParams(request);

        // Check whether the transform can be added
        final String groupId = request.getInlongGroupId();
        final String streamId = request.getInlongStreamId();
        final String transformName = request.getTransformName();
        groupCheckService.checkGroupStatus(groupId, operator);

        List<StreamTransformEntity> transformEntities = transformMapper.selectByRelatedId(groupId,
                streamId, transformName);
        if (CollectionUtils.isNotEmpty(transformEntities)) {
            String err = "stream transform already exists with groupId=%s, streamId=%s, transformName=%s";
            throw new BusinessException(String.format(err, groupId, streamId, transformName));
        }
        StreamTransformEntity transformEntity = CommonBeanUtils.copyProperties(request,
                StreamTransformEntity::new);
        transformEntity.setCreator(operator);
        transformEntity.setModifier(operator);
        transformMapper.insert(transformEntity);
        saveFieldOpt(transformEntity, request.getFieldList());
        return transformEntity.getId();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public Integer save(TransformRequest request, UserInfo opInfo) {
        // Check if it can be added
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(request.getInlongGroupId());
        if (groupEntity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND,
                    String.format("InlongGroup does not exist with InlongGroupId=%s", request.getInlongGroupId()));
        }
        // only the person in charges can query
        if (!opInfo.getAccountType().equals(UserTypeEnum.ADMIN.getCode())) {
            List<String> inCharges = Arrays.asList(groupEntity.getInCharges().split(InlongConstants.COMMA));
            if (!inCharges.contains(opInfo.getName())) {
                throw new BusinessException(ErrorCodeEnum.GROUP_PERMISSION_DENIED);
            }
        }
        // check inlong group status
        GroupStatus status = GroupStatus.forCode(groupEntity.getStatus());
        if (GroupStatus.notAllowedUpdate(status)) {
            throw new BusinessException(ErrorCodeEnum.OPT_NOT_ALLOWED_BY_STATUS,
                    String.format(ErrorCodeEnum.OPT_NOT_ALLOWED_BY_STATUS.getMessage(), status));
        }
        // Check if the record to be added exists
        List<StreamTransformEntity> transformEntities =
                transformMapper.selectByRelatedId(request.getInlongGroupId(),
                        request.getInlongStreamId(), request.getTransformName());
        if (CollectionUtils.isNotEmpty(transformEntities)) {
            throw new BusinessException(ErrorCodeEnum.RECORD_DUPLICATE,
                    String.format("stream transform already exists with groupId=%s, streamId=%s, transformName=%s",
                            request.getInlongGroupId(), request.getInlongStreamId(), request.getTransformName()));
        }
        // add record
        StreamTransformEntity transformEntity =
                CommonBeanUtils.copyProperties(request, StreamTransformEntity::new);
        transformEntity.setCreator(opInfo.getName());
        transformEntity.setModifier(opInfo.getName());
        transformMapper.insert(transformEntity);
        saveFieldOpt(transformEntity, request.getFieldList());
        return transformEntity.getId();
    }

    @Override
    public List<TransformResponse> listTransform(String groupId, String streamId) {
        LOGGER.debug("begin to fetch transform info by groupId={} and streamId={} ", groupId, streamId);
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        List<StreamTransformEntity> entityList = transformMapper.selectByRelatedId(groupId, streamId, null);
        if (CollectionUtils.isEmpty(entityList)) {
            return Collections.emptyList();
        }

        List<Integer> transformIds = entityList.stream().map(StreamTransformEntity::getId).collect(Collectors.toList());
        List<StreamTransformFieldEntity> fieldEntities = transformFieldMapper.selectByTransformIds(transformIds);
        Map<Integer, List<StreamField>> fieldInfoMap = fieldEntities.stream()
                .map(transformFieldEntity -> {
                    StreamField fieldInfo = CommonBeanUtils.copyProperties(transformFieldEntity, StreamField::new);
                    fieldInfo.setFieldType(transformFieldEntity.getFieldType());
                    fieldInfo.setId(transformFieldEntity.getRankNum());
                    return Pair.of(transformFieldEntity.getTransformId(), fieldInfo);
                }).collect(Collectors.groupingBy(Pair::getLeft,
                        Collectors.mapping(Pair::getRight, Collectors.toList())));
        List<TransformResponse> transformResponses = entityList.stream()
                .map(entity -> CommonBeanUtils.copyProperties(entity, TransformResponse::new))
                .collect(Collectors.toList());
        transformResponses.forEach(transformResponse -> {
            int transformId = transformResponse.getId();
            List<StreamField> fieldInfos = fieldInfoMap.get(transformId);
            if (CollectionUtils.isNotEmpty(fieldInfos)) {
                transformResponse.setFieldList(fieldInfos);
            }
        });
        return transformResponses;
    }

    @Override
    public List<TransformResponse> listTransform(String groupId, String streamId, UserInfo opInfo) {
        // Check if it can be added
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(groupId);
        if (groupEntity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND,
                    String.format("InlongGroup does not exist with InlongGroupId=%s", groupId));
        }
        // only the person in charges can query
        if (!opInfo.getAccountType().equals(UserTypeEnum.ADMIN.getCode())) {
            List<String> inCharges = Arrays.asList(groupEntity.getInCharges().split(InlongConstants.COMMA));
            if (!inCharges.contains(opInfo.getName())) {
                throw new BusinessException(ErrorCodeEnum.GROUP_PERMISSION_DENIED);
            }
        }
        // query result
        List<StreamTransformEntity> entityList = transformMapper.selectByRelatedId(groupId, streamId, null);
        if (CollectionUtils.isEmpty(entityList)) {
            return Collections.emptyList();
        }
        // get transform data
        List<Integer> transformIds = entityList.stream().map(StreamTransformEntity::getId).collect(Collectors.toList());
        List<StreamTransformFieldEntity> fieldEntities = transformFieldMapper.selectByTransformIds(transformIds);
        Map<Integer, List<StreamField>> fieldInfoMap = fieldEntities.stream()
                .map(transformFieldEntity -> {
                    StreamField fieldInfo = CommonBeanUtils.copyProperties(transformFieldEntity, StreamField::new);
                    fieldInfo.setFieldType(transformFieldEntity.getFieldType());
                    fieldInfo.setId(transformFieldEntity.getRankNum());
                    return Pair.of(transformFieldEntity.getTransformId(), fieldInfo);
                }).collect(Collectors.groupingBy(Pair::getLeft,
                        Collectors.mapping(Pair::getRight, Collectors.toList())));
        List<TransformResponse> transformResponses = entityList.stream()
                .map(entity -> CommonBeanUtils.copyProperties(entity, TransformResponse::new))
                .collect(Collectors.toList());
        transformResponses.forEach(transformResponse -> {
            int transformId = transformResponse.getId();
            List<StreamField> fieldInfos = fieldInfoMap.get(transformId);
            if (CollectionUtils.isNotEmpty(fieldInfos)) {
                transformResponse.setFieldList(fieldInfos);
            }
        });
        return transformResponses;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public Boolean update(TransformRequest request, String operator) {
        LOGGER.info("begin to update transform info: {}", request);
        // check request and parameters
        this.chkUnmodifiableParams(request);
        // Check whether the transform can be modified
        String groupId = request.getInlongGroupId();
        groupCheckService.checkGroupStatus(groupId, operator);
        Preconditions.expectNotNull(request.getId(), ErrorCodeEnum.ID_IS_EMPTY.getMessage());
        StreamTransformEntity transformEntity = CommonBeanUtils.copyProperties(request,
                StreamTransformEntity::new);
        transformEntity.setModifier(operator);
        int rowCount = transformMapper.updateByIdSelective(transformEntity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            String msg =
                    String.format("transform has already updated with groupId=%s, streamId=%s, name=%s, curVersion=%s",
                            request.getInlongGroupId(), request.getInlongStreamId(),
                            request.getTransformName(), request.getVersion());
            LOGGER.error(msg);
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        updateFieldOpt(transformEntity, request.getFieldList());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public Boolean update(TransformRequest request, UserInfo opInfo) {
        // check request and parameters
        this.chkUnmodifiableParams(request);
        // Check if it can be added
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(request.getInlongGroupId());
        if (groupEntity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND,
                    String.format("InlongGroup does not exist with InlongGroupId=%s", request.getInlongGroupId()));
        }
        // only the person in charges can query
        if (!opInfo.getAccountType().equals(UserTypeEnum.ADMIN.getCode())) {
            List<String> inCharges = Arrays.asList(groupEntity.getInCharges().split(InlongConstants.COMMA));
            if (!inCharges.contains(opInfo.getName())) {
                throw new BusinessException(ErrorCodeEnum.GROUP_PERMISSION_DENIED);
            }
        }
        // check inlong group status
        GroupStatus status = GroupStatus.forCode(groupEntity.getStatus());
        if (GroupStatus.notAllowedUpdate(status)) {
            throw new BusinessException(ErrorCodeEnum.OPT_NOT_ALLOWED_BY_STATUS,
                    String.format(ErrorCodeEnum.OPT_NOT_ALLOWED_BY_STATUS.getMessage(), status));
        }
        // update record
        StreamTransformEntity transformEntity =
                CommonBeanUtils.copyProperties(request, StreamTransformEntity::new);
        transformEntity.setModifier(opInfo.getName());
        int rowCount = transformMapper.updateByIdSelective(transformEntity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED,
                    String.format("transform has already updated with groupId=%s, streamId=%s, name=%s, curVersion=%s",
                            request.getInlongGroupId(), request.getInlongStreamId(),
                            request.getTransformName(), request.getVersion()));
        }
        updateFieldOpt(transformEntity, request.getFieldList());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public Boolean delete(DeleteTransformRequest request, String operator) {
        LOGGER.info("begin to logic delete transform for request={}", request);
        Preconditions.expectNotNull(request, "delete request of transform cannot be null");

        String groupId = request.getInlongGroupId();
        String streamId = request.getInlongStreamId();
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        Preconditions.expectNotBlank(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY);
        groupCheckService.checkGroupStatus(groupId, operator);

        List<StreamTransformEntity> entityList = transformMapper.selectByRelatedId(groupId, streamId,
                request.getTransformName());
        if (CollectionUtils.isNotEmpty(entityList)) {
            for (StreamTransformEntity entity : entityList) {
                Integer id = entity.getId();
                entity.setIsDeleted(id);
                entity.setModifier(operator);
                int rowCount = transformMapper.updateByIdSelective(entity);
                if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
                    LOGGER.error("transform has already updated with groupId={}, streamId={}, name={}, curVersion={}",
                            entity.getInlongGroupId(), entity.getInlongStreamId(),
                            entity.getTransformName(), entity.getVersion());
                    throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
                }
                transformFieldMapper.deleteAll(id);
            }
        }
        LOGGER.info("success to logic delete transform for request={} by user={}", request, operator);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public Boolean delete(DeleteTransformRequest request, UserInfo opInfo) {
        // Check if it can be added
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(request.getInlongGroupId());
        if (groupEntity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND,
                    String.format("InlongGroup does not exist with InlongGroupId=%s", request.getInlongGroupId()));
        }
        // only the person in charges can query
        if (!opInfo.getAccountType().equals(UserTypeEnum.ADMIN.getCode())) {
            List<String> inCharges = Arrays.asList(groupEntity.getInCharges().split(InlongConstants.COMMA));
            if (!inCharges.contains(opInfo.getName())) {
                throw new BusinessException(ErrorCodeEnum.GROUP_PERMISSION_DENIED);
            }
        }
        // check inlong group status
        GroupStatus status = GroupStatus.forCode(groupEntity.getStatus());
        if (GroupStatus.notAllowedUpdate(status)) {
            throw new BusinessException(ErrorCodeEnum.OPT_NOT_ALLOWED_BY_STATUS,
                    String.format(ErrorCodeEnum.OPT_NOT_ALLOWED_BY_STATUS.getMessage(), status));
        }
        // query records
        List<StreamTransformEntity> entityList =
                transformMapper.selectByRelatedId(request.getInlongGroupId(),
                        request.getInlongStreamId(), request.getTransformName());
        if (CollectionUtils.isNotEmpty(entityList)) {
            for (StreamTransformEntity entity : entityList) {
                Integer id = entity.getId();
                entity.setIsDeleted(id);
                entity.setModifier(opInfo.getName());
                int rowCount = transformMapper.updateByIdSelective(entity);
                if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
                    throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED,
                            String.format(
                                    "transform has already updated with groupId=%s, streamId=%s, name=%s, curVersion=%s",
                                    entity.getInlongGroupId(), entity.getInlongStreamId(),
                                    entity.getTransformName(), entity.getVersion()));
                }
                transformFieldMapper.deleteAll(id);
            }
        }
        return true;
    }

    private void checkParams(TransformRequest request) {
        Preconditions.expectNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        String groupId = request.getInlongGroupId();
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        String streamId = request.getInlongStreamId();
        Preconditions.expectNotBlank(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY);
        String transformType = request.getTransformType();
        Preconditions.expectNotBlank(transformType, ErrorCodeEnum.TRANSFORM_TYPE_IS_NULL);
        String transformName = request.getTransformName();
        Preconditions.expectNotBlank(transformName, ErrorCodeEnum.TRANSFORM_NAME_IS_NULL);
    }

    private void chkUnmodifiableParams(TransformRequest request) {
        StreamTransformEntity entity = transformMapper.selectById(request.getId());
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.TRANSFORM_NOT_FOUND);
        }
        // check record version
        Preconditions.expectEquals(entity.getVersion(), request.getVersion(),
                ErrorCodeEnum.CONFIG_EXPIRED,
                String.format("record has expired with record version=%d, request version=%d",
                        entity.getVersion(), request.getVersion()));
        // check group id
        if (StringUtils.isNotBlank(request.getInlongGroupId())
                && !entity.getInlongGroupId().equals(request.getInlongGroupId())) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "inlongGroupId not allowed modify");
        }
        // check stream id
        if (StringUtils.isNotBlank(request.getInlongStreamId())
                && !entity.getInlongStreamId().equals(request.getInlongStreamId())) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "inlongStreamId not allowed modify");
        }
        // check transform type
        if (StringUtils.isNotBlank(request.getTransformType())
                && !entity.getTransformType().equals(request.getTransformType())) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "transformType not allowed modify");
        }
        // check transform name
        if (StringUtils.isNotBlank(request.getTransformName())
                && !entity.getTransformName().equals(request.getTransformName())) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "transformName not allowed modify");
        }
        request.setInlongGroupId(entity.getInlongGroupId());
        request.setInlongStreamId(entity.getInlongStreamId());
        request.setTransformType(entity.getTransformType());
        request.setTransformName(entity.getTransformName());
    }

    private void updateFieldOpt(StreamTransformEntity entity, List<StreamField> fieldList) {
        Integer transformId = entity.getId();
        if (CollectionUtils.isEmpty(fieldList)) {
            return;
        }

        // First physically delete the existing fields
        transformFieldMapper.deleteAll(transformId);
        // Then batch save the source fields
        this.saveFieldOpt(entity, fieldList);

        LOGGER.debug("success to update transform field");
    }

    private void saveFieldOpt(StreamTransformEntity entity, List<StreamField> fieldList) {
        LOGGER.debug("begin to save transform field={}", fieldList);
        if (CollectionUtils.isEmpty(fieldList)) {
            return;
        }

        int size = fieldList.size();
        List<StreamTransformFieldEntity> entityList = new ArrayList<>(size);
        String groupId = entity.getInlongGroupId();
        String streamId = entity.getInlongStreamId();
        String transformType = entity.getTransformType();
        Integer transformId = entity.getId();
        for (StreamField fieldInfo : fieldList) {
            StreamTransformFieldEntity fieldEntity = CommonBeanUtils.copyProperties(fieldInfo,
                    StreamTransformFieldEntity::new);
            if (StringUtils.isEmpty(fieldEntity.getFieldComment())) {
                fieldEntity.setFieldComment(fieldEntity.getFieldName());
            }
            fieldEntity.setId(null);
            fieldEntity.setInlongGroupId(groupId);
            fieldEntity.setInlongStreamId(streamId);
            fieldEntity.setFieldType(fieldInfo.getFieldType());
            fieldEntity.setRankNum(fieldInfo.getId());
            fieldEntity.setTransformId(transformId);
            fieldEntity.setTransformType(transformType);
            fieldEntity.setIsDeleted(InlongConstants.UN_DELETED);
            entityList.add(fieldEntity);
        }

        transformFieldMapper.insertAll(entityList);
        LOGGER.debug("success to save transform fields");
    }
}
