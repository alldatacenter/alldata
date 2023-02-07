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

package org.apache.inlong.manager.service.node.iceberg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.iceberg.hive.HiveCatalog;
import org.apache.inlong.manager.common.consts.DataNodeType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.DataNodeEntity;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;
import org.apache.inlong.manager.pojo.node.iceberg.IcebergDataNodeDTO;
import org.apache.inlong.manager.pojo.node.iceberg.IcebergDataNodeInfo;
import org.apache.inlong.manager.pojo.node.iceberg.IcebergDataNodeRequest;
import org.apache.inlong.manager.service.node.AbstractDataNodeOperator;
import org.apache.inlong.manager.service.resource.sink.iceberg.IcebergCatalogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IcebergDataNodeOperator extends AbstractDataNodeOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcebergDataNodeOperator.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String dataNodeType) {
        return getDataNodeType().equals(dataNodeType);
    }

    @Override
    public String getDataNodeType() {
        return DataNodeType.ICEBERG;
    }

    @Override
    public DataNodeInfo getFromEntity(DataNodeEntity entity) {
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NODE_NOT_FOUND);
        }
        IcebergDataNodeInfo icebergDataNodeInfo = new IcebergDataNodeInfo();
        CommonBeanUtils.copyProperties(entity, icebergDataNodeInfo);
        if (StringUtils.isNotBlank(entity.getExtParams())) {
            IcebergDataNodeDTO dto = IcebergDataNodeDTO.getFromJson(entity.getExtParams());
            CommonBeanUtils.copyProperties(dto, icebergDataNodeInfo);
        }
        return icebergDataNodeInfo;
    }

    @Override
    protected void setTargetEntity(DataNodeRequest request, DataNodeEntity targetEntity) {
        IcebergDataNodeRequest icebergDataNodeRequest = (IcebergDataNodeRequest) request;
        CommonBeanUtils.copyProperties(icebergDataNodeRequest, targetEntity, true);
        try {
            IcebergDataNodeDTO dto = IcebergDataNodeDTO.getFromRequest(icebergDataNodeRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT,
                    String.format("Failed to build extParams for Iceberg node: %s", e.getMessage()));
        }
    }

    @Override
    public Boolean testConnection(DataNodeRequest request) {
        IcebergDataNodeRequest icebergDataNodeRequest = (IcebergDataNodeRequest) request;
        String metastoreUri = icebergDataNodeRequest.getUrl();
        String warehouse = icebergDataNodeRequest.getWarehouse();
        Preconditions.checkNotNull(metastoreUri, "connection url cannot be empty");
        try {
            HiveCatalog catalog = IcebergCatalogUtils.getCatalog(metastoreUri, warehouse);
            catalog.listNamespaces();
            LOGGER.info("iceberg connection not null - connection success for metastoreUri={}, warehouse={}",
                    metastoreUri, warehouse);
            return true;
        } catch (Exception e) {
            String errMsg = String.format("iceberg connection failed for metastoreUri=%s, warhouse=%s", metastoreUri,
                    warehouse);
            LOGGER.error(errMsg, e);
            throw new BusinessException(errMsg);
        }
    }

}
