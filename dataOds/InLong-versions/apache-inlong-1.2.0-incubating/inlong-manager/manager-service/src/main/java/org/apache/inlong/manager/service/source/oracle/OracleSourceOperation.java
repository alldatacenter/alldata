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

package org.apache.inlong.manager.service.source.oracle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.SourceType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.source.SourceListResponse;
import org.apache.inlong.manager.common.pojo.source.SourceRequest;
import org.apache.inlong.manager.common.pojo.source.StreamSource;
import org.apache.inlong.manager.common.pojo.source.oracle.OracleSource;
import org.apache.inlong.manager.common.pojo.source.oracle.OracleSourceDTO;
import org.apache.inlong.manager.common.pojo.source.oracle.OracleSourceListResponse;
import org.apache.inlong.manager.common.pojo.source.oracle.OracleSourceRequest;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.service.source.AbstractSourceOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Oracle source operation
 */
@Service
public class OracleSourceOperation extends AbstractSourceOperation {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(SourceType sourceType) {
        return SourceType.ORACLE == sourceType;
    }

    @Override
    protected String getSourceType() {
        return SourceType.ORACLE.getType();
    }

    @Override
    protected StreamSource getSource() {
        return new OracleSource();
    }

    @Override
    public PageInfo<? extends SourceListResponse> getPageInfo(Page<StreamSourceEntity> entityPage) {
        if (CollectionUtils.isEmpty(entityPage)) {
            return new PageInfo<>();
        }
        return entityPage.toPageInfo(entity -> this.getFromEntity(entity, OracleSourceListResponse::new));
    }

    @Override
    protected void setTargetEntity(SourceRequest request, StreamSourceEntity targetEntity) {
        OracleSourceRequest sourceRequest = (OracleSourceRequest) request;
        CommonBeanUtils.copyProperties(sourceRequest, targetEntity, true);
        try {
            OracleSourceDTO dto = OracleSourceDTO.getFromRequest(sourceRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT.getMessage());
        }
    }

    @Override
    public <T> T getFromEntity(StreamSourceEntity entity, Supplier<T> target) {
        T result = target.get();
        if (entity == null) {
            return result;
        }
        String existType = entity.getSourceType();
        Preconditions.checkTrue(getSourceType().equals(existType),
                String.format(ErrorCodeEnum.SOURCE_TYPE_NOT_SAME.getMessage(), getSourceType(), existType));
        OracleSourceDTO dto = OracleSourceDTO.getFromJson(entity.getExtParams());
        CommonBeanUtils.copyProperties(entity, result, true);
        CommonBeanUtils.copyProperties(dto, result, true);
        return result;
    }
}
