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

package org.apache.inlong.manager.service.sink;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GlobalConstants;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.SinkStatus;
import org.apache.inlong.manager.common.enums.SinkType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.sink.SinkApproveDTO;
import org.apache.inlong.manager.common.pojo.sink.SinkBriefResponse;
import org.apache.inlong.manager.common.pojo.sink.SinkField;
import org.apache.inlong.manager.common.pojo.sink.SinkListResponse;
import org.apache.inlong.manager.common.pojo.sink.SinkPageRequest;
import org.apache.inlong.manager.common.pojo.sink.SinkRequest;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.dao.mapper.StreamSinkEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSinkFieldEntityMapper;
import org.apache.inlong.manager.service.core.operation.InlongStreamProcessOperation;
import org.apache.inlong.manager.service.group.GroupCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of sink service interface
 */
@Service
public class StreamSinkServiceImpl implements StreamSinkService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamSinkServiceImpl.class);
    @Autowired
    private SinkOperationFactory operationFactory;
    @Autowired
    private GroupCheckService groupCheckService;
    @Autowired
    private StreamSinkEntityMapper sinkMapper;
    @Autowired
    private StreamSinkFieldEntityMapper sinkFieldMapper;
    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;
    private InlongStreamProcessOperation streamProcessOperation;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Integer save(SinkRequest request, String operator) {
        LOGGER.info("begin to save sink info: {}", request);
        this.checkParams(request);

        // Check if it can be added
        String groupId = request.getInlongGroupId();
        groupCheckService.checkGroupStatus(groupId, operator);

        // Make sure that there is no sink info with the current groupId and streamId
        String streamId = request.getInlongStreamId();
        String sinkType = request.getSinkType();
        String sinkName = request.getSinkName();
        List<StreamSinkEntity> sinkList = sinkMapper.selectByRelatedId(groupId, streamId, sinkName);
        for (StreamSinkEntity sinkEntity : sinkList) {
            if (sinkEntity != null && Objects.equals(sinkEntity.getSinkName(), sinkName)) {
                String err = "sink name=%s already exists with the groupId=%s streamId=%s";
                throw new BusinessException(String.format(err, sinkName, groupId, streamId));
            }
        }

        // According to the sink type, save sink information
        StreamSinkOperation operation = operationFactory.getInstance(SinkType.forType(sinkType));
        List<SinkField> fields = request.getSinkFieldList();
        // Remove id in sinkField when save
        if (CollectionUtils.isNotEmpty(fields)) {
            fields.stream().forEach(sinkField -> sinkField.setId(null));
        }
        int id = operation.saveOpt(request, operator);

        LOGGER.info("success to save sink info: {}", request);
        return id;
    }

    @Override
    public StreamSink get(Integer id) {
        Preconditions.checkNotNull(id, "sink id is empty");
        StreamSinkEntity entity = sinkMapper.selectByPrimaryKey(id);
        if (entity == null) {
            LOGGER.error("sink not found by id={}", id);
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_NOT_FOUND);
        }
        String sinkType = entity.getSinkType();
        StreamSinkOperation operation = operationFactory.getInstance(SinkType.forType(sinkType));
        StreamSink streamSink = operation.getByEntity(entity);
        LOGGER.debug("success to get sink info by id={}", id);
        return streamSink;
    }

    @Override
    public Integer getCount(String groupId, String streamId) {
        Integer count = sinkMapper.selectCount(groupId, streamId);
        LOGGER.debug("sink count={} with groupId={}, streamId={}", count, groupId, streamId);
        return count;
    }

    @Override
    public List<StreamSink> listSink(String groupId, String streamId) {
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        List<StreamSinkEntity> entityList = sinkMapper.selectByRelatedId(groupId, streamId, null);
        if (CollectionUtils.isEmpty(entityList)) {
            return Collections.emptyList();
        }
        List<StreamSink> responseList = new ArrayList<>();
        entityList.forEach(entity -> responseList.add(this.get(entity.getId())));

        LOGGER.debug("success to list sink by groupId={}, streamId={}", groupId, streamId);
        return responseList;
    }

    @Override
    public List<SinkBriefResponse> listBrief(String groupId, String streamId) {
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        Preconditions.checkNotNull(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());

        // Query all sink information and encapsulate it in the result set
        List<SinkBriefResponse> summaryList = sinkMapper.selectSummary(groupId, streamId);

        LOGGER.debug("success to list sink summary by groupId=" + groupId + ", streamId=" + streamId);
        return summaryList;
    }

    @Override
    public PageInfo<? extends SinkListResponse> listByCondition(SinkPageRequest request) {
        Preconditions.checkNotNull(request.getInlongGroupId(), ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());

        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<StreamSinkEntity> entityPage = sinkMapper.selectByCondition(request);
        Map<SinkType, Page<StreamSinkEntity>> sinkMap = Maps.newHashMap();
        for (StreamSinkEntity streamSink : entityPage) {
            SinkType sinkType = SinkType.forType(streamSink.getSinkType());
            sinkMap.computeIfAbsent(sinkType, k -> new Page<>()).add(streamSink);
        }
        List<SinkListResponse> sinkListResponses = Lists.newArrayList();
        for (Map.Entry<SinkType, Page<StreamSinkEntity>> entry : sinkMap.entrySet()) {
            SinkType sinkType = entry.getKey();
            StreamSinkOperation operation = operationFactory.getInstance(sinkType);
            PageInfo<? extends SinkListResponse> pageInfo = operation.getPageInfo(entry.getValue());
            sinkListResponses.addAll(pageInfo.getList());
        }
        // Encapsulate the paging query results into the PageInfo object to obtain related paging information
        PageInfo<? extends SinkListResponse> pageInfo = PageInfo.of(sinkListResponses);

        LOGGER.debug("success to list sink page, result size {}", pageInfo.getSize());
        return pageInfo;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean update(SinkRequest request, String operator) {
        LOGGER.info("begin to update sink info: {}", request);
        this.checkParams(request);
        Preconditions.checkNotNull(request.getId(), ErrorCodeEnum.ID_IS_EMPTY.getMessage());

        // Check if it can be modified
        String groupId = request.getInlongGroupId();
        String streamId = request.getInlongStreamId();
        String sinkName = request.getSinkName();
        String sinkType = request.getSinkType();
        final InlongGroupEntity groupEntity = groupCheckService.checkGroupStatus(groupId, operator);

        // Check whether the sink name exists with the same groupId and streamId
        List<StreamSinkEntity> sinkList = sinkMapper.selectByRelatedId(groupId, streamId, sinkName);
        for (StreamSinkEntity entity : sinkList) {
            Integer sinkId = entity.getId();
            if (!Objects.equals(request.getId(), sinkId) && Objects.equals(entity.getSinkName(), sinkName)) {
                String err = "sink name=%s already exists with the groupId=%s streamId=%s";
                throw new BusinessException(String.format(err, sinkName, groupId, streamId));
            }
        }
        List<SinkField> fields = request.getSinkFieldList();
        // Remove id in sinkField when save
        if (CollectionUtils.isNotEmpty(fields)) {
            fields.stream().forEach(sinkField -> sinkField.setId(null));
        }

        StreamSinkOperation operation = operationFactory.getInstance(SinkType.forType(sinkType));
        operation.updateOpt(request, operator);

        // The inlong group status is [Configuration successful], then asynchronously initiate
        // the [Single inlong stream resource creation] workflow
        if (GroupStatus.CONFIG_SUCCESSFUL.getCode().equals(groupEntity.getStatus())) {
            // To work around the circular reference check we manually instantiate and wire
            if (streamProcessOperation == null) {
                streamProcessOperation = new InlongStreamProcessOperation();
                autowireCapableBeanFactory.autowireBean(streamProcessOperation);
            }
            streamProcessOperation.startProcess(groupId, streamId, operator, true);
        }
        LOGGER.info("success to update sink info: {}", request);
        return true;
    }

    @Override
    public void updateStatus(int id, int status, String log) {
        StreamSinkEntity entity = new StreamSinkEntity();
        entity.setId(id);
        entity.setStatus(status);
        entity.setOperateLog(log);
        sinkMapper.updateStatus(entity);

        LOGGER.info("success to update sink status={} for id={} with log: {}", status, id, log);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public Boolean delete(Integer id, String operator) {
        LOGGER.info("begin to delete sink by id={}", id);
        Preconditions.checkNotNull(id, ErrorCodeEnum.ID_IS_EMPTY.getMessage());
        StreamSinkEntity entity = sinkMapper.selectByPrimaryKey(id);
        Preconditions.checkNotNull(entity, ErrorCodeEnum.SINK_INFO_NOT_FOUND.getMessage());
        groupCheckService.checkGroupStatus(entity.getInlongGroupId(), operator);

        entity.setPreviousStatus(entity.getStatus());
        entity.setStatus(GlobalConstants.DELETED_STATUS);
        entity.setIsDeleted(id);
        entity.setModifier(operator);
        entity.setModifyTime(new Date());
        sinkMapper.updateByPrimaryKeySelective(entity);
        sinkFieldMapper.logicDeleteAll(id);

        LOGGER.info("success to delete sink info: {}", entity);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean logicDeleteAll(String groupId, String streamId, String operator) {
        LOGGER.info("begin to logic delete all sink info by groupId={}, streamId={}", groupId, streamId);
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        Preconditions.checkNotNull(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());

        // Check if it can be deleted
        groupCheckService.checkGroupStatus(groupId, operator);

        Date now = new Date();
        List<StreamSinkEntity> entityList = sinkMapper.selectByRelatedId(groupId, streamId, null);
        if (CollectionUtils.isNotEmpty(entityList)) {
            entityList.forEach(entity -> {
                Integer id = entity.getId();
                entity.setPreviousStatus(entity.getStatus());
                entity.setStatus(GlobalConstants.DELETED_STATUS);
                entity.setIsDeleted(id);
                entity.setModifier(operator);
                entity.setModifyTime(now);
                sinkMapper.updateByPrimaryKeySelective(entity);
                sinkFieldMapper.logicDeleteAll(id);
            });
        }

        LOGGER.info("success to logic delete all sink by groupId={}, streamId={}", groupId, streamId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean deleteAll(String groupId, String streamId, String operator) {
        LOGGER.info("begin to delete all sink by groupId={}, streamId={}", groupId, streamId);
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        Preconditions.checkNotNull(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());

        // Check if it can be deleted
        groupCheckService.checkGroupStatus(groupId, operator);

        List<StreamSinkEntity> entityList = sinkMapper.selectByRelatedId(groupId, streamId, null);
        if (CollectionUtils.isNotEmpty(entityList)) {
            entityList.forEach(entity -> {
                sinkMapper.deleteByPrimaryKey(entity.getId());
                sinkFieldMapper.deleteAll(entity.getId());
            });
        }

        LOGGER.info("success to delete all sink by groupId={}, streamId={}", groupId, streamId);
        return true;
    }

    @Override
    public List<String> getExistsStreamIdList(String groupId, String sinkType, List<String> streamIdList) {
        LOGGER.debug("begin to filter stream by groupId={}, type={}, streamId={}", groupId, sinkType, streamIdList);
        if (StringUtils.isEmpty(sinkType) || CollectionUtils.isEmpty(streamIdList)) {
            return Collections.emptyList();
        }

        List<String> resultList = sinkMapper.selectExistsStreamId(groupId, sinkType, streamIdList);
        LOGGER.debug("success to filter stream id list, result streamId={}", resultList);
        return resultList;
    }

    @Override
    public List<String> getSinkTypeList(String groupId, String streamId) {
        if (StringUtils.isEmpty(streamId)) {
            return Collections.emptyList();
        }

        List<String> resultList = sinkMapper.selectSinkType(groupId, streamId);
        LOGGER.debug("success to get sink type by groupId={}, streamId={}, result={}", groupId, streamId, resultList);
        return resultList;
    }

    @Override
    public Boolean updateAfterApprove(List<SinkApproveDTO> approveList, String operator) {
        LOGGER.info("begin to update sink after approve: {}", approveList);
        if (CollectionUtils.isEmpty(approveList)) {
            return true;
        }

        Date now = new Date();
        for (SinkApproveDTO dto : approveList) {
            // According to the sink type, save sink information
            String sinkType = dto.getSinkType();
            Preconditions.checkNotNull(sinkType, ErrorCodeEnum.SINK_TYPE_IS_NULL.getMessage());

            StreamSinkEntity entity = new StreamSinkEntity();
            entity.setId(dto.getId());

            int status = (dto.getStatus() == null) ? SinkStatus.CONFIG_ING.getCode() : dto.getStatus();
            entity.setPreviousStatus(entity.getStatus());
            entity.setStatus(status);
            entity.setModifier(operator);
            entity.setModifyTime(now);
            sinkMapper.updateByPrimaryKeySelective(entity);
        }

        LOGGER.info("success to update sink after approve: {}", approveList);
        return true;
    }

    private void checkParams(SinkRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        String groupId = request.getInlongGroupId();
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        String streamId = request.getInlongStreamId();
        Preconditions.checkNotNull(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());
        String sinkType = request.getSinkType();
        Preconditions.checkNotNull(sinkType, ErrorCodeEnum.SINK_TYPE_IS_NULL.getMessage());
        String sinkName = request.getSinkName();
        Preconditions.checkNotNull(sinkName, ErrorCodeEnum.SINK_NAME_IS_NULL.getMessage());
    }
}
