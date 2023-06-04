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

package org.apache.inlong.manager.service.node;

import lombok.extern.slf4j.Slf4j;

import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.dao.entity.DataNodeEntity;
import org.apache.inlong.manager.dao.mapper.DataNodeEntityMapper;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Data node helper service
 */
@Slf4j
@Service
public class DataNodeOperateHelper {

    @Autowired
    private DataNodeEntityMapper dataNodeMapper;
    @Autowired
    private DataNodeOperatorFactory operatorFactory;

    /**
     * Get data node info by name and type
     */
    public DataNodeInfo getDataNodeInfo(String nodeName, String nodeType) {
        DataNodeEntity entity = dataNodeMapper.selectByUniqueKey(nodeName, nodeType);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.RECORD_NOT_FOUND,
                    String.format("data node not found by name=%s, type=%s", nodeName, nodeType));
        }
        DataNodeOperator dataNodeOperator = operatorFactory.getInstance(nodeType);
        DataNodeInfo dataNodeInfo = dataNodeOperator.getFromEntity(entity);
        log.debug("success to get data node info by name={}, type={}", nodeName, nodeType);
        return dataNodeInfo;
    }
}
