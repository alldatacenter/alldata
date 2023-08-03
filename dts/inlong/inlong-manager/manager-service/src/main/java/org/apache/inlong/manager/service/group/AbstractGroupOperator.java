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

package org.apache.inlong.manager.service.group;

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.mapper.InlongClusterEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupExtEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamExtEntityMapper;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.service.cluster.InlongClusterOperator;
import org.apache.inlong.manager.service.cluster.InlongClusterOperatorFactory;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.stream.InlongStreamService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.util.set.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default operator of inlong group.
 */
public abstract class AbstractGroupOperator implements InlongGroupOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGroupOperator.class);

    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected InlongStreamService streamService;
    @Autowired
    protected InlongClusterService clusterService;

    @Autowired
    protected InlongGroupEntityMapper groupMapper;
    @Autowired
    protected InlongGroupExtEntityMapper groupExtMapper;
    @Autowired
    protected InlongStreamExtEntityMapper streamExtMapper;
    @Autowired
    protected InlongClusterEntityMapper clusterEntityMapper;
    @Autowired
    protected InlongClusterOperatorFactory clusterOperatorFactory;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public String saveOpt(InlongGroupRequest request, String operator) {
        String groupId = request.getInlongGroupId();
        InlongGroupEntity entity = CommonBeanUtils.copyProperties(request, InlongGroupEntity::new);
        // if the mqResource was empty, fill with inlongGroupId
        if (StringUtils.isEmpty(entity.getMqResource())) {
            entity.setMqResource(groupId);
        }
        // set the ext params
        setTargetEntity(request, entity);

        // after saving, the status is set to [GROUP_WAIT_SUBMIT]
        entity.setStatus(GroupStatus.TO_BE_SUBMIT.getCode());
        entity.setCreator(operator);
        entity.setModifier(operator);

        groupMapper.insert(entity);
        return groupId;
    }

    /**
     * Set the parameters of the target entity.
     *
     * @param request inlong group request
     * @param targetEntity entity which will set the new parameters
     */
    protected abstract void setTargetEntity(InlongGroupRequest request, InlongGroupEntity targetEntity);

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ)
    public void updateOpt(InlongGroupRequest request, String operator) {
        InlongGroupEntity entity = groupMapper.selectByGroupId(request.getInlongGroupId());
        // set the ext params
        setTargetEntity(request, entity);
        entity.setModifier(operator);
        int rowCount = groupMapper.updateByIdentifierSelective(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED,
                    String.format("record has already updated with group id=%s, curVersion=%d",
                            request.getInlongGroupId(), request.getVersion()));
        }
    }

    @Override
    public Map<String, Object> getDetailInfo(InlongGroupInfo groupInfo) {
        Map<String, Object> map = new HashMap<>(getClusterInfoByTag(groupInfo.getInlongClusterTag()));
        map.putIfAbsent(groupInfo.getMqType(), getMqInfo(groupInfo));
        return map;
    }

    @Override
    public Map<String, Object> getClusterInfoByTag(String clusterTag) {
        Map<String, Object> clusterMap = new HashMap<>();
        Set<String> mqClusters = Sets.newHashSet(ClusterType.PULSAR, ClusterType.TUBEMQ, ClusterType.KAFKA);
        List<InlongClusterEntity> clusterEntities = clusterEntityMapper.selectByClusterTag(clusterTag);
        for (InlongClusterEntity clusterEntity : clusterEntities) {
            if (mqClusters.contains(clusterEntity.getType())) {
                continue;
            }
            InlongClusterOperator instance = clusterOperatorFactory.getInstance(clusterEntity.getType());
            clusterMap.putIfAbsent(clusterEntity.getType(), instance.getClusterInfo(clusterEntity));
        }
        return clusterMap;
    }

    @Override
    public Object getMqInfo(InlongGroupInfo groupInfo) {
        InlongClusterOperator instance = clusterOperatorFactory.getInstance(groupInfo.getMqType());
        List<InlongClusterEntity> clusterEntities =
                clusterEntityMapper.selectByKey(groupInfo.getInlongClusterTag(), null, groupInfo.getMqType());
        if (CollectionUtils.isEmpty(clusterEntities)) {
            return null;
        }
        List<Object> mqClusterInfo = new ArrayList<>();
        for (InlongClusterEntity clusterEntity : clusterEntities) {
            mqClusterInfo.add(instance.getClusterInfo(clusterEntity));
        }
        return mqClusterInfo;
    }

}
