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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GlobalConstants;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.stream.StreamField;
import org.apache.inlong.manager.common.pojo.transform.TransformRequest;
import org.apache.inlong.manager.common.pojo.transform.TransformResponse;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.StreamTransformEntity;
import org.apache.inlong.manager.dao.entity.StreamTransformFieldEntity;
import org.apache.inlong.manager.dao.mapper.StreamTransformEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamTransformFieldEntityMapper;
import org.apache.inlong.manager.service.group.GroupCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of transform service interface
 */
@Service
@Slf4j
public class StreamTransformServiceImpl implements StreamTransformService {

    @Autowired
    protected StreamTransformEntityMapper transformMapper;
    @Autowired
    protected StreamTransformFieldEntityMapper transformFieldMapper;
    @Autowired
    protected GroupCheckService groupCheckService;

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public Integer save(TransformRequest transformRequest, String operator) {
        log.info("begin to save transform info: {}", transformRequest);
        this.checkParams(transformRequest);

        // Check whether the transform can be added
        final String groupId = transformRequest.getInlongGroupId();
        final String streamId = transformRequest.getInlongStreamId();
        final String transformName = transformRequest.getTransformName();
        groupCheckService.checkGroupStatus(groupId, operator);

        List<StreamTransformEntity> transformEntities = transformMapper.selectByRelatedId(groupId,
                streamId, transformName);
        if (CollectionUtils.isNotEmpty(transformEntities)) {
            String err = "stream transform already exists with groupId=%s, streamId=%s, transformName=%s";
            throw new BusinessException(String.format(err, groupId, streamId, transformName));
        }
        StreamTransformEntity transformEntity = CommonBeanUtils.copyProperties(transformRequest,
                StreamTransformEntity::new);
        transformEntity.setVersion(0);
        transformEntity.setCreator(operator);
        transformEntity.setModifier(operator);
        Date now = new Date();
        transformEntity.setCreateTime(now);
        transformEntity.setModifyTime(now);
        transformEntity.setIsDeleted(GlobalConstants.UN_DELETED);
        transformMapper.insert(transformEntity);
        saveFieldOpt(transformEntity, transformRequest.getFieldList());
        return transformEntity.getId();
    }

    @Override
    public List<TransformResponse> listTransform(String groupId, String streamId) {
        log.info("begin to fetch transform info by groupId={} and streamId={} ", groupId, streamId);
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
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
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public boolean update(TransformRequest transformRequest, String operator) {
        log.info("begin to update transform info: {}", transformRequest);
        this.checkParams(transformRequest);
        // Check whether the transform can be modified
        String groupId = transformRequest.getInlongGroupId();
        groupCheckService.checkGroupStatus(groupId, operator);
        Preconditions.checkNotNull(transformRequest.getId(), ErrorCodeEnum.ID_IS_EMPTY.getMessage());
        StreamTransformEntity transformEntity = CommonBeanUtils.copyProperties(transformRequest,
                StreamTransformEntity::new);
        transformEntity.setModifier(operator);
        transformEntity.setVersion(transformEntity.getVersion() + 1);
        transformEntity.setModifyTime(new Date());

        transformMapper.updateByIdSelective(transformEntity);
        updateFieldOpt(transformEntity, transformRequest.getFieldList());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public boolean delete(String groupId, String streamId, String transformName, String operator) {
        log.info("begin to logic delete transform by groupId={} streamId={}, transformName={}", groupId, streamId,
                transformName);
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        Preconditions.checkNotNull(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());
        groupCheckService.checkGroupStatus(groupId, operator);
        Date now = new Date();
        List<StreamTransformEntity> entityList = transformMapper.selectByRelatedId(groupId, streamId, transformName);
        if (CollectionUtils.isNotEmpty(entityList)) {
            for (StreamTransformEntity entity : entityList) {
                Integer id = entity.getId();
                entity.setVersion(entity.getVersion() + 1);
                entity.setIsDeleted(id);
                entity.setModifier(operator);
                entity.setModifyTime(now);
                transformMapper.updateByIdSelective(entity);
                transformFieldMapper.deleteAll(id);
            }
        }
        log.info("success to logic delete transform by groupId={}, streamId={}, transformName={}", groupId, streamId,
                transformName);
        return true;
    }

    private void checkParams(TransformRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        String groupId = request.getInlongGroupId();
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        String streamId = request.getInlongStreamId();
        Preconditions.checkNotNull(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());
        String transformType = request.getTransformType();
        Preconditions.checkNotNull(transformType, ErrorCodeEnum.TRANSFORM_TYPE_IS_NULL.getMessage());
        String transformName = request.getTransformName();
        Preconditions.checkNotNull(transformName, ErrorCodeEnum.TRANSFORM_NAME_IS_NULL.getMessage());
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

        log.info("success to update transform field");
    }

    private void saveFieldOpt(StreamTransformEntity entity, List<StreamField> fieldList) {
        log.info("begin to save transform field={}", fieldList);
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
            fieldEntity.setIsDeleted(GlobalConstants.UN_DELETED);
            entityList.add(fieldEntity);
        }

        transformFieldMapper.insertAll(entityList);
        log.info("success to save transform fields");
    }
}
