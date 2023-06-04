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

package org.apache.inlong.manager.service.cluster.node;

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.NodeStatus;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.InlongClusterNodeEntity;
import org.apache.inlong.manager.dao.mapper.InlongClusterNodeEntityMapper;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default operator of inlong cluster node.
 */
public abstract class AbstractClusterNodeOperator implements InlongClusterNodeOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClusterNodeOperator.class);

    @Autowired
    protected InlongClusterNodeEntityMapper clusterNodeMapper;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Integer saveOpt(ClusterNodeRequest request, String operator) {
        InlongClusterNodeEntity entity = CommonBeanUtils.copyProperties(request, InlongClusterNodeEntity::new);
        // set the ext params
        this.setTargetEntity(request, entity);

        entity.setCreator(operator);
        entity.setModifier(operator);
        entity.setStatus(NodeStatus.HEARTBEAT_TIMEOUT.getStatus());
        clusterNodeMapper.insert(entity);

        return entity.getId();
    }

    /**
     * Set the parameters of the target entity.
     *
     * @param request inlong cluster request
     * @param targetEntity entity which will set the new parameters
     */
    protected abstract void setTargetEntity(ClusterNodeRequest request, InlongClusterNodeEntity targetEntity);

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ)
    public void updateOpt(ClusterNodeRequest request, String operator) {
        InlongClusterNodeEntity entity = CommonBeanUtils.copyProperties(request, InlongClusterNodeEntity::new);
        // set the ext params
        this.setTargetEntity(request, entity);
        entity.setModifier(operator);
        if (InlongConstants.AFFECTED_ONE_ROW != clusterNodeMapper.updateByIdSelective(entity)) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED,
                    String.format(
                            "cluster node has already updated with ip=%s, port=%s, protocolType=%s, type=%s, curVersion=%s",
                            entity.getIp(), entity.getPort(), entity.getProtocolType(), entity.getType(),
                            entity.getVersion()));
        }
        LOGGER.debug("success to update inlong cluster node={}", request);
    }
}
