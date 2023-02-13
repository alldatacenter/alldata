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

package org.apache.inlong.manager.service.node.hudi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.DataNodeType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.DataNodeEntity;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;
import org.apache.inlong.manager.pojo.node.hudi.HudiDataNodeDTO;
import org.apache.inlong.manager.pojo.node.hudi.HudiDataNodeInfo;
import org.apache.inlong.manager.pojo.node.hudi.HudiDataNodeRequest;
import org.apache.inlong.manager.service.node.AbstractDataNodeOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HudiDataNodeOperator extends AbstractDataNodeOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(HudiDataNodeOperator.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String dataNodeType) {
        return getDataNodeType().equals(dataNodeType);
    }

    @Override
    public String getDataNodeType() {
        return DataNodeType.HUDI;
    }

    @Override
    public DataNodeInfo getFromEntity(DataNodeEntity entity) {
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NODE_NOT_FOUND);
        }

        HudiDataNodeInfo hudiDataNodeInfo = new HudiDataNodeInfo();
        CommonBeanUtils.copyProperties(entity, hudiDataNodeInfo);
        if (StringUtils.isNotBlank(entity.getExtParams())) {
            HudiDataNodeDTO dto = HudiDataNodeDTO.getFromJson(entity.getExtParams());
            CommonBeanUtils.copyProperties(dto, hudiDataNodeInfo);
        }
        return hudiDataNodeInfo;
    }

    @Override
    protected void setTargetEntity(DataNodeRequest request, DataNodeEntity targetEntity) {
        HudiDataNodeRequest hudiDataNodeRequest = (HudiDataNodeRequest) request;
        CommonBeanUtils.copyProperties(hudiDataNodeRequest, targetEntity, true);
        try {
            HudiDataNodeDTO dto = HudiDataNodeDTO.getFromRequest(hudiDataNodeRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT,
                    String.format("Failed to build extParams for Hudi node: %s", e.getMessage()));
        }
    }

}
