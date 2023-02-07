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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.SinkStatus;
import org.apache.inlong.manager.common.enums.StreamStatus;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.InlongStreamEntity;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSinkEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSinkFieldEntityMapper;
import org.apache.inlong.manager.pojo.common.OrderFieldEnum;
import org.apache.inlong.manager.pojo.common.OrderTypeEnum;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.UpdateResult;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.sink.SinkApproveDTO;
import org.apache.inlong.manager.pojo.sink.SinkBriefInfo;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.SinkPageRequest;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.service.group.GroupCheckService;
import org.apache.inlong.manager.service.stream.InlongStreamProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of sink service interface
 */
@Service
public class StreamSinkServiceImpl implements StreamSinkService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamSinkServiceImpl.class);

    @Autowired
    private SinkOperatorFactory operatorFactory;
    @Autowired
    private GroupCheckService groupCheckService;
    @Autowired
    private InlongStreamEntityMapper streamMapper;
    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private StreamSinkEntityMapper sinkMapper;
    @Autowired
    private StreamSinkFieldEntityMapper sinkFieldMapper;
    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    // To avoid circular dependencies, you cannot use @Autowired, it will be injected by AutowireCapableBeanFactory
    private InlongStreamProcessService streamProcessOperation;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Integer save(SinkRequest request, String operator) {
        LOGGER.info("begin to save sink info: {}", request);
        this.checkParams(request);

        // Check if it can be added
        String groupId = request.getInlongGroupId();
        groupCheckService.checkGroupStatus(groupId, operator);

        // Make sure that there is no same sink name under the current groupId and streamId
        String streamId = request.getInlongStreamId();
        String sinkName = request.getSinkName();
        // Check whether the stream exist or not
        InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(groupId, streamId);
        Preconditions.checkNotNull(streamEntity, ErrorCodeEnum.STREAM_NOT_FOUND.getMessage());

        // Check whether the sink name exists with the same groupId and streamId
        StreamSinkEntity exists = sinkMapper.selectByUniqueKey(groupId, streamId, sinkName);
        if (exists != null && exists.getSinkName().equals(sinkName)) {
            String err = "sink name=%s already exists with the groupId=%s streamId=%s";
            throw new BusinessException(String.format(err, sinkName, groupId, streamId));
        }

        // According to the sink type, save sink information
        StreamSinkOperator sinkOperator = operatorFactory.getInstance(request.getSinkType());
        List<SinkField> fields = request.getSinkFieldList();
        // Remove id in sinkField when save
        if (CollectionUtils.isNotEmpty(fields)) {
            fields.forEach(sinkField -> sinkField.setId(null));
        }
        int id = sinkOperator.saveOpt(request, operator);
        boolean streamSuccess = StreamStatus.CONFIG_SUCCESSFUL.getCode().equals(streamEntity.getStatus());
        if (streamSuccess || StreamStatus.CONFIG_FAILED.getCode().equals(streamEntity.getStatus())) {
            boolean enableCreateResource = InlongConstants.ENABLE_CREATE_RESOURCE.equals(
                    request.getEnableCreateResource());
            SinkStatus nextStatus = enableCreateResource ? SinkStatus.CONFIG_ING : SinkStatus.CONFIG_SUCCESSFUL;
            StreamSinkEntity sinkEntity = sinkMapper.selectByPrimaryKey(id);
            sinkEntity.setStatus(nextStatus.getCode());
            sinkMapper.updateStatus(sinkEntity);
        }

        // If the stream is [CONFIG_SUCCESSFUL], then asynchronously start the [CREATE_STREAM_RESOURCE] process
        if (streamSuccess && request.getStartProcess()) {
            this.startProcessForSink(groupId, streamId, operator);
        }

        LOGGER.info("success to save sink info: {}", request);
        return id;
    }
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Integer save(SinkRequest request, UserInfo opInfo) {
        // check request parameter
        checkSinkRequestParams(request);
        // check operator info
        if (opInfo == null) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_USER_EMPTY);
        }
        InlongGroupEntity entity = groupMapper.selectByGroupId(request.getInlongGroupId());
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND,
                    String.format("InlongGroup does not exist with InlongGroupId=%s", request.getInlongGroupId()));
        }
        // only the person in charges can query
        if (!opInfo.getRoles().contains(UserTypeEnum.ADMIN.name())) {
            List<String> inCharges = Arrays.asList(entity.getInCharges().split(InlongConstants.COMMA));
            if (!inCharges.contains(opInfo.getName())) {
                throw new BusinessException(ErrorCodeEnum.GROUP_PERMISSION_DENIED);
            }
        }
        // check group status
        GroupStatus curState = GroupStatus.forCode(entity.getStatus());
        if (GroupStatus.notAllowedUpdate(curState)) {
            throw new BusinessException(String.format(ErrorCodeEnum.OPT_NOT_ALLOWED_BY_STATUS.getMessage(), curState));
        }
        // Check whether the stream exist or not
        InlongStreamEntity streamEntity =
                streamMapper.selectByIdentifier(request.getInlongGroupId(), request.getInlongStreamId());
        if (streamEntity == null) {
            throw new BusinessException(ErrorCodeEnum.STREAM_NOT_FOUND);
        }
        // Check whether the sink name exists with the same groupId and streamId
        StreamSinkEntity exists = sinkMapper.selectByUniqueKey(
                request.getInlongGroupId(), request.getInlongStreamId(), request.getSinkName());
        if (exists != null && exists.getSinkName().equals(request.getSinkName())) {
            throw new BusinessException(ErrorCodeEnum.RECORD_DUPLICATE,
                    String.format("sink name=%s already exists with the groupId=%s streamId=%s",
                            request.getSinkName(), request.getInlongGroupId(), request.getInlongStreamId()));
        }
        // According to the sink type, save sink information
        StreamSinkOperator sinkOperator = operatorFactory.getInstance(request.getSinkType());
        List<SinkField> fields = request.getSinkFieldList();
        // Remove id in sinkField when save
        if (CollectionUtils.isNotEmpty(fields)) {
            fields.forEach(sinkField -> sinkField.setId(null));
        }
        int id = sinkOperator.saveOpt(request, opInfo.getName());
        boolean streamSuccess = StreamStatus.CONFIG_SUCCESSFUL.getCode().equals(streamEntity.getStatus());
        if (streamSuccess || StreamStatus.CONFIG_FAILED.getCode().equals(streamEntity.getStatus())) {
            boolean enableCreateResource = InlongConstants.ENABLE_CREATE_RESOURCE.equals(
                    request.getEnableCreateResource());
            SinkStatus nextStatus = enableCreateResource ? SinkStatus.CONFIG_ING : SinkStatus.CONFIG_SUCCESSFUL;
            StreamSinkEntity sinkEntity = sinkMapper.selectByPrimaryKey(id);
            sinkEntity.setStatus(nextStatus.getCode());
            sinkMapper.updateStatus(sinkEntity);
        }
        // If the stream is [CONFIG_SUCCESSFUL], then asynchronously start the [CREATE_STREAM_RESOURCE] process
        if (streamSuccess && request.getStartProcess()) {
            this.startProcessForSink(request.getInlongGroupId(), request.getInlongStreamId(), opInfo.getName());
        }
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
        StreamSinkOperator sinkOperator = operatorFactory.getInstance(entity.getSinkType());
        StreamSink streamSink = sinkOperator.getFromEntity(entity);
        LOGGER.debug("success to get sink info by id={}", id);
        return streamSink;
    }

    @Override
    public StreamSink get(Integer id, UserInfo opInfo) {
        // check operator info
        if (opInfo == null) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_USER_EMPTY);
        }
        // check sink id
        if (id == null) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "sink id is empty");
        }
        StreamSinkEntity entity = sinkMapper.selectByPrimaryKey(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_NOT_FOUND);
        }
        InlongGroupEntity groupEntity =
                groupMapper.selectByGroupId(entity.getInlongGroupId());
        if (groupEntity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }
        // only the person in charges can query
        if (!opInfo.getRoles().contains(UserTypeEnum.ADMIN.name())) {
            List<String> inCharges = Arrays.asList(groupEntity.getInCharges().split(InlongConstants.COMMA));
            if (!inCharges.contains(opInfo.getName())) {
                throw new BusinessException(ErrorCodeEnum.GROUP_PERMISSION_DENIED);
            }
        }
        StreamSinkOperator sinkOperator = operatorFactory.getInstance(entity.getSinkType());
        return sinkOperator.getFromEntity(entity);
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
        List<StreamSinkEntity> entityList = sinkMapper.selectByRelatedId(groupId, streamId);
        if (CollectionUtils.isEmpty(entityList)) {
            return Collections.emptyList();
        }
        List<StreamSink> responseList = new ArrayList<>();
        entityList.forEach(entity -> responseList.add(this.get(entity.getId())));

        LOGGER.debug("success to list sink by groupId={}, streamId={}", groupId, streamId);
        return responseList;
    }

    @Override
    public List<SinkBriefInfo> listBrief(String groupId, String streamId) {
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        Preconditions.checkNotNull(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());

        List<SinkBriefInfo> summaryList = sinkMapper.selectSummary(groupId, streamId);
        LOGGER.debug("success to list sink summary by groupId=" + groupId + ", streamId=" + streamId);
        return summaryList;
    }

    @Override
    public Map<String, List<StreamSink>> getSinksMap(InlongGroupInfo groupInfo, List<InlongStreamInfo> streamInfos) {
        String groupId = groupInfo.getInlongGroupId();
        LOGGER.debug("begin to get sink map for groupId={}", groupId);

        List<StreamSink> streamSinks = this.listSink(groupId, null);
        Map<String, List<StreamSink>> result = streamSinks.stream()
                .collect(Collectors.groupingBy(StreamSink::getInlongStreamId, HashMap::new,
                        Collectors.toCollection(ArrayList::new)));

        LOGGER.debug("success to get sink map, size={}, groupInfo={}", result.size(), groupInfo);
        return result;
    }

    @Override
    public PageResult<? extends StreamSink> listByCondition(SinkPageRequest request) {
        Preconditions.checkNotNull(request.getInlongGroupId(), ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());

        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        OrderFieldEnum.checkOrderField(request);
        OrderTypeEnum.checkOrderType(request);
        List<StreamSinkEntity> entityPage = sinkMapper.selectByCondition(request);
        Map<String, Page<StreamSinkEntity>> sinkMap = Maps.newHashMap();
        for (StreamSinkEntity streamSink : entityPage) {
            sinkMap.computeIfAbsent(streamSink.getSinkType(), k -> new Page<>()).add(streamSink);
        }
        List<StreamSink> responseList = Lists.newArrayList();
        for (Map.Entry<String, Page<StreamSinkEntity>> entry : sinkMap.entrySet()) {
            StreamSinkOperator sinkOperator = operatorFactory.getInstance(entry.getKey());
            PageResult<? extends StreamSink> pageInfo = sinkOperator.getPageInfo(entry.getValue());
            responseList.addAll(pageInfo.getList());
        }
        // Encapsulate the paging query results into the PageInfo object to obtain related paging information
        PageResult<StreamSink> pageResult = new PageResult<>(responseList);

        LOGGER.debug("success to list sink page, result size {}", pageResult.getList().size());
        return pageResult;
    }

    @Override
    public List<? extends StreamSink> listByCondition(SinkPageRequest request, UserInfo opInfo) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "group query request cannot be empty");
        }
        // check sink id
        if (StringUtils.isBlank(request.getInlongGroupId())) {
            throw new BusinessException(ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        }
        // check opInfo
        if (opInfo == null) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_USER_EMPTY);
        }
        // query result
        List<StreamSinkEntity> sinkEntityList = sinkMapper.selectByCondition(request);
        Map<String, Page<StreamSinkEntity>> sinkMap = Maps.newHashMap();
        for (StreamSinkEntity streamSink : sinkEntityList) {
            sinkMap.computeIfAbsent(streamSink.getSinkType(), k -> new Page<>()).add(streamSink);
        }
        List<StreamSink> filterResult = Lists.newArrayList();
        for (Map.Entry<String, Page<StreamSinkEntity>> entry : sinkMap.entrySet()) {
            StreamSinkOperator sinkOperator = operatorFactory.getInstance(entry.getKey());
            PageResult<? extends StreamSink> pageInfo = sinkOperator.getPageInfo(entry.getValue());
            for (StreamSink streamSink : pageInfo.getList()) {
                InlongGroupEntity groupEntity =
                        groupMapper.selectByGroupId(streamSink.getInlongGroupId());
                if (groupEntity == null) {
                    continue;
                }
                // only the person in charges can query
                if (!opInfo.getRoles().contains(UserTypeEnum.ADMIN.name())) {
                    List<String> inCharges = Arrays.asList(groupEntity.getInCharges().split(InlongConstants.COMMA));
                    if (!inCharges.contains(opInfo.getName())) {
                        continue;
                    }
                }
                filterResult.add(streamSink);
            }
        }
        return filterResult;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean update(SinkRequest request, String operator) {
        LOGGER.info("begin to update sink by id: {}", request);
        this.checkParams(request);
        Preconditions.checkNotNull(request.getId(), ErrorCodeEnum.ID_IS_EMPTY.getMessage());

        // Check if it can be modified
        String groupId = request.getInlongGroupId();
        String streamId = request.getInlongStreamId();
        String sinkName = request.getSinkName();
        groupCheckService.checkGroupStatus(groupId, operator);

        // Check whether the stream exist or not
        InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(groupId, streamId);
        Preconditions.checkNotNull(streamEntity, ErrorCodeEnum.STREAM_NOT_FOUND.getMessage());

        // Check whether the sink name exists with the same groupId and streamId
        StreamSinkEntity existEntity = sinkMapper.selectByUniqueKey(groupId, streamId, sinkName);
        if (existEntity != null && !existEntity.getId().equals(request.getId())) {
            String errMsg = "sink name=%s already exists with the groupId=%s streamId=%s";
            throw new BusinessException(String.format(errMsg, sinkName, groupId, streamId));
        }

        SinkStatus nextStatus = null;
        boolean streamSuccess = StreamStatus.CONFIG_SUCCESSFUL.getCode().equals(streamEntity.getStatus());
        if (streamSuccess || StreamStatus.CONFIG_FAILED.getCode().equals(streamEntity.getStatus())) {
            boolean enableCreateResource = InlongConstants.ENABLE_CREATE_RESOURCE.equals(
                    request.getEnableCreateResource());
            nextStatus = enableCreateResource ? SinkStatus.CONFIG_ING : SinkStatus.CONFIG_SUCCESSFUL;
        }
        StreamSinkOperator sinkOperator = operatorFactory.getInstance(request.getSinkType());
        sinkOperator.updateOpt(request, nextStatus, operator);

        // If the stream is [CONFIG_SUCCESSFUL], then asynchronously start the [CREATE_STREAM_RESOURCE] process
        if (streamSuccess && request.getStartProcess()) {
            this.startProcessForSink(groupId, streamId, operator);
        }

        LOGGER.info("success to update sink by id: {}", request);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean update(SinkRequest request, UserInfo opInfo) {
        // check request parameter
        checkSinkRequestParams(request);
        if (request.getId() == null) {
            throw new BusinessException(ErrorCodeEnum.ID_IS_EMPTY);
        }
        // check opInfo
        if (opInfo == null) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_USER_EMPTY);
        }
        // check group record
        InlongGroupEntity entity = groupMapper.selectByGroupId(request.getInlongGroupId());
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND,
                    String.format("InlongGroup does not exist with InlongGroupId=%s", request.getInlongGroupId()));
        }
        // only the person in charges can query
        if (!opInfo.getRoles().contains(UserTypeEnum.ADMIN.name())) {
            List<String> inCharges = Arrays.asList(entity.getInCharges().split(InlongConstants.COMMA));
            if (!inCharges.contains(opInfo.getName())) {
                throw new BusinessException(ErrorCodeEnum.GROUP_PERMISSION_DENIED);
            }
        }
        // Check if group status can be modified
        GroupStatus curState = GroupStatus.forCode(entity.getStatus());
        if (GroupStatus.notAllowedUpdate(curState)) {
            throw new BusinessException(String.format(ErrorCodeEnum.OPT_NOT_ALLOWED_BY_STATUS.getMessage(), curState));
        }
        // Check whether the stream exist or not
        InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(
                request.getInlongGroupId(), request.getInlongStreamId());
        if (streamEntity == null) {
            throw new BusinessException(ErrorCodeEnum.STREAM_NOT_FOUND);
        }
        // Check whether the sink name exists with the same groupId and streamId
        StreamSinkEntity existEntity = sinkMapper.selectByUniqueKey(
                request.getInlongGroupId(), request.getInlongStreamId(), request.getSinkName());
        if (existEntity != null && !existEntity.getId().equals(request.getId())) {
            throw new BusinessException(ErrorCodeEnum.RECORD_DUPLICATE,
                    String.format("sink name=%s already exists with the groupId=%s streamId=%s",
                            request.getSinkName(), request.getInlongGroupId(), request.getInlongStreamId()));
        }
        // update record
        SinkStatus nextStatus = null;
        boolean streamSuccess = StreamStatus.CONFIG_SUCCESSFUL.getCode().equals(streamEntity.getStatus());
        if (streamSuccess || StreamStatus.CONFIG_FAILED.getCode().equals(streamEntity.getStatus())) {
            boolean enableCreateResource = InlongConstants.ENABLE_CREATE_RESOURCE.equals(
                    request.getEnableCreateResource());
            nextStatus = enableCreateResource ? SinkStatus.CONFIG_ING : SinkStatus.CONFIG_SUCCESSFUL;
        }
        StreamSinkOperator sinkOperator = operatorFactory.getInstance(request.getSinkType());
        sinkOperator.updateOpt(request, nextStatus, opInfo.getName());
        // If the stream is [CONFIG_SUCCESSFUL], then asynchronously start the [CREATE_STREAM_RESOURCE] process
        if (streamSuccess && request.getStartProcess()) {
            this.startProcessForSink(request.getInlongGroupId(), request.getInlongStreamId(), opInfo.getName());
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public UpdateResult updateByKey(SinkRequest request, String operator) {
        LOGGER.info("begin to update sink by key: {}", request);

        // Check whether the stream sink exists
        String groupId = request.getInlongGroupId();
        String streamId = request.getInlongStreamId();
        String sinkName = request.getSinkName();
        StreamSinkEntity entity = sinkMapper.selectByUniqueKey(groupId, streamId, sinkName);
        if (entity == null) {
            String errMsg = String.format("stream sink not found with groupId=%s, streamId=%s, sinkName=%s",
                    groupId, streamId, sinkName);
            LOGGER.error(errMsg);
            throw new BusinessException(errMsg);
        }

        request.setId(entity.getId());
        Boolean result = this.update(request, operator);
        LOGGER.info("success to update sink by key: {}", request);
        return new UpdateResult(entity.getId(), result, request.getVersion() + 1);
    }

    @Override
    public void updateStatus(Integer id, int status, String log) {
        StreamSinkEntity entity = new StreamSinkEntity();
        entity.setId(id);
        entity.setStatus(status);
        entity.setOperateLog(log);
        sinkMapper.updateStatus(entity);

        LOGGER.info("success to update sink status={} for id={} with log: {}", status, id, log);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean delete(Integer id, Boolean startProcess, String operator) {
        LOGGER.info("begin to delete sink by id={}", id);
        Preconditions.checkNotNull(id, ErrorCodeEnum.ID_IS_EMPTY.getMessage());
        StreamSinkEntity entity = sinkMapper.selectByPrimaryKey(id);
        Preconditions.checkNotNull(entity, ErrorCodeEnum.SINK_INFO_NOT_FOUND.getMessage());

        groupCheckService.checkGroupStatus(entity.getInlongGroupId(), operator);

        StreamSinkOperator sinkOperator = operatorFactory.getInstance(entity.getSinkType());
        sinkOperator.deleteOpt(entity, operator);

        if (startProcess) {
            this.deleteProcessForSink(entity.getInlongGroupId(), entity.getInlongStreamId(), operator);
        }

        LOGGER.info("success to delete sink by id: {}", entity);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean delete(Integer id, Boolean startProcess, UserInfo opInfo) {
        if (id == null) {
            throw new BusinessException(ErrorCodeEnum.ID_IS_EMPTY);
        }
        // check opInfo
        if (opInfo == null) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_USER_EMPTY);
        }
        // check stream sink record
        StreamSinkEntity sinkEntity = sinkMapper.selectByPrimaryKey(id);
        if (sinkEntity == null) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_NOT_FOUND);
        }
        // check group record
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(sinkEntity.getInlongGroupId());
        if (groupEntity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND,
                    String.format("InlongGroup does not exist with InlongGroupId=%s", sinkEntity.getInlongGroupId()));
        }
        // only the person in charges can query
        if (!opInfo.getRoles().contains(UserTypeEnum.ADMIN.name())) {
            List<String> inCharges = Arrays.asList(groupEntity.getInCharges().split(InlongConstants.COMMA));
            if (!inCharges.contains(opInfo.getName())) {
                throw new BusinessException(ErrorCodeEnum.GROUP_PERMISSION_DENIED);
            }
        }
        // Check if group status can be modified
        GroupStatus curState = GroupStatus.forCode(groupEntity.getStatus());
        if (GroupStatus.notAllowedUpdate(curState)) {
            throw new BusinessException(String.format(ErrorCodeEnum.OPT_NOT_ALLOWED_BY_STATUS.getMessage(), curState));
        }
        // delete record
        StreamSinkOperator sinkOperator = operatorFactory.getInstance(sinkEntity.getSinkType());
        sinkOperator.deleteOpt(sinkEntity, opInfo.getName());
        if (startProcess) {
            this.deleteProcessForSink(sinkEntity.getInlongGroupId(), sinkEntity.getInlongStreamId(), opInfo.getName());
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean deleteByKey(String groupId, String streamId, String sinkName,
            Boolean startProcess, String operator) {
        LOGGER.info("begin to delete sink by groupId={}, streamId={}, sinkName={}", groupId, streamId, sinkName);

        // Check whether the sink name exists with the same groupId and streamId
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        Preconditions.checkNotNull(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());
        Preconditions.checkNotNull(sinkName, "stream sink name is empty or null");
        StreamSinkEntity entity = sinkMapper.selectByUniqueKey(groupId, streamId, sinkName);
        Preconditions.checkNotNull(entity, String.format("stream sink not exist by groupId=%s streamId=%s sinkName=%s",
                groupId, streamId, sinkName));

        groupCheckService.checkGroupStatus(entity.getInlongGroupId(), operator);

        StreamSinkOperator sinkOperator = operatorFactory.getInstance(entity.getSinkType());
        sinkOperator.deleteOpt(entity, operator);

        if (startProcess) {
            this.deleteProcessForSink(entity.getInlongGroupId(), entity.getInlongStreamId(), operator);
        }

        LOGGER.info("success to delete sink by key: {}", entity);
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

        List<StreamSinkEntity> entityList = sinkMapper.selectByRelatedId(groupId, streamId);
        if (CollectionUtils.isNotEmpty(entityList)) {
            entityList.forEach(entity -> {
                Integer id = entity.getId();
                entity.setPreviousStatus(entity.getStatus());
                entity.setStatus(InlongConstants.DELETED_STATUS);
                entity.setIsDeleted(id);
                entity.setModifier(operator);
                int rowCount = sinkMapper.updateByIdSelective(entity);
                if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
                    LOGGER.error("sink has already updated with groupId={}, streamId={}, name={}, curVersion={}",
                            entity.getInlongGroupId(), entity.getInlongStreamId(), entity.getSinkName(),
                            entity.getVersion());
                    throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
                }
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

        List<StreamSinkEntity> entityList = sinkMapper.selectByRelatedId(groupId, streamId);
        if (CollectionUtils.isNotEmpty(entityList)) {
            entityList.forEach(entity -> {
                sinkMapper.deleteById(entity.getId());
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

        for (SinkApproveDTO dto : approveList) {
            // According to the sink type, save sink information
            String sinkType = dto.getSinkType();
            Preconditions.checkNotNull(sinkType, ErrorCodeEnum.SINK_TYPE_IS_NULL.getMessage());

            StreamSinkEntity entity = sinkMapper.selectByPrimaryKey(dto.getId());

            int status = (dto.getStatus() == null) ? SinkStatus.CONFIG_ING.getCode() : dto.getStatus();
            entity.setPreviousStatus(entity.getStatus());
            entity.setStatus(status);
            entity.setModifier(operator);
            int rowCount = sinkMapper.updateByIdSelective(entity);
            if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
                LOGGER.error("sink has already updated with groupId={}, streamId={}, name={}, curVersion={}",
                        entity.getInlongGroupId(), entity.getInlongStreamId(), entity.getSinkName(),
                        entity.getVersion());
                throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
            }
        }

        LOGGER.info("success to update sink after approve: {}", approveList);
        return true;
    }

    private void checkSinkRequestParams(SinkRequest request) {
        // check request parameter
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.REQUEST_IS_EMPTY);
        }
        // check group id
        String groupId = request.getInlongGroupId();
        if (StringUtils.isBlank(groupId)) {
            throw new BusinessException(ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        }
        // check stream id
        String streamId = request.getInlongStreamId();
        if (StringUtils.isBlank(streamId)) {
            throw new BusinessException(ErrorCodeEnum.STREAM_ID_IS_EMPTY);
        }
        // check sinkType
        String sinkType = request.getSinkType();
        if (StringUtils.isBlank(sinkType)) {
            throw new BusinessException(ErrorCodeEnum.SINK_TYPE_IS_NULL);
        }
        // check sinkName
        String sinkName = request.getSinkName();
        if (StringUtils.isBlank(sinkName)) {
            throw new BusinessException(ErrorCodeEnum.SINK_NAME_IS_NULL);
        }
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

    private void startProcessForSink(String groupId, String streamId, String operator) {
        // to work around the circular reference check, manually instantiate and wire
        if (streamProcessOperation == null) {
            streamProcessOperation = new InlongStreamProcessService();
            autowireCapableBeanFactory.autowireBean(streamProcessOperation);
        }

        streamProcessOperation.startProcess(groupId, streamId, operator, false);
        LOGGER.info("success to start the start-stream-process for groupId={} streamId={}", groupId, streamId);
    }

    private void deleteProcessForSink(String groupId, String streamId, String operator) {
        // to work around the circular reference check, manually instantiate and wire
        if (streamProcessOperation == null) {
            streamProcessOperation = new InlongStreamProcessService();
            autowireCapableBeanFactory.autowireBean(streamProcessOperation);
        }

        streamProcessOperation.deleteProcess(groupId, streamId, operator, false);
        LOGGER.info("success to start the delete-stream-process for groupId={} streamId={}", groupId, streamId);
    }
}
