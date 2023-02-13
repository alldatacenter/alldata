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

package org.apache.inlong.manager.service.heartbeat;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.enums.ComponentTypeEnum;
import org.apache.inlong.common.heartbeat.GroupHeartbeat;
import org.apache.inlong.common.heartbeat.StreamHeartbeat;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.ComponentHeartbeatEntity;
import org.apache.inlong.manager.dao.entity.GroupHeartbeatEntity;
import org.apache.inlong.manager.dao.entity.StreamHeartbeatEntity;
import org.apache.inlong.manager.dao.mapper.ComponentHeartbeatEntityMapper;
import org.apache.inlong.manager.dao.mapper.GroupHeartbeatEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamHeartbeatEntityMapper;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.heartbeat.ComponentHeartbeatResponse;
import org.apache.inlong.manager.pojo.heartbeat.GroupHeartbeatResponse;
import org.apache.inlong.manager.pojo.heartbeat.HeartbeatPageRequest;
import org.apache.inlong.manager.pojo.heartbeat.HeartbeatQueryRequest;
import org.apache.inlong.manager.pojo.heartbeat.HeartbeatReportRequest;
import org.apache.inlong.manager.pojo.heartbeat.StreamHeartbeatResponse;
import org.apache.inlong.manager.service.core.HeartbeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Heartbeat service layer implementation
 */
@Service
@Slf4j
public class HeartbeatServiceImpl implements HeartbeatService {

    private static final Gson GSON = new Gson();

    @Autowired
    private HeartbeatManager heartbeatManager;
    @Autowired
    private ComponentHeartbeatEntityMapper componentHeartbeatMapper;
    @Autowired
    private GroupHeartbeatEntityMapper groupHeartbeatMapper;
    @Autowired
    private StreamHeartbeatEntityMapper streamHeartbeatMapper;

    @Override
    public Boolean reportHeartbeat(HeartbeatReportRequest request) {
        if (request == null || StringUtils.isBlank(request.getComponentType())) {
            log.warn("request is null or component null, just return");
            return false;
        }
        if (log.isDebugEnabled()) {
            log.debug("received heartbeat: " + request);
        }
        heartbeatManager.reportHeartbeat(request);
        ComponentTypeEnum componentType = ComponentTypeEnum.forType(request.getComponentType());
        switch (componentType) {
            case Sort:
            case DataProxy:
            case Agent:
            case Cache:
            case SDK:
                return updateHeartbeatOpt(request);
            default:
                log.error("Unsupported componentType={} for Inlong", componentType);
                return false;
        }
    }

