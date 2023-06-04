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

package org.apache.inlong.manager.service.node.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.DataNodeType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.DataNodeEntity;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;
import org.apache.inlong.manager.pojo.node.es.ElasticsearchDataNodeDTO;
import org.apache.inlong.manager.pojo.node.es.ElasticsearchDataNodeInfo;
import org.apache.inlong.manager.pojo.node.es.ElasticsearchDataNodeRequest;
import org.apache.inlong.manager.service.node.AbstractDataNodeOperator;
import org.apache.inlong.manager.service.resource.sink.es.ElasticsearchApi;
import org.apache.inlong.manager.service.resource.sink.es.ElasticsearchConfig;
import org.elasticsearch.client.RequestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ElasticsearchDataNodeOperator extends AbstractDataNodeOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchDataNodeOperator.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String dataNodeType) {
        return getDataNodeType().equals(dataNodeType);
    }

    @Override
    public String getDataNodeType() {
        return DataNodeType.ELASTICSEARCH;
    }

    @Override
    protected void setTargetEntity(DataNodeRequest request, DataNodeEntity targetEntity) {
        ElasticsearchDataNodeRequest esRequest = (ElasticsearchDataNodeRequest) request;
        CommonBeanUtils.copyProperties(esRequest, targetEntity, true);
        try {
            ElasticsearchDataNodeDTO dto = ElasticsearchDataNodeDTO.getFromRequest(esRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT,
                    String.format("Failed to build extParams for Elasticsearch node: %s", e.getMessage()));
        }
    }

    @Override
    public DataNodeInfo getFromEntity(DataNodeEntity entity) {
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NODE_NOT_FOUND);
        }
        ElasticsearchDataNodeInfo info = new ElasticsearchDataNodeInfo();
        CommonBeanUtils.copyProperties(entity, info);
        if (StringUtils.isNotBlank(entity.getExtParams())) {
            ElasticsearchDataNodeDTO dto = ElasticsearchDataNodeDTO.getFromJson(entity.getExtParams());
            CommonBeanUtils.copyProperties(dto, info);
        }
        return info;
    }

    @Override
    public Boolean testConnection(DataNodeRequest request) {
        String url = request.getUrl();
        String username = request.getUsername();
        String password = request.getToken();
        Preconditions.expectNotBlank(url, ErrorCodeEnum.INVALID_PARAMETER, "connection url cannot be empty");
        ElasticsearchApi client = new ElasticsearchApi();
        ElasticsearchConfig config = new ElasticsearchConfig();
        if (StringUtils.isNotEmpty(request.getUsername())) {
            config.setAuthEnable(true);
            config.setUsername(username);
            config.setPassword(password);
        }
        config.setHosts(url);
        client.setEsConfig(config);
        boolean result;
        try {
            result = client.getEsClient().ping(RequestOptions.DEFAULT);
            LOGGER.info("elasticsearch connection is {} for url={}, username={}, password={}", result, url, username,
                    password);
            return result;
        } catch (Exception e) {
            String errMsg = String.format("elasticsearch connection failed for url=%s, username=%s, password=%s", url,
                    username, password);
            LOGGER.error(errMsg, e);
            throw new BusinessException(errMsg);
        }
    }

}
