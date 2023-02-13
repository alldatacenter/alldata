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

package org.apache.inlong.manager.service.sink.starrocks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.dao.entity.StreamSinkFieldEntity;
import org.apache.inlong.manager.pojo.node.starrocks.StarRocksDataNodeInfo;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.starrocks.StarRocksColumnInfo;
import org.apache.inlong.manager.pojo.sink.starrocks.StarRocksSink;
import org.apache.inlong.manager.pojo.sink.starrocks.StarRocksSinkDTO;
import org.apache.inlong.manager.pojo.sink.starrocks.StarRocksSinkRequest;
import org.apache.inlong.manager.service.sink.AbstractSinkOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * StarRocks sink operator, such as save or update StarRocks field, etc.
 */
@Service
public class StarRocksSinkOperator extends AbstractSinkOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StarRocksSinkOperator.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.STARROCKS.equals(sinkType);
    }

    @Override
    protected String getSinkType() {
        return SinkType.STARROCKS;
    }

    @Override
    protected void setTargetEntity(SinkRequest request, StreamSinkEntity targetEntity) {
        Preconditions.checkTrue(this.getSinkType().equals(request.getSinkType()),
                ErrorCodeEnum.SINK_TYPE_NOT_SUPPORT.getMessage() + ": " + getSinkType());
        StarRocksSinkRequest sinkRequest = (StarRocksSinkRequest) request;
        try {
            StarRocksSinkDTO dto = StarRocksSinkDTO.getFromRequest(sinkRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            LOGGER.error("parsing json string to sink info failed", e);
            throw new BusinessException(ErrorCodeEnum.SINK_SAVE_FAILED.getMessage());
        }
    }

    @Override
    public StreamSink getFromEntity(@NotNull StreamSinkEntity entity) {
        StarRocksSink sink = new StarRocksSink();
        if (entity == null) {
            return sink;
        }

        StarRocksSinkDTO dto = StarRocksSinkDTO.getFromJson(entity.getExtParams());
        if (StringUtils.isBlank(dto.getJdbcUrl())) {
            Preconditions.checkNotEmpty(entity.getDataNodeName(),
                    "starRocks jdbc url unspecified and data node is empty");
            StarRocksDataNodeInfo dataNodeInfo = (StarRocksDataNodeInfo) dataNodeHelper.getDataNodeInfo(
                    entity.getDataNodeName(), entity.getSinkType());
            CommonBeanUtils.copyProperties(dataNodeInfo, dto, true);
            dto.setJdbcUrl(dataNodeInfo.getUrl());
            dto.setPassword(dataNodeInfo.getToken());
        }
        Preconditions.checkNotEmpty(dto.getLoadUrl(), "StarRocks load url is empty");
        Preconditions.checkNotEmpty(dto.getJdbcUrl(), "StarRocks jdbc url is empty");
        CommonBeanUtils.copyProperties(entity, sink, true);
        CommonBeanUtils.copyProperties(dto, sink, true);
        List<SinkField> sinkFields = super.getSinkFields(entity.getId());
        sink.setSinkFieldList(sinkFields);
        return sink;
    }

    @Override
    public void saveFieldOpt(SinkRequest request) {
        List<SinkField> fieldList = request.getSinkFieldList();
        LOGGER.info("begin to save es sink fields={}", fieldList);
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
            this.checkFieldInfo(fieldInfo);
            StreamSinkFieldEntity fieldEntity = CommonBeanUtils.copyProperties(fieldInfo, StreamSinkFieldEntity::new);
            if (StringUtils.isEmpty(fieldEntity.getFieldComment())) {
                fieldEntity.setFieldComment(fieldEntity.getFieldName());
            }
            try {
                StarRocksColumnInfo dto = StarRocksColumnInfo.getFromRequest(fieldInfo);
                fieldEntity.setExtParams(objectMapper.writeValueAsString(dto));
            } catch (Exception e) {
                LOGGER.error("parsing json string to sink field info failed", e);
                throw new BusinessException(ErrorCodeEnum.SINK_SAVE_FAILED.getMessage());
            }
            fieldEntity.setInlongGroupId(groupId);
            fieldEntity.setInlongStreamId(streamId);
            fieldEntity.setSinkType(sinkType);
            fieldEntity.setSinkId(sinkId);
            fieldEntity.setIsDeleted(InlongConstants.UN_DELETED);
            entityList.add(fieldEntity);
        }

        sinkFieldMapper.insertAll(entityList);
        LOGGER.info("success to save starRock sink fields");
    }

    @Override
    public List<SinkField> getSinkFields(Integer sinkId) {
        List<StreamSinkFieldEntity> sinkFieldEntities = sinkFieldMapper.selectBySinkId(sinkId);
        List<SinkField> fieldList = new ArrayList<>();
        if (CollectionUtils.isEmpty(sinkFieldEntities)) {
            return fieldList;
        }
        sinkFieldEntities.forEach(field -> {
            SinkField sinkField = new SinkField();
            if (StringUtils.isNotBlank(field.getExtParams())) {
                StarRocksColumnInfo starRocksColumnInfo = StarRocksColumnInfo.getFromJson(
                        field.getExtParams());
                CommonBeanUtils.copyProperties(field, starRocksColumnInfo, true);
                fieldList.add(starRocksColumnInfo);
            } else {
                CommonBeanUtils.copyProperties(field, sinkField, true);
                fieldList.add(sinkField);
            }

        });
        return fieldList;
    }

}
