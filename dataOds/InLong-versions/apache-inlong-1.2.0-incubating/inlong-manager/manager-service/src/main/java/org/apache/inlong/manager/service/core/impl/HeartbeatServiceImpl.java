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

package org.apache.inlong.manager.service.core.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.enums.ComponentTypeEnum;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.heartbeat.ComponentHeartbeat;
import org.apache.inlong.manager.common.pojo.heartbeat.ComponentHeartbeatResponse;
import org.apache.inlong.manager.common.pojo.heartbeat.GroupHeartbeat;
import org.apache.inlong.manager.common.pojo.heartbeat.GroupHeartbeatResponse;
import org.apache.inlong.manager.common.pojo.heartbeat.HeartbeatPageRequest;
import org.apache.inlong.manager.common.pojo.heartbeat.HeartbeatQueryRequest;
import org.apache.inlong.manager.common.pojo.heartbeat.HeartbeatReportRequest;
import org.apache.inlong.manager.common.pojo.heartbeat.StreamHeartbeat;
import org.apache.inlong.manager.common.pojo.heartbeat.StreamHeartbeatResponse;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.ComponentHeartbeatEntity;
import org.apache.inlong.manager.dao.entity.GroupHeartbeatEntity;
import org.apache.inlong.manager.dao.entity.StreamHeartbeatEntity;
import org.apache.inlong.manager.dao.mapper.ComponentHeartbeatEntityMapper;
import org.apache.inlong.manager.dao.mapper.GroupHeartbeatEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamHeartbeatEntityMapper;
import org.apache.inlong.manager.service.core.HeartbeatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Heartbeat service layer implementation
 */
