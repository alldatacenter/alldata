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

package org.apache.inlong.manager.service.cluster;

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.dao.mapper.InlongClusterEntityMapper;
import org.apache.inlong.manager.pojo.cluster.ClusterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default operator of inlong cluster.
 */
public abstract class AbstractClusterOperator implements InlongClusterOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClusterOperator.class);

    @Autowired
    protected InlongClusterEntityMapper clusterMapper;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Integer saveOpt(ClusterRequest request, String operator) {
        InlongClusterEntity entity = CommonBeanUtils.copyProperties(request, InlongClusterEntity::new);
        // set the ext params
        this.setTargetEntity(request, entity);

        entity.setCreator(operator);
        entity.setModifier(operator);
        clusterMapper.insert(entity);

        return entity.getId();
    }

    /**
     * Set the parameters of the target entity.
     *
     * @param request inlong cluster request
     * @param targetEntity entity which will set the new parameters
     */
    protected abstract void setTargetEntity(ClusterRequest request, InlongClusterEntity targetEntity);

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ)
    public void updateOpt(ClusterRequest request, String operator) {
        InlongClusterEntity entity = CommonBeanUtils.copyProperties(request, InlongClusterEntity::new);
        // set the ext params
        this.setTargetEntity(request, entity);
        entity.setModifier(operator);
        int rowCount = clusterMapper.updateByIdSelective(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            LOGGER.error("cluster has already updated with name={}, type={}, curVersion={}", request.getName(),
                    request.getType(), request.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
    }

    @Override
    public Boolean testConnection(ClusterRequest request) {
        throw new BusinessException(
                String.format(ErrorCodeEnum.CLUSTER_TYPE_NOT_SUPPORTED.getMessage(), request.getType()));
    }

}
