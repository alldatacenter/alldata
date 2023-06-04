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

package org.apache.inlong.manager.service.sink.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.DataNodeType;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.AESUtils;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.dao.entity.StreamSinkFieldEntity;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.es.ElasticsearchFieldInfo;
import org.apache.inlong.manager.pojo.sink.es.ElasticsearchSink;
import org.apache.inlong.manager.pojo.sink.es.ElasticsearchSinkDTO;
import org.apache.inlong.manager.pojo.sink.es.ElasticsearchSinkRequest;
import org.apache.inlong.manager.service.sink.AbstractSinkOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch sink operator, such as save or update Elasticsearch field, etc.
 */
@Service
public class ElasticsearchSinkOperator extends AbstractSinkOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchSinkOperator.class);
    private static final String KEY_FIELDS = "fieldNames";

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.ELASTICSEARCH.equals(sinkType);
    }

    @Override
    protected String getSinkType() {
        return SinkType.ELASTICSEARCH;
    }

    @Override
    protected void setTargetEntity(SinkRequest request, StreamSinkEntity targetEntity) {
        if (!this.getSinkType().equals(request.getSinkType())) {
            throw new BusinessException(ErrorCodeEnum.SINK_TYPE_NOT_SUPPORT,
                    ErrorCodeEnum.SINK_TYPE_NOT_SUPPORT.getMessage() + ": " + getSinkType());
        }
        ElasticsearchSinkRequest sinkRequest = (ElasticsearchSinkRequest) request;
        try {
            ElasticsearchSinkDTO dto = ElasticsearchSinkDTO.getFromRequest(sinkRequest);

            DataNodeInfo dataNodeInfo =
                    dataNodeHelper.getDataNodeInfo(request.getDataNodeName(), DataNodeType.ELASTICSEARCH);
            String esUrl = dataNodeInfo.getUrl();
            dto.setHosts(esUrl);

            dto.setUsername(dataNodeInfo.getUsername());
            Integer encryptVersion = AESUtils.getCurrentVersion(null);
            String passwd = null;
            if (StringUtils.isNotEmpty(dataNodeInfo.getToken())) {
                passwd = AESUtils.encryptToString(dataNodeInfo.getToken().getBytes(StandardCharsets.UTF_8),
                        encryptVersion);
            }
            dto.setPassword(passwd);
            dto.setEncryptVersion(encryptVersion);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_SAVE_FAILED,
                    String.format("serialize extParams of Elasticsearch SinkDTO failure: %s", e.getMessage()));
        }
    }

    @Override
    public StreamSink getFromEntity(StreamSinkEntity entity) {
        ElasticsearchSink sink = new ElasticsearchSink();
        if (entity == null) {
            return sink;
        }

        ElasticsearchSinkDTO dto = ElasticsearchSinkDTO.getFromJson(entity.getExtParams());
        CommonBeanUtils.copyProperties(entity, sink, true);
        CommonBeanUtils.copyProperties(dto, sink, true);
        List<SinkField> sinkFields = getSinkFields(entity.getId());
        sink.setSinkFieldList(sinkFields);
        return sink;
    }

    @Override
    public Map<String, String> parse2IdParams(StreamSinkEntity streamSink, List<String> fields) {
        Map<String, String> idParams = super.parse2IdParams(streamSink, fields);
        StringBuilder sb = new StringBuilder();
        for (String field : fields) {
            sb.append(field).append(" ");
        }
        idParams.put(KEY_FIELDS, sb.toString());
        return idParams;
    }

    @Override
    public void saveFieldOpt(SinkRequest request) {
        List<SinkField> fieldList = request.getSinkFieldList();
        LOGGER.debug("begin to save es sink fields={}", fieldList);
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
                ElasticsearchFieldInfo dto = ElasticsearchFieldInfo.getFromRequest(fieldInfo);
                fieldEntity.setExtParams(objectMapper.writeValueAsString(dto));
            } catch (Exception e) {
                throw new BusinessException(ErrorCodeEnum.SINK_SAVE_FAILED,
                        String.format("serialize extParams of Elasticsearch FieldInfo failure: %s", e.getMessage()));
            }
            fieldEntity.setInlongGroupId(groupId);
            fieldEntity.setInlongStreamId(streamId);
            fieldEntity.setSinkType(sinkType);
            fieldEntity.setSinkId(sinkId);
            fieldEntity.setIsDeleted(InlongConstants.UN_DELETED);
            entityList.add(fieldEntity);
        }

        sinkFieldMapper.insertAll(entityList);
        LOGGER.debug("success to save es sink fields");
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
                ElasticsearchFieldInfo elasticsearchFieldInfo = ElasticsearchFieldInfo.getFromJson(
                        field.getExtParams());
                CommonBeanUtils.copyProperties(field, elasticsearchFieldInfo, true);
                fieldList.add(elasticsearchFieldInfo);
            } else {
                CommonBeanUtils.copyProperties(field, sinkField, true);
                fieldList.add(sinkField);
            }

        });
        return fieldList;
    }

}
