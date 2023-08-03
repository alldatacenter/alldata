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

package org.apache.inlong.manager.service.node.postgresql;

import org.apache.inlong.manager.common.consts.DataNodeType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.DataNodeEntity;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;
import org.apache.inlong.manager.pojo.node.postgresql.PostgreSQLDataNodeDTO;
import org.apache.inlong.manager.pojo.node.postgresql.PostgreSQLDataNodeInfo;
import org.apache.inlong.manager.pojo.node.postgresql.PostgreSQLDataNodeRequest;
import org.apache.inlong.manager.service.node.AbstractDataNodeOperator;
import org.apache.inlong.manager.service.resource.sink.postgresql.PostgreSQLJdbcUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;

/**
 * PostgreSQL data node operator
 */
@Service
public class PostgreSQLDataNodeOperator extends AbstractDataNodeOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreSQLDataNodeOperator.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String dataNodeType) {
        return getDataNodeType().equals(dataNodeType);
    }

    @Override
    public String getDataNodeType() {
        return DataNodeType.POSTGRESQL;
    }

    @Override
    public DataNodeInfo getFromEntity(DataNodeEntity entity) {
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NODE_NOT_FOUND);
        }

        PostgreSQLDataNodeInfo dataNodeInfo = new PostgreSQLDataNodeInfo();
        CommonBeanUtils.copyProperties(entity, dataNodeInfo);
        if (StringUtils.isNotBlank(entity.getExtParams())) {
            PostgreSQLDataNodeDTO dto = PostgreSQLDataNodeDTO.getFromJson(entity.getExtParams());
            CommonBeanUtils.copyProperties(dto, dataNodeInfo);
        }
        return dataNodeInfo;
    }

    @Override
    protected void setTargetEntity(DataNodeRequest request, DataNodeEntity targetEntity) {
        PostgreSQLDataNodeRequest nodeRequest = (PostgreSQLDataNodeRequest) request;
        CommonBeanUtils.copyProperties(nodeRequest, targetEntity, true);
        try {
            PostgreSQLDataNodeDTO dto = PostgreSQLDataNodeDTO.getFromRequest(nodeRequest, targetEntity.getExtParams());
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT,
                    String.format("Failed to build extParams for PostgreSQL node: %s", e.getMessage()));
        }
    }

    @Override
    public Boolean testConnection(DataNodeRequest request) {
        String jdbcUrl = PostgreSQLDataNodeDTO.convertToJdbcUrl(request.getUrl());
        String username = request.getUsername();
        String password = request.getToken();
        Preconditions.expectNotBlank(jdbcUrl, ErrorCodeEnum.INVALID_PARAMETER, "connection jdbcUrl cannot be empty");
        try (Connection ignored = PostgreSQLJdbcUtils.getConnection(jdbcUrl, username, password)) {
            LOGGER.info("postgresql connection not null - connection success for jdbcUrl={}, username={}, password={}",
                    jdbcUrl, username, password);
            return true;
        } catch (Exception e) {
            String errMsg = String.format("postgresql connection failed for jdbcUrl=%s, username=%s, password=%s",
                    jdbcUrl,
                    username, password);
            LOGGER.error(errMsg, e);
            throw new BusinessException(errMsg);
        }
    }
}
