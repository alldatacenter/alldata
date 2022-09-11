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
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamConfigLogListResponse;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamConfigLogPageRequest;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamConfigLogRequest;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.StreamConfigLogEntity;
import org.apache.inlong.manager.dao.mapper.StreamConfigLogEntityMapper;
import org.apache.inlong.manager.service.core.StreamConfigLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Stream config log service layer implementation
 */
@Service
public class StreamConfigLogServiceImpl extends AbstractService<StreamConfigLogEntity>
        implements StreamConfigLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamConfigLogServiceImpl.class);

    @Autowired
    private StreamConfigLogEntityMapper streamConfigLogEntityMapper;

    @Override
    public String reportConfigLog(InlongStreamConfigLogRequest request) {
        if (putData(convertData(request))) {
            return "Receive success";
        } else {
            LOGGER.warn("Receive Queue is full, data will be discarded !");
            return "Receive Queue is full, data will be discarded !";
        }
    }

    @Override
    public PageInfo<InlongStreamConfigLogListResponse> listByCondition(
            InlongStreamConfigLogPageRequest request) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("begin to list source page by " + request);
        }
        Preconditions.checkNotNull(request.getInlongGroupId(), ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        PageHelper.startPage(request.getPageNum(), request.getPageSize());

        if (request.getReportTime() == null) {
            Instant instant = Instant.now().minus(Duration.ofMillis(5));
            request.setReportTime(new Date(instant.toEpochMilli()));
        }
        Page<StreamConfigLogEntity> entityPage = (Page<StreamConfigLogEntity>)
                streamConfigLogEntityMapper.selectByCondition(request);
        List<InlongStreamConfigLogListResponse> detailList = CommonBeanUtils
                .copyListProperties(entityPage, InlongStreamConfigLogListResponse::new);

        PageInfo<InlongStreamConfigLogListResponse> pageInfo = new PageInfo<>(detailList);
        pageInfo.setTotal(entityPage.getTotal());
        return pageInfo;
    }

    /**
     * Batch insert stream config log.
     *
     * @param entryList stream config log list.
     * @return whether succeed.
     */
    public boolean batchInsertEntities(List<StreamConfigLogEntity> entryList) {
        streamConfigLogEntityMapper.insertOrUpdateAll(entryList);
        return true;
    }

    /**
     * Convert request to stream config log info.
     *
     * @param request stream config log list.
     * @return the updated stream config log info.
     */
    public StreamConfigLogEntity convertData(InlongStreamConfigLogRequest request) {
        StreamConfigLogEntity entity = new StreamConfigLogEntity();
        entity.setComponentName(request.getComponentName());
        entity.setInlongGroupId(request.getInlongGroupId());
        entity.setInlongStreamId(request.getInlongStreamId());
        entity.setConfigName(request.getConfigName());
        entity.setReportTime(new Date(request.getReportTime()));
        entity.setVersion(request.getVersion());
        entity.setLogInfo(request.getLogInfo());
        entity.setLogType(request.getLogType());
        entity.setIp(request.getIp());
        return entity;
    }
}
