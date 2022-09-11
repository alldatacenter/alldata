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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GlobalConstants;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.StreamStatus;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.sink.SinkBriefResponse;
import org.apache.inlong.manager.common.pojo.sink.SinkRequest;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.common.pojo.source.SourceRequest;
import org.apache.inlong.manager.common.pojo.source.StreamSource;
import org.apache.inlong.manager.common.pojo.stream.FullStreamRequest;
import org.apache.inlong.manager.common.pojo.stream.FullStreamResponse;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamApproveRequest;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamExtInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamListResponse;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamPageRequest;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamRequest;
import org.apache.inlong.manager.common.pojo.stream.StreamBriefResponse;
import org.apache.inlong.manager.common.pojo.stream.StreamField;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.InlongStreamEntity;
import org.apache.inlong.manager.dao.entity.InlongStreamExtEntity;
import org.apache.inlong.manager.dao.entity.InlongStreamFieldEntity;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamExtEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamFieldEntityMapper;
import org.apache.inlong.manager.service.core.InlongStreamService;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Inlong stream service layer implementation
 */
@Service
public class InlongStreamServiceImpl implements InlongStreamService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongStreamServiceImpl.class);

    @Autowired
    private InlongStreamEntityMapper streamMapper;
    @Autowired
    private InlongStreamFieldEntityMapper streamFieldMapper;
    @Autowired
    private InlongStreamExtEntityMapper streamExtMapper;
    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private StreamSourceService sourceService;
    @Autowired
    private StreamSinkService sinkService;

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public Integer save(InlongStreamRequest request, String operator) {
        LOGGER.debug("begin to save inlong stream info={}", request);
        Preconditions.checkNotNull(request, "inlong stream info is empty");
        String groupId = request.getInlongGroupId();
        String streamId = request.getInlongStreamId();
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        Preconditions.checkNotNull(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());

        // Check if it can be added
        checkGroupStatusIsTemp(groupId);

        // The streamId under the same groupId cannot be repeated
        Integer count = streamMapper.selectExistByIdentifier(groupId, streamId);
        if (count >= 1) {
            LOGGER.error("inlong stream id [{}] has already exists", streamId);
            throw new BusinessException(ErrorCodeEnum.STREAM_ID_DUPLICATE);
        }
        if (StringUtils.isEmpty(request.getMqResource())) {
            request.setMqResource(streamId);
        }
        // Processing inlong stream
        InlongStreamEntity streamEntity = CommonBeanUtils.copyProperties(request, InlongStreamEntity::new);
        streamEntity.setStatus(StreamStatus.NEW.getCode());
        streamEntity.setCreator(operator);
        streamEntity.setCreateTime(new Date());

        streamMapper.insertSelective(streamEntity);
        saveField(groupId, streamId, request.getFieldList());
        if (CollectionUtils.isNotEmpty(request.getExtList())) {
            saveOrUpdateExt(groupId, streamId, request.getExtList());
        }

        LOGGER.info("success to save inlong stream info for groupId={}", groupId);
        return streamEntity.getId();
    }

    @Override
    public InlongStreamInfo get(String groupId, String streamId) {
        LOGGER.debug("begin to get inlong stream by groupId={}, streamId={}", groupId, streamId);
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        Preconditions.checkNotNull(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());

        InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(groupId, streamId);
        if (streamEntity == null) {
            LOGGER.error("inlong stream not found by groupId={}, streamId={}", groupId, streamId);
            throw new BusinessException(ErrorCodeEnum.STREAM_NOT_FOUND);
        }

        InlongStreamInfo streamInfo = CommonBeanUtils.copyProperties(streamEntity, InlongStreamInfo::new);
        List<StreamField> streamFields = getStreamFields(groupId, streamId);
        streamInfo.setFieldList(streamFields);
        List<InlongStreamExtEntity> extEntities = streamExtMapper.selectByRelatedId(groupId, streamId);
        List<InlongStreamExtInfo> exts = CommonBeanUtils.copyListProperties(extEntities, InlongStreamExtInfo::new);
        streamInfo.setExtList(exts);
        LOGGER.info("success to get inlong stream for groupId={}", groupId);
        return streamInfo;
    }

    @Override
    public List<InlongStreamInfo> list(String groupId) {
        LOGGER.debug("begin to list inlong streams by groupId={}", groupId);
        List<InlongStreamEntity> inlongStreamEntityList = streamMapper.selectByGroupId(groupId);
        List<InlongStreamInfo> streamList = CommonBeanUtils.copyListProperties(inlongStreamEntityList,
                InlongStreamInfo::new);
        List<StreamField> streamFields = getStreamFields(groupId, null);
        Map<String, List<StreamField>> streamFieldMap = streamFields.stream().collect(
                Collectors.groupingBy(StreamField::getInlongStreamId,
                        HashMap::new,
                        Collectors.toCollection(ArrayList::new)));
        List<InlongStreamExtEntity> extEntities = streamExtMapper.selectByRelatedId(groupId, null);
        Map<String, List<InlongStreamExtInfo>> extInfoMap = extEntities.stream()
                .map(extEntity -> CommonBeanUtils.copyProperties(extEntity, InlongStreamExtInfo::new))
                .collect(Collectors.groupingBy(InlongStreamExtInfo::getInlongStreamId,
                        HashMap::new,
                        Collectors.toCollection(ArrayList::new)));
        streamList.forEach(streamInfo -> {
            String streamId = streamInfo.getInlongStreamId();
            List<StreamField> fieldInfos = streamFieldMap.get(streamId);
            streamInfo.setFieldList(fieldInfos);
            List<InlongStreamExtInfo> extInfos = extInfoMap.get(streamId);
            streamInfo.setExtList(extInfos);
        });
        return streamList;
    }

    @Override
    public Boolean exist(String groupId, String streamId) {
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());
        InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(groupId, streamId);
        return streamEntity != null;
    }

    /**
     * Query and set the extended information and data source fields of the inlong stream
     */
    private List<StreamField> getStreamFields(String groupId, String streamId) {
        List<InlongStreamFieldEntity> fieldEntityList = streamFieldMapper.selectByIdentifier(groupId, streamId);
        if (CollectionUtils.isEmpty(fieldEntityList)) {
            return Collections.emptyList();
        }
        return CommonBeanUtils.copyListProperties(fieldEntityList, StreamField::new);
    }

    @Override
    public PageInfo<InlongStreamListResponse> listByCondition(InlongStreamPageRequest request) {
        LOGGER.debug("begin to list inlong stream page by {}", request);

        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<InlongStreamEntity> entityPage = (Page<InlongStreamEntity>) streamMapper.selectByCondition(request);
        List<InlongStreamListResponse> streamList = CommonBeanUtils.copyListProperties(entityPage,
                InlongStreamListResponse::new);

        // Filter out inlong streams that do not have this sink type (only one of each inlong stream can be created)
        String groupId = request.getInlongGroupId();
        String sinkType = request.getSinkType();
        if (StringUtils.isNotEmpty(sinkType)) {
            List<String> streamIdList = streamList.stream().map(InlongStreamListResponse::getInlongStreamId)
                    .distinct().collect(Collectors.toList());
            List<String> resultList = sinkService.getExistsStreamIdList(groupId, sinkType, streamIdList);
            streamList.removeIf(entity -> resultList.contains(entity.getInlongStreamId()));
        }

        // Query all stream sink targets corresponding to each inlong stream according to streamId
        if (request.getNeedSinkList() == 1) {
            streamList.forEach(stream -> {
                String streamId = stream.getInlongStreamId();
                List<String> sinkTypeList = sinkService.getSinkTypeList(groupId, streamId);
                stream.setSinkTypeList(sinkTypeList);
            });
        }

        // Encapsulate the paging query results into the PageInfo object to obtain related paging information
        PageInfo<InlongStreamListResponse> page = new PageInfo<>(streamList);
        page.setTotal(streamList.size());

        LOGGER.debug("success to list inlong stream info for groupId={}", groupId);
        return page;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public Boolean update(InlongStreamRequest request, String operator) {
        LOGGER.debug("begin to update inlong stream info={}", request);
        Preconditions.checkNotNull(request, "inlong stream request is empty");
        String groupId = request.getInlongGroupId();
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        String streamId = request.getInlongStreamId();
        Preconditions.checkNotNull(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());

        // Check if it can be modified
        InlongGroupEntity inlongGroupEntity = this.checkGroupStatusIsTemp(groupId);

        // Make sure the stream was exists
        InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(groupId, streamId);
        if (streamEntity == null) {
            LOGGER.error("inlong stream not found by groupId={}, streamId={}", groupId, streamId);
            throw new BusinessException(ErrorCodeEnum.STREAM_NOT_FOUND);
        }

        // Check whether the current inlong group status supports modification
        this.checkCanUpdate(inlongGroupEntity.getStatus(), streamEntity, request);

        CommonBeanUtils.copyProperties(request, streamEntity, true);
        streamEntity.setModifier(operator);
        streamMapper.updateByIdentifierSelective(streamEntity);

        // Update field information
        updateField(groupId, streamId, request.getFieldList());
        // Update extension info
        List<InlongStreamExtInfo> extInfos = request.getExtList();
        saveOrUpdateExt(groupId, streamId, extInfos);

        LOGGER.info("success to update inlong stream for groupId={}", groupId);
        return true;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public Boolean delete(String groupId, String streamId, String operator) {
        LOGGER.debug("begin to delete inlong stream, groupId={}, streamId={}", groupId, streamId);
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        Preconditions.checkNotNull(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());

        // Check if it can be deleted
        this.checkGroupStatusIsTemp(groupId);

        InlongStreamEntity entity = streamMapper.selectByIdentifier(groupId, streamId);
        if (entity == null) {
            LOGGER.error("inlong stream not found by groupId={}, streamId={}", groupId, streamId);
            throw new BusinessException(ErrorCodeEnum.STREAM_NOT_FOUND);
        }

        // If there is undeleted stream source, the deletion fails
        Integer sourceCount = sourceService.getCount(groupId, streamId);
        if (sourceCount > 0) {
            LOGGER.error("inlong stream has undeleted sources, delete failed");
            throw new BusinessException(ErrorCodeEnum.STREAM_DELETE_HAS_SOURCE);
        }

        // If there is undeleted stream sink, the deletion fails
        int sinkCount = sinkService.getCount(groupId, streamId);
        if (sinkCount > 0) {
            LOGGER.error("inlong stream has undeleted sinks, delete failed");
            throw new BusinessException(ErrorCodeEnum.STREAM_DELETE_HAS_SINK);
        }

        entity.setIsDeleted(entity.getId());
        entity.setModifier(operator);
        streamMapper.updateByPrimaryKey(entity);

        // Logically delete the associated field table
        LOGGER.debug("begin to delete inlong stream field, streamId={}", streamId);
        streamFieldMapper.logicDeleteAllByIdentifier(groupId, streamId);
        streamExtMapper.logicDeleteAllByRelatedId(groupId, streamId);

        LOGGER.info("success to delete inlong stream, ext property and fields for groupId={}", groupId);
        return true;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public Boolean logicDeleteAll(String groupId, String operator) {
        LOGGER.debug("begin to delete all inlong stream by groupId={}", groupId);
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());

        // Check if it can be deleted
        this.checkGroupStatusIsTemp(groupId);

        List<InlongStreamEntity> entityList = streamMapper.selectByGroupId(groupId);
        if (CollectionUtils.isEmpty(entityList)) {
            LOGGER.info("inlong stream not found by groupId={}", groupId);
            return true;
        }

        for (InlongStreamEntity entity : entityList) {
            entity.setIsDeleted(1);
            entity.setModifier(operator);
            streamMapper.updateByIdentifierSelective(entity);

            String streamId = entity.getInlongStreamId();
            // Logically delete the associated field, source and sink info
            streamFieldMapper.logicDeleteAllByIdentifier(groupId, streamId);
            streamExtMapper.logicDeleteAllByRelatedId(groupId, streamId);
            sourceService.logicDeleteAll(groupId, streamId, operator);
            sinkService.logicDeleteAll(groupId, streamId, operator);
        }

        LOGGER.info("success to delete all inlong stream, ext property and fields by groupId={}", groupId);
        return true;
    }

    @Override
    public List<StreamBriefResponse> getBriefList(String groupId) {
        LOGGER.debug("begin to get inlong stream brief list by groupId={}", groupId);
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());

        List<InlongStreamEntity> entityList = streamMapper.selectByGroupId(groupId);
        List<StreamBriefResponse> briefInfoList = CommonBeanUtils
                .copyListProperties(entityList, StreamBriefResponse::new);

        // Query stream sinks based on groupId and streamId
        for (StreamBriefResponse briefInfo : briefInfoList) {
            String streamId = briefInfo.getInlongStreamId();
            List<SinkBriefResponse> sinkList = sinkService.listBrief(groupId, streamId);
            briefInfo.setSinkList(sinkList);
        }

        LOGGER.info("success to get inlong stream brief list for groupId={}", groupId);
        return briefInfoList;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public boolean saveAll(FullStreamRequest fullStreamRequest, String operator) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("begin to save all stream page info: {}", fullStreamRequest);
        }
        Preconditions.checkNotNull(fullStreamRequest, "fullStreamRequest is empty");
        InlongStreamRequest streamRequest = fullStreamRequest.getStreamInfo();
        Preconditions.checkNotNull(streamRequest, "inlong stream info is empty");

        // Save inlong stream
        save(streamRequest, operator);

        // Save source info
        if (CollectionUtils.isNotEmpty(fullStreamRequest.getSourceInfo())) {
            for (SourceRequest source : fullStreamRequest.getSourceInfo()) {
                sourceService.save(source, operator);
            }
        }

        // Save sink info
        if (CollectionUtils.isNotEmpty(fullStreamRequest.getSinkInfo())) {
            for (SinkRequest sinkInfo : fullStreamRequest.getSinkInfo()) {
                sinkService.save(sinkInfo, operator);
            }
        }

        LOGGER.info("success to save all stream page info");
        return true;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public boolean batchSaveAll(List<FullStreamRequest> fullStreamRequestList, String operator) {
        if (CollectionUtils.isEmpty(fullStreamRequestList)) {
            return true;
        }
        LOGGER.info("begin to batch save all stream page info, batch size={}", fullStreamRequestList.size());

        // Check if it can be added
        InlongStreamRequest firstStream = fullStreamRequestList.get(0).getStreamInfo();
        Preconditions.checkNotNull(firstStream, "inlong stream info is empty");
        String groupId = firstStream.getInlongGroupId();
        this.checkGroupStatusIsTemp(groupId);

        // This bulk save is only used when creating or editing inlong group after approval is rejected.
        // To ensure data consistency, you need to physically delete all associated data and then add
        // Note: There may be records with the same groupId and streamId in the historical data,
        // and the ones with is_deleted=0 should be deleted
        streamMapper.deleteAllByGroupId(groupId);

        for (FullStreamRequest pageInfo : fullStreamRequestList) {
            // Delete the inlong stream extensions and fields corresponding to groupId and streamId
            InlongStreamRequest streamInfo = pageInfo.getStreamInfo();
            String streamId = streamInfo.getInlongStreamId();
            streamFieldMapper.deleteAllByIdentifier(groupId, streamId);
            streamExtMapper.deleteAllByRelatedId(groupId, streamId);
            //  Delete all stream source
            sourceService.deleteAll(groupId, streamId, operator);
            // Delete all stream sink
            sinkService.deleteAll(groupId, streamId, operator);
            // Save the inlong stream of this batch
            this.saveAll(pageInfo, operator);
        }
        LOGGER.info("success to batch save all stream page info");
        return true;
    }

    @Override
    public PageInfo<FullStreamResponse> listAllWithGroupId(InlongStreamPageRequest request) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("begin to list full inlong stream page by {}", request);
        }
        Preconditions.checkNotNull(request, "request is empty");
        Preconditions.checkNotNull(request.getInlongGroupId(), ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());

        // 1. Query all valid data sources under groupId
        String groupId = request.getInlongGroupId();
        // The person in charge of the inlong group has the authority of all inlong streams
        InlongGroupEntity inlongGroupEntity = groupMapper.selectByGroupId(groupId);
        Preconditions.checkNotNull(inlongGroupEntity, "inlong group not found by groupId=" + groupId);

        String inCharges = inlongGroupEntity.getInCharges();
        request.setInCharges(inCharges);

        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<InlongStreamEntity> page = (Page<InlongStreamEntity>) streamMapper.selectByCondition(request);
        List<InlongStreamInfo> streamInfoList = CommonBeanUtils.copyListProperties(page, InlongStreamInfo::new);

        // Convert and encapsulate the paged results
        List<FullStreamResponse> responseList = new ArrayList<>(streamInfoList.size());
        for (InlongStreamInfo streamInfo : streamInfoList) {
            // Set the field information of the inlong stream
            String streamId = streamInfo.getInlongStreamId();
            List<StreamField> streamFields = getStreamFields(groupId, streamId);
            streamInfo.setFieldList(streamFields);
            List<InlongStreamExtInfo> streamExtInfos = CommonBeanUtils.copyListProperties(
                    streamExtMapper.selectByRelatedId(groupId, streamId), InlongStreamExtInfo::new);
            streamInfo.setExtList(streamExtInfos);

            FullStreamResponse pageInfo = new FullStreamResponse();
            pageInfo.setStreamInfo(streamInfo);

            // Query stream sources information
            List<StreamSource> sourceList = sourceService.listSource(groupId, streamId);
            pageInfo.setSourceInfo(sourceList);

            // Query various stream sinks and its extended information, field information
            List<StreamSink> sinkList = sinkService.listSink(groupId, streamId);
            pageInfo.setSinkInfo(sinkList);

            // Add a single result to the paginated list
            responseList.add(pageInfo);
        }

        PageInfo<FullStreamResponse> pageInfo = new PageInfo<>(responseList);
        pageInfo.setTotal(pageInfo.getTotal());

        LOGGER.debug("success to list full inlong stream info");
        return pageInfo;
    }

    @Override
    public int selectCountByGroupId(String groupId) {
        LOGGER.debug("begin to get count by groupId={}", groupId);
        if (StringUtils.isEmpty(groupId)) {
            return 0;
        }
        int count = streamMapper.selectCountByGroupId(groupId);
        LOGGER.info("success to get count");
        return count;
    }

    @Override
    public List<InlongStreamBriefInfo> getTopicList(String groupId) {
        LOGGER.debug("begin bo get topic list by group id={}", groupId);
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());

        List<InlongStreamBriefInfo> topicList = streamMapper.selectBriefList(groupId);
        LOGGER.debug("success to get topic list by groupId={}", groupId);
        return topicList;
    }

    @Override
    public boolean updateAfterApprove(List<InlongStreamApproveRequest> streamApproveList, String operator) {
        if (CollectionUtils.isEmpty(streamApproveList)) {
            return true;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("begin to update stream after approve={}", streamApproveList);
        }

        String groupId = null;
        for (InlongStreamApproveRequest info : streamApproveList) {
            // Modify the inlong stream info after approve
            InlongStreamEntity streamEntity = new InlongStreamEntity();
            groupId = info.getInlongGroupId(); // these groupIds are all the same
            streamEntity.setInlongGroupId(groupId);
            streamEntity.setInlongStreamId(info.getInlongStreamId());
            streamEntity.setStatus(StreamStatus.CONFIG_ING.getCode());
            streamMapper.updateByIdentifierSelective(streamEntity);

            // Modify the sink info after approve, such as update cluster info
            sinkService.updateAfterApprove(info.getSinkList(), operator);
        }

        LOGGER.info("success to update stream after approve for groupId={}", groupId);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean updateStatus(String groupId, String streamId, Integer status, String operator) {
        LOGGER.debug("begin to update status by groupId={}, streamId={}", groupId, streamId);
        streamMapper.updateStatusByIdentifier(groupId, streamId, status, operator);
        LOGGER.info("success to update stream after approve for groupId=" + groupId + ", streamId=" + streamId);
        return true;
    }

    @Override
    public void insertDlqOrRlq(String groupId, String topicName, String operator) {
        Integer count = streamMapper.selectExistByIdentifier(groupId, topicName);
        if (count >= 1) {
            LOGGER.error("DLQ/RLQ topic already exists with name={}", topicName);
            throw new BusinessException(ErrorCodeEnum.STREAM_ID_DUPLICATE, "DLQ/RLQ topic already exists");
        }

        InlongStreamEntity streamEntity = new InlongStreamEntity();
        streamEntity.setInlongGroupId(groupId);
        streamEntity.setInlongStreamId(topicName);
        streamEntity.setMqResource(topicName);
        streamEntity.setDescription("This is DLQ / RLQ topic created by SYSTEM");
        streamEntity.setDailyRecords(1000);
        streamEntity.setDailyStorage(1000);
        streamEntity.setPeakRecords(1000);
        streamEntity.setMaxLength(1000);

        streamEntity.setStatus(StreamStatus.CONFIG_SUCCESSFUL.getCode());
        streamEntity.setIsDeleted(GlobalConstants.UN_DELETED);
        streamEntity.setCreator(operator);
        streamEntity.setModifier(operator);
        Date now = new Date();
        streamEntity.setCreateTime(now);
        streamEntity.setModifyTime(now);

        streamMapper.insert(streamEntity);
    }

    @Override
    public void logicDeleteDlqOrRlq(String groupId, String topicName, String operator) {
        streamMapper.logicDeleteDlqOrRlq(groupId, topicName, operator);
        LOGGER.info("success to logic delete dlq or rlq by groupId={}, topicName={}", groupId, topicName);
    }

    /**
     * Update field information
     * <p/>First physically delete the existing field information, and then add the field information of this batch
     */
    @Transactional(rollbackFor = Throwable.class)
    void updateField(String groupId, String streamId, List<StreamField> fieldList) {
        LOGGER.debug("begin to update inlong stream field, groupId={}, streamId={}, field={}", groupId, streamId,
                fieldList);
        try {
            streamFieldMapper.deleteAllByIdentifier(groupId, streamId);
            saveField(groupId, streamId, fieldList);
            LOGGER.info("success to update inlong stream field for groupId={}", groupId);
        } catch (Exception e) {
            LOGGER.error("failed to update inlong stream field: ", e);
            throw new BusinessException(ErrorCodeEnum.STREAM_FIELD_SAVE_FAILED);
        }
    }

    @Transactional(rollbackFor = Throwable.class)
    void saveField(String groupId, String streamId, List<StreamField> infoList) {
        if (CollectionUtils.isEmpty(infoList)) {
            return;
        }
        infoList.stream().forEach(streamField -> streamField.setId(null));
        List<InlongStreamFieldEntity> list = CommonBeanUtils.copyListProperties(infoList,
                InlongStreamFieldEntity::new);
        for (InlongStreamFieldEntity entity : list) {
            entity.setInlongGroupId(groupId);
            entity.setInlongStreamId(streamId);
            entity.setIsDeleted(GlobalConstants.UN_DELETED);
        }
        streamFieldMapper.insertAll(list);
    }

    @Transactional(rollbackFor = Throwable.class)
    void saveOrUpdateExt(String groupId, String streamId, List<InlongStreamExtInfo> exts) {
        LOGGER.info("begin to save or update inlong stream ext info, groupId={}, streamId={}, ext={}", groupId,
                streamId, exts);
        if (CollectionUtils.isEmpty(exts)) {
            return;
        }

        List<InlongStreamExtEntity> entityList = CommonBeanUtils.copyListProperties(exts, InlongStreamExtEntity::new);
        entityList.forEach(streamEntity -> {
            streamEntity.setInlongGroupId(groupId);
            streamEntity.setInlongStreamId(streamId);
        });
        streamExtMapper.insertOnDuplicateKeyUpdate(entityList);
        LOGGER.info("success to save or update inlong stream ext for groupId={}", groupId);
    }

    /**
     * Check whether the inlong group status is temporary
     *
     * @param groupId inlong group id
     * @return inlong group entity
     */
    private InlongGroupEntity checkGroupStatusIsTemp(String groupId) {
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        Preconditions.checkNotNull(entity, "groupId is invalid");
        // Add/modify/delete is not allowed under certain inlong group status
        GroupStatus curState = GroupStatus.forCode(entity.getStatus());
        if (GroupStatus.isTempStatus(curState)) {
            LOGGER.error("inlong group status was not allowed to add/update/delete inlong stream");
            throw new BusinessException(ErrorCodeEnum.STREAM_OPT_NOT_ALLOWED);
        }

        return entity;
    }

    /**
     * Verify the fields that cannot be modified in the current inlong group status
     *
     * @param groupStatus Inlong group status
     * @param streamEntity Original inlong stream entity
     * @param request New inlong stream information
     */
    private void checkCanUpdate(Integer groupStatus, InlongStreamEntity streamEntity, InlongStreamRequest request) {
        if (streamEntity == null || request == null) {
            return;
        }

        // Fields that are not allowed to be modified when the inlong group [configuration is successful]
        if (GroupStatus.CONFIG_SUCCESSFUL.getCode().equals(groupStatus)) {
            checkUpdatedFields(streamEntity, request);
        }

        // Inlong group [Waiting to submit] [Approval rejected] [Configuration failed], if there is a
        // stream source/stream sink, the fields that are not allowed to be modified
        List<Integer> statusList = Arrays.asList(
                GroupStatus.TO_BE_SUBMIT.getCode(),
                GroupStatus.APPROVE_REJECTED.getCode(),
                GroupStatus.CONFIG_FAILED.getCode());
        if (statusList.contains(groupStatus)) {
            String groupId = request.getInlongGroupId();
            String streamId = request.getInlongStreamId();
            // Whether there is undeleted stream source and sink
            int sourceCount = sourceService.getCount(groupId, streamId);
            int sinkCount = sinkService.getCount(groupId, streamId);
            if (sourceCount > 0 || sinkCount > 0) {
                checkUpdatedFields(streamEntity, request);
            }
        }
    }

    /**
     * Check that groupId, streamId  are not allowed to be modified
     */
    private void checkUpdatedFields(InlongStreamEntity streamEntity, InlongStreamRequest request) {
        String newGroupId = request.getInlongGroupId();
        if (newGroupId != null && !newGroupId.equals(streamEntity.getInlongGroupId())) {
            LOGGER.error("current status was not allowed to update inlong group id");
            throw new BusinessException(ErrorCodeEnum.STREAM_ID_UPDATE_NOT_ALLOWED);
        }

        String newStreamId = request.getInlongStreamId();
        if (newStreamId != null && !newStreamId.equals(streamEntity.getInlongStreamId())) {
            LOGGER.error("current status was not allowed to update inlong stream id");
            throw new BusinessException(ErrorCodeEnum.STREAM_ID_UPDATE_NOT_ALLOWED);
        }
    }

}
