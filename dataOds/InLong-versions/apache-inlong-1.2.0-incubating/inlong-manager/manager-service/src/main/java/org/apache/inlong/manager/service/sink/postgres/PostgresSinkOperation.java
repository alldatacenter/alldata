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

package org.apache.inlong.manager.service.sink.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import javax.validation.constraints.NotNull;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GlobalConstants;
import org.apache.inlong.manager.common.enums.SinkStatus;
import org.apache.inlong.manager.common.enums.SinkType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.sink.SinkField;
import org.apache.inlong.manager.common.pojo.sink.SinkListResponse;
import org.apache.inlong.manager.common.pojo.sink.SinkRequest;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.common.pojo.sink.postgres.PostgresSinkDTO;
import org.apache.inlong.manager.common.pojo.sink.postgres.PostgresSinkListResponse;
import org.apache.inlong.manager.common.pojo.sink.postgres.PostgresSinkRequest;
import org.apache.inlong.manager.common.pojo.sink.postgres.PostgresSink;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.dao.entity.StreamSinkFieldEntity;
import org.apache.inlong.manager.dao.mapper.StreamSinkEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSinkFieldEntityMapper;
import org.apache.inlong.manager.service.sink.StreamSinkOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Postgres sink operation
 */
@Service
public class PostgresSinkOperation implements StreamSinkOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresSinkOperation.class);

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StreamSinkEntityMapper sinkMapper;
    @Autowired
    private StreamSinkFieldEntityMapper sinkFieldMapper;

    @Override
    public Boolean accept(SinkType sinkType) {
        return SinkType.POSTGRES.equals(sinkType);
    }

    @Override
    public Integer saveOpt(SinkRequest request, String operator) {
        String sinkType = request.getSinkType();
        Preconditions.checkTrue(SinkType.SINK_POSTGRES.equals(sinkType),
                ErrorCodeEnum.SINK_TYPE_NOT_SUPPORT.getMessage() + ": " + sinkType);

        PostgresSinkRequest postgresSinkRequest = (PostgresSinkRequest) request;
        StreamSinkEntity entity = CommonBeanUtils.copyProperties(postgresSinkRequest, StreamSinkEntity::new);
        entity.setStatus(SinkStatus.NEW.getCode());
        entity.setIsDeleted(GlobalConstants.UN_DELETED);
        entity.setCreator(operator);
        entity.setModifier(operator);
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setModifyTime(now);

        // get the ext params
        PostgresSinkDTO dto = PostgresSinkDTO.getFromRequest(postgresSinkRequest);
        try {
            entity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_SAVE_FAILED);
        }
        sinkMapper.insert(entity);
        Integer sinkId = entity.getId();
        request.setId(sinkId);
        this.saveFieldOpt(request);
        return sinkId;
    }

    @Override
    public void saveFieldOpt(SinkRequest request) {
        List<SinkField> fieldList = request.getSinkFieldList();
        LOGGER.info("begin to save field={}", fieldList);
        if (CollectionUtils.isEmpty(fieldList)) {
            return;
        }

        int size = fieldList.size();
        List<StreamSinkFieldEntity> entityList = new ArrayList<>(size);
        String groupId = request.getInlongGroupId();
        String streamId = request.getInlongStreamId();
        String sinkType = request.getSinkType();
        Integer sinkId = request.getId();
        for (SinkField fieldInfo : fieldList) {
            StreamSinkFieldEntity fieldEntity = CommonBeanUtils.copyProperties(fieldInfo, StreamSinkFieldEntity::new);
            if (StringUtils.isEmpty(fieldEntity.getFieldComment())) {
                fieldEntity.setFieldComment(fieldEntity.getFieldName());
            }
            fieldEntity.setInlongGroupId(groupId);
            fieldEntity.setInlongStreamId(streamId);
            fieldEntity.setSinkType(sinkType);
            fieldEntity.setSinkId(sinkId);
            fieldEntity.setIsDeleted(GlobalConstants.UN_DELETED);
            entityList.add(fieldEntity);
        }

        sinkFieldMapper.insertAll(entityList);
        LOGGER.info("success to save field");
    }

    @Override
    public StreamSink getByEntity(@NotNull StreamSinkEntity entity) {
        Preconditions.checkNotNull(entity, ErrorCodeEnum.SINK_INFO_NOT_FOUND.getMessage());
        String existType = entity.getSinkType();
        Preconditions.checkTrue(SinkType.SINK_POSTGRES.equals(existType),
                String.format(ErrorCodeEnum.SINK_TYPE_NOT_SAME.getMessage(), SinkType.SINK_POSTGRES, existType));
        StreamSink response = this.getFromEntity(entity, PostgresSink::new);
        List<StreamSinkFieldEntity> entities = sinkFieldMapper.selectBySinkId(entity.getId());
        List<SinkField> infos = CommonBeanUtils.copyListProperties(entities, SinkField::new);
        response.setSinkFieldList(infos);
        return response;
    }

    @Override
    public <T> T getFromEntity(StreamSinkEntity entity, Supplier<T> target) {
        T result = target.get();
        if (entity == null) {
            return result;
        }
        String existType = entity.getSinkType();
        Preconditions.checkTrue(SinkType.SINK_POSTGRES.equals(existType),
                String.format(ErrorCodeEnum.SINK_TYPE_NOT_SAME.getMessage(), SinkType.SINK_POSTGRES, existType));

        PostgresSinkDTO dto = PostgresSinkDTO.getFromJson(entity.getExtParams());
        CommonBeanUtils.copyProperties(entity, result, true);
        CommonBeanUtils.copyProperties(dto, result, true);

        return result;
    }

    @Override
    public PageInfo<? extends SinkListResponse> getPageInfo(Page<StreamSinkEntity> entityPage) {
        if (CollectionUtils.isEmpty(entityPage)) {
            return new PageInfo<>();
        }
        return entityPage.toPageInfo(entity -> this.getFromEntity(entity, PostgresSinkListResponse::new));
    }

    @Override
    public void updateOpt(SinkRequest request, String operator) {
        String sinkType = request.getSinkType();
        Preconditions.checkTrue(SinkType.SINK_POSTGRES.equals(sinkType),
                String.format(ErrorCodeEnum.SINK_TYPE_NOT_SAME.getMessage(), SinkType.SINK_POSTGRES, sinkType));

        StreamSinkEntity entity = sinkMapper.selectByPrimaryKey(request.getId());
        Preconditions.checkNotNull(entity, ErrorCodeEnum.SINK_INFO_NOT_FOUND.getMessage());
        PostgresSinkRequest postgresSinkRequest = (PostgresSinkRequest) request;
        CommonBeanUtils.copyProperties(postgresSinkRequest, entity, true);
        try {
            PostgresSinkDTO dto = PostgresSinkDTO.getFromRequest(postgresSinkRequest);
            entity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage());
        }

        entity.setPreviousStatus(entity.getStatus());
        entity.setStatus(SinkStatus.CONFIG_ING.getCode());
        entity.setModifier(operator);
        entity.setModifyTime(new Date());
        sinkMapper.updateByPrimaryKeySelective(entity);

        boolean onlyAdd = SinkStatus.CONFIG_SUCCESSFUL.getCode().equals(entity.getPreviousStatus());
        this.updateFieldOpt(onlyAdd, postgresSinkRequest);

        LOGGER.info("success to update sink of type={}", sinkType);
    }

    @Override
    public void updateFieldOpt(Boolean onlyAdd, SinkRequest request) {
        Integer sinkId = request.getId();
        List<SinkField> fieldRequestList = request.getSinkFieldList();
        if (CollectionUtils.isEmpty(fieldRequestList)) {
            return;
        }
        if (onlyAdd) {
            List<StreamSinkFieldEntity> existsFieldList = sinkFieldMapper.selectBySinkId(sinkId);
            if (existsFieldList.size() > fieldRequestList.size()) {
                throw new BusinessException(ErrorCodeEnum.SINK_FIELD_UPDATE_NOT_ALLOWED);
            }
            for (int i = 0; i < existsFieldList.size(); i++) {
                if (!existsFieldList.get(i).getFieldName().equals(fieldRequestList.get(i).getFieldName())) {
                    throw new BusinessException(ErrorCodeEnum.SINK_FIELD_UPDATE_NOT_ALLOWED);
                }
            }
        }
        // First physically delete the existing fields
        sinkFieldMapper.deleteAll(sinkId);
        // Then batch save the sink fields
        this.saveFieldOpt(request);
        LOGGER.info("success to update field");
    }

}
