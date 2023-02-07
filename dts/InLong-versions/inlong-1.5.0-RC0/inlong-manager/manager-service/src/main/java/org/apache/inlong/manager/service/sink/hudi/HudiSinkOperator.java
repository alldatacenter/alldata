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

package org.apache.inlong.manager.service.sink.hudi;

import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.FieldType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.pojo.node.hudi.HudiDataNodeInfo;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.hudi.HudiColumnInfo;
import org.apache.inlong.manager.pojo.sink.hudi.HudiSink;
import org.apache.inlong.manager.pojo.sink.hudi.HudiSinkDTO;
import org.apache.inlong.manager.pojo.sink.hudi.HudiSinkRequest;
import org.apache.inlong.manager.service.sink.AbstractSinkOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Hudi sink operator, such as save or update hudi field, etc.
 */
@Service
public class HudiSinkOperator extends AbstractSinkOperator {

    private static final String HOODIE_PRIMARY_KEY_FIELD = "hoodie.datasource.write.recordkey.field";

    private static final Logger LOGGER = LoggerFactory.getLogger(HudiSinkOperator.class);

    private static final String CATALOG_TYPE_HIVE = "HIVE";

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.HUDI.equals(sinkType);
    }

    @Override
    protected String getSinkType() {
        return SinkType.HUDI;
    }

    @Override
    protected void setTargetEntity(SinkRequest request, StreamSinkEntity targetEntity) {
        Preconditions.checkTrue(this.getSinkType().equals(request.getSinkType()),
                ErrorCodeEnum.SINK_TYPE_NOT_SUPPORT.getMessage() + ": " + getSinkType());
        HudiSinkRequest sinkRequest = (HudiSinkRequest) request;

        String partitionKey = sinkRequest.getPartitionKey();
        String primaryKey = sinkRequest.getPrimaryKey();
        boolean primaryKeyExist = StringUtils.isNotEmpty(partitionKey);
        boolean partitionKeyExist = StringUtils.isNotEmpty(primaryKey);
        if (primaryKeyExist || partitionKeyExist) {
            Set<String> fieldNames = sinkRequest.getSinkFieldList().stream().map(SinkField::getFieldName)
                    .collect(Collectors.toSet());
            if (primaryKeyExist) {
                checkState(
                        fieldNames.contains(partitionKey),
                        "The partitionKey({}) must be included in the sinkFieldList({})",
                        partitionKey, fieldNames);
            }
            if (partitionKeyExist) {
                checkState(
                        fieldNames.contains(partitionKey),
                        "The primaryKey({}) must be included in the sinkFieldList({})",
                        primaryKey,
                        fieldNames);
            }
        }

        try {
            HudiSinkDTO dto = HudiSinkDTO.getFromRequest(sinkRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            LOGGER.error("parsing json string to sink info failed", e);
            throw new BusinessException(ErrorCodeEnum.SINK_SAVE_FAILED.getMessage());
        }
    }

    @Override
    public StreamSink getFromEntity(StreamSinkEntity entity) {
        HudiSink sink = new HudiSink();
        if (entity == null) {
            return sink;
        }

        HudiSinkDTO dto = HudiSinkDTO.getFromJson(entity.getExtParams());
        if (StringUtils.isBlank(dto.getCatalogUri()) && CATALOG_TYPE_HIVE.equals(dto.getCatalogType())) {
            Preconditions.checkNotEmpty(entity.getDataNodeName(),
                    "hudi catalog uri unspecified and data node is empty");
            HudiDataNodeInfo dataNodeInfo = (HudiDataNodeInfo) dataNodeHelper.getDataNodeInfo(
                    entity.getDataNodeName(), entity.getSinkType());
            CommonBeanUtils.copyProperties(dataNodeInfo, dto, true);
            dto.setCatalogUri(dataNodeInfo.getUrl());
        }

        CommonBeanUtils.copyProperties(entity, sink, true);
        CommonBeanUtils.copyProperties(dto, sink, true);
        List<SinkField> sinkFields = super.getSinkFields(entity.getId());
        sink.setSinkFieldList(sinkFields);
        return sink;
    }

    @Override
    protected void checkFieldInfo(SinkField field) {
        if (FieldType.forName(field.getFieldType()) == FieldType.DECIMAL) {
            HudiColumnInfo info = HudiColumnInfo.getFromJson(field.getExtParams());
            if (info.getPrecision() == null || info.getScale() == null) {
                String errorMsg = String.format("precision or scale not specified for decimal field (%s)",
                        field.getFieldName());
                LOGGER.error("field info check error: {}", errorMsg);
                throw new BusinessException(errorMsg);
            }
            if (info.getPrecision() < info.getScale()) {
                String errorMsg = String.format(
                        "precision (%d) must be greater or equal than scale (%d) for decimal field (%s)",
                        info.getPrecision(), info.getScale(), field.getFieldName());
                LOGGER.error("field info check error: {}", errorMsg);
                throw new BusinessException(errorMsg);
            }
        }
    }

}