@Service
public class HeartbeatServiceImpl implements HeartbeatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatServiceImpl.class);
    private static final Gson GSON = new Gson();

    @Autowired
    private ComponentHeartbeatEntityMapper componentHeartbeatMapper;
    @Autowired
    private GroupHeartbeatEntityMapper groupHeartbeatMapper;
    @Autowired
    private StreamHeartbeatEntityMapper streamHeartbeatMapper;

    @Override
    public Boolean reportHeartbeat(HeartbeatReportRequest request) {
        if (request == null || StringUtils.isBlank(request.getComponent())) {
            LOGGER.warn("request is null or component null, just return");
            return false;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received heartbeat: " + request);
        }

        ComponentTypeEnum componentType = ComponentTypeEnum.valueOf(request.getComponent());
        switch (componentType) {
            case Sort:
            case DataProxy:
            case Agent:
            case Cache:
                return updateHeartbeatOpt(request);
            default:
                throw new BusinessException("Unsupported component type for " + request.getComponent());
        }
    }

    @Override
    public ComponentHeartbeatResponse getComponentHeartbeat(HeartbeatQueryRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        String component = request.getComponent();
        Preconditions.checkNotEmpty(component, ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInstance(), ErrorCodeEnum.REQUEST_INSTANCE_EMPTY.getMessage());

        ComponentTypeEnum componentType = ComponentTypeEnum.valueOf(component);
        switch (componentType) {
            case Sort:
            case DataProxy:
            case Agent:
            case Cache:
                ComponentHeartbeatEntity res = componentHeartbeatMapper.selectByKey(component, request.getInstance());
                return CommonBeanUtils.copyProperties(res, ComponentHeartbeatResponse::new);
            default:
                throw new BusinessException("Unsupported component type for " + component);
        }
    }

    @Override
    public GroupHeartbeatResponse getGroupHeartbeat(HeartbeatQueryRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        String component = request.getComponent();
        Preconditions.checkNotEmpty(component, ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInstance(), ErrorCodeEnum.REQUEST_INSTANCE_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInlongGroupId(), ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());

        ComponentTypeEnum componentType = ComponentTypeEnum.valueOf(component);
        switch (componentType) {
            case Sort:
            case DataProxy:
            case Agent:
            case Cache:
                GroupHeartbeatEntity result = groupHeartbeatMapper.selectByKey(component, request.getInstance(),
                        request.getInlongGroupId());
                return CommonBeanUtils.copyProperties(result, GroupHeartbeatResponse::new);
            default:
                throw new BusinessException("Unsupported component type for " + component);
        }
    }

    @Override
    public StreamHeartbeatResponse getStreamHeartbeat(HeartbeatQueryRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        String component = request.getComponent();
        Preconditions.checkNotEmpty(component, ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInstance(), ErrorCodeEnum.REQUEST_INSTANCE_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInlongGroupId(), ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInlongStreamId(), ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());

        ComponentTypeEnum componentType = ComponentTypeEnum.valueOf(component);
        switch (componentType) {
            case Sort:
            case DataProxy:
            case Agent:
            case Cache:
                StreamHeartbeatEntity result = streamHeartbeatMapper.selectByKey(component, request.getInstance(),
                        request.getInlongGroupId(), request.getInlongStreamId());
                return CommonBeanUtils.copyProperties(result, StreamHeartbeatResponse::new);
            default:
                throw new BusinessException("Unsupported component type for " + component);
        }
    }

    @Override
    public PageInfo<ComponentHeartbeatResponse> listComponentHeartbeat(HeartbeatPageRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        String component = request.getComponent();
        Preconditions.checkNotEmpty(component, ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());

        ComponentTypeEnum componentType = ComponentTypeEnum.valueOf(component);
        switch (componentType) {
            case Sort:
            case DataProxy:
            case Agent:
            case Cache:
                return listComponentHeartbeatOpt(request);
            default:
                throw new BusinessException("Unsupported component type for " + component);
        }
    }

    @Override
    public PageInfo<GroupHeartbeatResponse> listGroupHeartbeat(HeartbeatPageRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        String component = request.getComponent();
        Preconditions.checkNotEmpty(component, ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());

        ComponentTypeEnum componentType = ComponentTypeEnum.valueOf(component);
        switch (componentType) {
            case Sort:
            case DataProxy:
            case Agent:
            case Cache:
                return listGroupHeartbeatOpt(request);
            default:
                throw new BusinessException("Unsupported component type for " + component);
        }
    }

    @Override
    public PageInfo<StreamHeartbeatResponse> listStreamHeartbeat(HeartbeatPageRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        String component = request.getComponent();
        Preconditions.checkNotEmpty(component, ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInlongGroupId(), ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());

        ComponentTypeEnum componentType = ComponentTypeEnum.valueOf(component);
        switch (componentType) {
            case Sort:
            case DataProxy:
            case Agent:
            case Cache:
                return listStreamHeartbeatOpt(request);
            default:
                throw new BusinessException("Unsupported component type for " + component);
        }
    }

    /**
     * Default implementation for updating heartbeat
     */
    private Boolean updateHeartbeatOpt(HeartbeatReportRequest request) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("heartbeat request json = {}", GSON.toJson(request));
        }
        String component = request.getComponent();
        String instance = request.getInstance();
        Long reportTime = request.getReportTime();

        // Add component heartbeats
        ComponentHeartbeat componentHeartbeat = request.getComponentHeartbeat();
        if (componentHeartbeat != null) {
            ComponentHeartbeatEntity entity = new ComponentHeartbeatEntity();
            entity.setComponent(component);
            entity.setInstance(instance);
            entity.setReportTime(reportTime);
            entity.setStatusHeartbeat(componentHeartbeat.getStatusHeartbeat());
            entity.setMetricHeartbeat(componentHeartbeat.getMetricHeartbeat());
            componentHeartbeatMapper.insertOrUpdateByKey(entity);
        }

        // Add group heartbeats
        List<GroupHeartbeat> groupHeartbeats = request.getGroupHeartbeats();
        if (CollectionUtils.isNotEmpty(groupHeartbeats)) {
            groupHeartbeatMapper.insertOrUpdateAll(component, instance, reportTime, groupHeartbeats);
        }

        // Add stream heartbeats
        List<StreamHeartbeat> streamHeartbeats = request.getStreamHeartbeats();
        if (CollectionUtils.isNotEmpty(streamHeartbeats)) {
            streamHeartbeatMapper.insertOrUpdateAll(component, instance, reportTime, streamHeartbeats);
        }

        return true;
    }

    private PageInfo<ComponentHeartbeatResponse> listComponentHeartbeatOpt(HeartbeatPageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<ComponentHeartbeatEntity> entityPage = (Page<ComponentHeartbeatEntity>)
                componentHeartbeatMapper.selectByCondition(request);
        List<ComponentHeartbeatResponse> responseList = CommonBeanUtils.copyListProperties(entityPage,
                ComponentHeartbeatResponse::new);

        PageInfo<ComponentHeartbeatResponse> pageInfo = new PageInfo<>(responseList);
        pageInfo.setTotal(responseList.size());
        return pageInfo;
    }

    private PageInfo<GroupHeartbeatResponse> listGroupHeartbeatOpt(HeartbeatPageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<GroupHeartbeatEntity> entityPage = (Page<GroupHeartbeatEntity>) groupHeartbeatMapper.selectByCondition(
                request);
        List<GroupHeartbeatResponse> responseList = CommonBeanUtils.copyListProperties(entityPage,
                GroupHeartbeatResponse::new);

        PageInfo<GroupHeartbeatResponse> pageInfo = new PageInfo<>(responseList);
        pageInfo.setTotal(responseList.size());
        return pageInfo;
    }

    private PageInfo<StreamHeartbeatResponse> listStreamHeartbeatOpt(HeartbeatPageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<StreamHeartbeatEntity> entityPage = (Page<StreamHeartbeatEntity>)
                streamHeartbeatMapper.selectByCondition(request);
        List<StreamHeartbeatResponse> responseList = CommonBeanUtils.copyListProperties(entityPage,
                StreamHeartbeatResponse::new);

        PageInfo<StreamHeartbeatResponse> pageInfo = new PageInfo<>(responseList);
        pageInfo.setTotal(responseList.size());
        return pageInfo;
    }

}
