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

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.dao.entity.DataNodeEntity;
import org.apache.inlong.manager.dao.mapper.DataNodeEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSourceEntityMapper;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default operation of data node.
 */
public abstract class AbstractDataNodeOperator implements DataNodeOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDataNodeOperator.class);

    @Autowired
    protected DataNodeEntityMapper dataNodeEntityMapper;
    @Autowired
    protected StreamSourceEntityMapper sourceMapper;
    @Autowired
    protected InlongGroupEntityMapper groupMapper;
    @Autowired
    protected InlongStreamEntityMapper streamMapper;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Integer saveOpt(DataNodeRequest request, String operator) {
        DataNodeEntity entity = CommonBeanUtils.copyProperties(request, DataNodeEntity::new);
        // set the ext params
        this.setTargetEntity(request, entity);
        entity.setCreator(operator);
        entity.setModifier(operator);
        dataNodeEntityMapper.insert(entity);

        return entity.getId();
    }

    /**
     * Set the parameters of the target entity.
     *
     * @param request data node request
     * @param targetEntity entity which will set the new parameters
     */
    protected abstract void setTargetEntity(DataNodeRequest request, DataNodeEntity targetEntity);

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ)
    public void updateOpt(DataNodeRequest request, String operator) {
        DataNodeEntity entity = CommonBeanUtils.copyProperties(request, DataNodeEntity::new);
        // set the ext params
        this.setTargetEntity(request, entity);
        entity.setModifier(operator);
        int rowCount = dataNodeEntityMapper.updateByIdSelective(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED,
                    String.format(
                            "failure to update data node with name=%s, type=%s, request version=%d, updated row=%d",
                            request.getName(), request.getType(), request.getVersion(), rowCount));
        }
    }

    @Override
    public Map<String, String> parse2SinkParams(DataNodeInfo info) {
        return JsonUtils.parseObject(info.getExtParams(), HashMap.class);
    }

    @Override
    public Boolean testConnection(DataNodeRequest request) {
        throw new BusinessException(
                String.format(ErrorCodeEnum.DATA_NODE_TYPE_NOT_SUPPORTED.getMessage(), request.getType()));
    }

    @Override
    public void updateRelatedStreamSource(DataNodeRequest request, DataNodeEntity entity, String operator) {
        LOGGER.info("do nothing for the data node type ={}", request.getType());
    }

    public void retryStreamSourceByDataNodeNameAndType(String dataNodeName, String type, String operator) {
        Integer status = SourceStatus.TO_BE_ISSUED_RETRY.getCode();
        LOGGER.info("begin to update stream source status by dataNodeName={}, status={}, by operator={}",
                dataNodeName, status, operator);
        List<Integer> needUpdateIds = sourceMapper.selectNeedUpdateIdsByClusterAndDataNode(null, dataNodeName, type);
        try {
            sourceMapper.updateStatusByIds(needUpdateIds, status, operator);
            LOGGER.info("success to update stream source status by dataNodeName={}, status={}, by operator={}",
                    dataNodeName, status, operator);
        } catch (Exception e) {
            LOGGER.error("failed to update stream source status by dataNodeName={}, status={}, by operator={}",
                    dataNodeName, status, operator, e);
        }
    }
}
