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

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.InlongClusterNodeEntity;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default cluster node operator.
 */
@Slf4j
@Service
public class DefaultClusterNodeOperator extends AbstractClusterNodeOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClusterNodeOperator.class);
    public static final String DEFAULT = "DEFAULT";

    @Override
    public Boolean accept(String clusterNodeType) {
        return getClusterNodeType().equals(clusterNodeType);
    }

    @Override
    public String getClusterNodeType() {
        return DEFAULT;
    }

    @Override
    public ClusterNodeResponse getFromEntity(InlongClusterNodeEntity entity) {
        ClusterNodeResponse clusterNodeResponse = CommonBeanUtils.copyProperties(entity, ClusterNodeResponse::new);
        LOGGER.debug("success to get inlong cluster node by id={}", entity.getId());
        return clusterNodeResponse;
    }

    @Override
    protected void setTargetEntity(ClusterNodeRequest request, InlongClusterNodeEntity targetEntity) {
        LOGGER.debug("do nothing for default cluster node in set target entity");
    }
}