    @Override
    public ComponentHeartbeatResponse getComponentHeartbeat(HeartbeatQueryRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        String component = request.getComponent();
        Preconditions.checkNotEmpty(component, ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInstance(), ErrorCodeEnum.REQUEST_INSTANCE_EMPTY.getMessage());

        ComponentTypeEnum componentType = ComponentTypeEnum.forType(component);
        switch (componentType) {
            case Sort:
            case DataProxy:
            case Agent:
            case Cache:
            case SDK:
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

        ComponentTypeEnum componentType = ComponentTypeEnum.forType(component);
        switch (componentType) {
            case Sort:
            case DataProxy:
            case Agent:
            case Cache:
            case SDK:
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

        ComponentTypeEnum componentType = ComponentTypeEnum.forType(component);
        switch (componentType) {
            case Sort:
            case DataProxy:
            case Agent:
            case Cache:
            case SDK:
                StreamHeartbeatEntity result = streamHeartbeatMapper.selectByKey(component, request.getInstance(),
                        request.getInlongGroupId(), request.getInlongStreamId());
                return CommonBeanUtils.copyProperties(result, StreamHeartbeatResponse::new);
            default:
                throw new BusinessException("Unsupported component type for " + component);
        }
    }

    @Override
    public PageResult<ComponentHeartbeatResponse> listComponentHeartbeat(HeartbeatPageRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        String component = request.getComponent();
        Preconditions.checkNotEmpty(component, ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());

        ComponentTypeEnum componentType = ComponentTypeEnum.forType(component);
        switch (componentType) {
            case Sort:
            case DataProxy:
            case Agent:
            case Cache:
            case SDK:
                return listComponentHeartbeatOpt(request);
            default:
                throw new BusinessException("Unsupported component type for " + component);
        }
    }

    @Override
    public PageResult<GroupHeartbeatResponse> listGroupHeartbeat(HeartbeatPageRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        String component = request.getComponent();
        Preconditions.checkNotEmpty(component, ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());

        ComponentTypeEnum componentType = ComponentTypeEnum.forType(component);
        switch (componentType) {
            case Sort:
            case DataProxy:
            case Agent:
            case Cache:
            case SDK:
                return listGroupHeartbeatOpt(request);
            default:
                throw new BusinessException("Unsupported component type for " + component);
        }
    }

    @Override
    public PageResult<StreamHeartbeatResponse> listStreamHeartbeat(HeartbeatPageRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        String component = request.getComponent();
        Preconditions.checkNotEmpty(component, ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInlongGroupId(), ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());

        ComponentTypeEnum componentType = ComponentTypeEnum.forType(component);
        switch (componentType) {
            case Sort:
            case DataProxy:
            case Agent:
            case Cache:
            case SDK:
                return listStreamHeartbeatOpt(request);
            default:
                throw new BusinessException("Unsupported component type for " + component);
        }
    }

    /**
     * Default implementation for updating heartbeat
     */
    private Boolean updateHeartbeatOpt(HeartbeatReportRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("heartbeat request json = {}", GSON.toJson(request));
        }
        String component = request.getComponentType();
        String instanceIp = request.getIp();
        Long reportTime = request.getReportTime();

        // Add component heartbeats
        ComponentHeartbeatEntity entity = new ComponentHeartbeatEntity();
        entity.setComponent(component);
        entity.setInstance(instanceIp);
        entity.setReportTime(reportTime);
        componentHeartbeatMapper.insertOrUpdateByKey(entity);

        // Add group heartbeats
        List<GroupHeartbeat> groupHeartbeats = request.getGroupHeartbeats();
        if (CollectionUtils.isNotEmpty(groupHeartbeats)) {
            groupHeartbeatMapper.insertOrUpdateAll(component, instanceIp, reportTime, groupHeartbeats);
        }

        // Add stream heartbeats
        List<StreamHeartbeat> streamHeartbeats = request.getStreamHeartbeats();
        if (CollectionUtils.isNotEmpty(streamHeartbeats)) {
            streamHeartbeatMapper.insertOrUpdateAll(component, instanceIp, reportTime, streamHeartbeats);
        }

        return true;
    }

    private PageResult<ComponentHeartbeatResponse> listComponentHeartbeatOpt(HeartbeatPageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<ComponentHeartbeatEntity> entityPage =
                (Page<ComponentHeartbeatEntity>) componentHeartbeatMapper.selectByCondition(request);
        List<ComponentHeartbeatResponse> responseList = CommonBeanUtils.copyListProperties(entityPage,
                ComponentHeartbeatResponse::new);

        return new PageResult<>(responseList, entityPage.getTotal());
    }

    private PageResult<GroupHeartbeatResponse> listGroupHeartbeatOpt(HeartbeatPageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<GroupHeartbeatEntity> entityPage = (Page<GroupHeartbeatEntity>) groupHeartbeatMapper.selectByCondition(
                request);
        List<GroupHeartbeatResponse> responseList = CommonBeanUtils.copyListProperties(entityPage,
                GroupHeartbeatResponse::new);

        return new PageResult<>(responseList,
                entityPage.getTotal(), entityPage.getPageNum(), entityPage.getPageSize());
    }

    private PageResult<StreamHeartbeatResponse> listStreamHeartbeatOpt(HeartbeatPageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<StreamHeartbeatEntity> entityPage =
                (Page<StreamHeartbeatEntity>) streamHeartbeatMapper.selectByCondition(request);
        List<StreamHeartbeatResponse> responseList = CommonBeanUtils.copyListProperties(entityPage,
                StreamHeartbeatResponse::new);

        return new PageResult<>(responseList, entityPage.getTotal(), entityPage.getPageNum(), entityPage.getPageSize());
    }

}
