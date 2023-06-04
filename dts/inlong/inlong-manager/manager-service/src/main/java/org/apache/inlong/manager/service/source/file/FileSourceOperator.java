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

package org.apache.inlong.manager.service.source.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.dao.mapper.StreamSourceEntityMapper;
import org.apache.inlong.manager.pojo.source.SourceRequest;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.source.file.FileSource;
import org.apache.inlong.manager.pojo.source.file.FileSourceDTO;
import org.apache.inlong.manager.pojo.source.file.FileSourceRequest;
import org.apache.inlong.manager.pojo.source.SubSourceDTO;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.service.source.AbstractSourceOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * File source operator, such as get or set file source info.
 */
@Service
public class FileSourceOperator extends AbstractSourceOperator {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StreamSourceEntityMapper sourceMapper;

    @Override
    public Boolean accept(String sourceType) {
        return SourceType.FILE.equals(sourceType);
    }

    @Override
    protected String getSourceType() {
        return SourceType.FILE;
    }

    @Override
    protected void setTargetEntity(SourceRequest request, StreamSourceEntity targetEntity) {
        FileSourceRequest sourceRequest = (FileSourceRequest) request;
        try {
            CommonBeanUtils.copyProperties(sourceRequest, targetEntity, true);
            FileSourceDTO dto = FileSourceDTO.getFromRequest(sourceRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT,
                    String.format("serialize extParams of File SourceDTO failure: %s", e.getMessage()));
        }
    }

    @Override
    public StreamSource getFromEntity(StreamSourceEntity entity) {
        FileSource source = new FileSource();
        if (entity == null) {
            return source;
        }

        FileSourceDTO dto = FileSourceDTO.getFromJson(entity.getExtParams());
        CommonBeanUtils.copyProperties(entity, source, true);
        CommonBeanUtils.copyProperties(dto, source, true);

        List<StreamField> sourceFields = super.getSourceFields(entity.getId());
        source.setFieldList(sourceFields);

        List<StreamSourceEntity> subSourceList = sourceMapper.selectByTemplateId(entity.getId());
        source.setSubSourceList(subSourceList.stream().map(subEntity -> SubSourceDTO.builder()
                .id(subEntity.getId())
                .templateId(entity.getId())
                .agentIp(subEntity.getAgentIp())
                .status(subEntity.getStatus()).build())
                .collect(Collectors.toList()));
        return source;
    }

}
