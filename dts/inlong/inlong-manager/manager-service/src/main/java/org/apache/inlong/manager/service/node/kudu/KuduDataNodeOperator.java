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

package org.apache.inlong.manager.service.node.kudu;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.DataNodeType;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.DataNodeEntity;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;
import org.apache.inlong.manager.pojo.node.kudu.KuduDataNodeDTO;
import org.apache.inlong.manager.pojo.node.kudu.KuduDataNodeInfo;
import org.apache.inlong.manager.pojo.node.kudu.KuduDataNodeRequest;
import org.apache.inlong.manager.service.node.AbstractDataNodeOperator;
import org.apache.inlong.manager.service.resource.sink.kudu.KuduResourceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KuduDataNodeOperator extends AbstractDataNodeOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KuduDataNodeOperator.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String dataNodeType) {
        return getDataNodeType().equals(dataNodeType);
    }

    @Override
    public String getDataNodeType() {
        return DataNodeType.KUDU;
    }

    @Override
    protected void setTargetEntity(DataNodeRequest request, DataNodeEntity targetEntity) {
        KuduDataNodeRequest kuduRequest = (KuduDataNodeRequest) request;
        String masters = kuduRequest.getMasters();
        if (StringUtils.isBlank(masters)) {
            throw new BusinessException(ErrorCodeEnum.SINK_SAVE_FAILED, "masters can not be empty!");
        }
        if (masters.contains(InlongConstants.SEMICOLON)) {
            throw new BusinessException(ErrorCodeEnum.SINK_SAVE_FAILED, "masters can not contain comma!");
        }
        CommonBeanUtils.copyProperties(kuduRequest, targetEntity, true);
        try {
            KuduDataNodeDTO dto = KuduDataNodeDTO.getFromRequest(kuduRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT,
                    String.format("Failed to build extParams for Kudu node: %s", e.getMessage()));
        }
    }

    @Override
    public DataNodeInfo getFromEntity(DataNodeEntity entity) {
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NODE_NOT_FOUND);
        }
        KuduDataNodeInfo info = new KuduDataNodeInfo();
        CommonBeanUtils.copyProperties(entity, info);
        if (StringUtils.isNotBlank(entity.getExtParams())) {
            KuduDataNodeDTO dto = KuduDataNodeDTO.getFromJson(entity.getExtParams());
            CommonBeanUtils.copyProperties(dto, info);
        }
        return info;
    }

    @Override
    public Boolean testConnection(DataNodeRequest request) {
        KuduDataNodeRequest kuduRequest = (KuduDataNodeRequest) request;
        String masters = kuduRequest.getMasters();
        Preconditions.expectNotBlank(masters, ErrorCodeEnum.INVALID_PARAMETER, "masters cannot be empty");

        try (KuduResourceClient kuduClient = new KuduResourceClient(masters)) {
            kuduClient.getTablesList();
            LOGGER.info("kudu connection not null - connection success for masters={}",
                    masters);
            return true;
        } catch (Exception e) {
            String errMsg = String.format("kudu connection failed for masters=%s", masters);
            LOGGER.error(errMsg, e);
            throw new BusinessException(errMsg);
        }

    }
}
