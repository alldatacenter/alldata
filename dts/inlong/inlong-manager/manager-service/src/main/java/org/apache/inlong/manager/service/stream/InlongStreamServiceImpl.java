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

package org.apache.inlong.manager.service.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.StreamStatus;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.tool.excel.ExcelTool;
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
import org.apache.inlong.manager.pojo.common.OrderFieldEnum;
import org.apache.inlong.manager.pojo.common.OrderTypeEnum;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.sink.ParseFieldRequest;
import org.apache.inlong.manager.pojo.sink.SinkBriefInfo;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sort.util.FieldInfoUtils;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.stream.InlongStreamApproveRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamExtInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamPageRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamRequest;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.pojo.user.UserRoleCode;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.apache.inlong.manager.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.inlong.manager.common.consts.InlongConstants.PATTERN_NORMAL_CHARACTERS;
import static org.apache.inlong.manager.common.consts.InlongConstants.STATEMENT_TYPE_CSV;
import static org.apache.inlong.manager.common.consts.InlongConstants.STATEMENT_TYPE_JSON;
import static org.apache.inlong.manager.common.consts.InlongConstants.STATEMENT_TYPE_SQL;
import static org.apache.inlong.manager.common.consts.InlongConstants.STREAM_FIELD_TYPES;
import static org.apache.inlong.manager.common.consts.InlongConstants.BATCH_PARSING_FILED_JSON_COMMENT_PROP;
import static org.apache.inlong.manager.common.consts.InlongConstants.BATCH_PARSING_FILED_JSON_NAME_PROP;
import static org.apache.inlong.manager.common.consts.InlongConstants.BATCH_PARSING_FILED_JSON_TYPE_PROP;
import static org.apache.inlong.manager.pojo.stream.InlongStreamExtParam.packExtParams;
import static org.apache.inlong.manager.pojo.stream.InlongStreamExtParam.unpackExtParams;

/**
 * Inlong stream service layer implementation
 */
@Service
public class InlongStreamServiceImpl implements InlongStreamService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongStreamServiceImpl.class);
    private static final String PARSE_FIELD_CSV_SPLITTER = "\t|\\s|,";
    private static final int PARSE_FIELD_CSV_MAX_COLUMNS = 3;
    private static final int PARSE_FIELD_CSV_MIN_COLUMNS = 2;

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
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserService userService;

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public Integer save(InlongStreamRequest request, String operator) {
        LOGGER.debug("begin to save inlong stream info={}", request);
        Preconditions.expectNotNull(request, "inlong stream info is empty");
        String groupId = request.getInlongGroupId();
        String streamId = request.getInlongStreamId();
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        Preconditions.expectNotBlank(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY);

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
        streamEntity.setModifier(operator);
        // Processing extended attributes
        String extParam = packExtParams(request);
        streamEntity.setExtParams(extParam);

        streamMapper.insertSelective(streamEntity);
        saveField(groupId, streamId, request.getFieldList());
        List<InlongStreamExtInfo> extList = request.getExtList();
        if (CollectionUtils.isNotEmpty(extList)) {
            saveOrUpdateExt(groupId, streamId, extList);
        }

        LOGGER.info("success to save inlong stream info for groupId={}", groupId);
        return streamEntity.getId();
    }

    @Override
    public Integer save(InlongStreamRequest request, UserInfo opInfo) {
        InlongGroupEntity entity = groupMapper.selectByGroupId(request.getInlongGroupId());
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }
        // only the person in charges can query
        userService.checkUser(entity.getInCharges(), opInfo.getName(),
                ErrorCodeEnum.GROUP_PERMISSION_DENIED.getMessage());
        // Add/modify/delete is not allowed under temporary inlong group status
        GroupStatus curState = GroupStatus.forCode(entity.getStatus());
        if (GroupStatus.isTempStatus(curState)) {
            throw new BusinessException(ErrorCodeEnum.STREAM_OPT_NOT_ALLOWED,
                    String.format("inlong groupId=%s status=%s was not allowed to add/update/delete stream",
                            request.getInlongGroupId(), curState));
        }
        // The streamId under the same groupId cannot be repeated
        Integer count = streamMapper.selectExistByIdentifier(
                request.getInlongGroupId(), request.getInlongStreamId());
        if (count >= 1) {
            throw new BusinessException(ErrorCodeEnum.STREAM_ID_DUPLICATE);
        }
        if (StringUtils.isEmpty(request.getMqResource())) {
            request.setMqResource(request.getInlongStreamId());
        }
        // Processing extended attributes
        String extParams = packExtParams(request);
        request.setExtParams(extParams);
        // Processing inlong stream
        InlongStreamEntity streamEntity = CommonBeanUtils.copyProperties(request, InlongStreamEntity::new);
        streamEntity.setStatus(StreamStatus.NEW.getCode());
        streamEntity.setCreator(opInfo.getName());
        streamEntity.setModifier(opInfo.getName());
        // add record
        streamMapper.insertSelective(streamEntity);
        saveField(request.getInlongGroupId(), request.getInlongStreamId(), request.getFieldList());
        List<InlongStreamExtInfo> extList = request.getExtList();
        if (CollectionUtils.isNotEmpty(extList)) {
            saveOrUpdateExt(request.getInlongGroupId(), request.getInlongStreamId(), extList);
        }
        return streamEntity.getId();
    }

    @Override
    public Boolean exist(String groupId, String streamId) {
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        Preconditions.expectNotBlank(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY);
        InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(groupId, streamId);
        return streamEntity != null;
    }

    @Override
    public InlongStreamInfo get(String groupId, String streamId) {
        LOGGER.debug("begin to get inlong stream by groupId={}, streamId={}", groupId, streamId);
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        Preconditions.expectNotBlank(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY);

        InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(groupId, streamId);
        if (streamEntity == null) {
            LOGGER.error("inlong stream not found by groupId={}, streamId={}", groupId, streamId);
            throw new BusinessException(ErrorCodeEnum.STREAM_NOT_FOUND);
        }

        InlongStreamInfo streamInfo = CommonBeanUtils.copyProperties(streamEntity, InlongStreamInfo::new);
        List<StreamField> streamFields = getStreamFields(groupId, streamId);
        streamInfo.setFieldList(streamFields);
        // load ext infos
        List<InlongStreamExtEntity> extEntities = streamExtMapper.selectByRelatedId(groupId, streamId);
        List<InlongStreamExtInfo> extInfos = CommonBeanUtils.copyListProperties(extEntities, InlongStreamExtInfo::new);
        streamInfo.setExtList(extInfos);
        // load extParams
        unpackExtParams(streamEntity.getExtParams(), streamInfo);

        List<StreamSink> sinkList = sinkService.listSink(groupId, streamId);
        streamInfo.setSinkList(sinkList);
        List<StreamSource> sourceList = sourceService.listSource(groupId, streamId);
        streamInfo.setSourceList(sourceList);
        return streamInfo;
    }

    @Override
    public InlongStreamInfo get(String groupId, String streamId, UserInfo opInfo) {
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }
        // only the person in charges can query
        userService.checkUser(entity.getInCharges(), opInfo.getName(),
                ErrorCodeEnum.GROUP_PERMISSION_DENIED.getMessage());
        // get stream information
        InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(groupId, streamId);
        if (streamEntity == null) {
            throw new BusinessException(ErrorCodeEnum.STREAM_NOT_FOUND);
        }
        InlongStreamInfo streamInfo = CommonBeanUtils.copyProperties(streamEntity, InlongStreamInfo::new);
        // Processing extParams
        unpackExtParams(streamEntity.getExtParams(), streamInfo);
        // Load fields
        List<StreamField> streamFields = getStreamFields(groupId, streamId);
        streamInfo.setFieldList(streamFields);
        List<InlongStreamExtEntity> extEntities = streamExtMapper.selectByRelatedId(groupId, streamId);
        List<InlongStreamExtInfo> extInfos = CommonBeanUtils.copyListProperties(extEntities, InlongStreamExtInfo::new);
        streamInfo.setExtList(extInfos);
        List<StreamSink> sinkList = sinkService.listSink(groupId, streamId);
        streamInfo.setSinkList(sinkList);
        List<StreamSource> sourceList = sourceService.listSource(groupId, streamId);
        streamInfo.setSourceList(sourceList);
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
            // Processing extParams
            unpackExtParams(streamInfo.getExtParams(), streamInfo);
            List<StreamField> fieldInfos = streamFieldMap.get(streamId);
            streamInfo.setFieldList(fieldInfos);
            List<InlongStreamExtInfo> extInfos = extInfoMap.get(streamId);
            streamInfo.setExtList(extInfos);
            List<StreamSink> sinkList = sinkService.listSink(groupId, streamId);
            streamInfo.setSinkList(sinkList);
            List<StreamSource> sourceList = sourceService.listSource(groupId, streamId);
            streamInfo.setSourceList(sourceList);
        });
        return streamList;
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
    public PageResult<InlongStreamBriefInfo> listBrief(InlongStreamPageRequest request) {
        LOGGER.debug("begin to list inlong stream page by {}", request);

        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        OrderFieldEnum.checkOrderField(request);
        OrderTypeEnum.checkOrderType(request);
        Page<InlongStreamEntity> entityPage = (Page<InlongStreamEntity>) streamMapper.selectByCondition(request);
        List<InlongStreamBriefInfo> streamList = CommonBeanUtils.copyListProperties(entityPage,
                InlongStreamBriefInfo::new);

        PageResult<InlongStreamBriefInfo> pageResult = new PageResult<>(streamList,
                entityPage.getTotal(), entityPage.getPageNum(), entityPage.getPageSize());

        LOGGER.debug("success to list inlong stream info for groupId={}", request.getInlongGroupId());
        return pageResult;
    }

    @Override
    public List<InlongStreamBriefInfo> listBrief(InlongStreamPageRequest request, UserInfo opInfo) {
        request.setCurrentUser(opInfo.getName());
        request.setIsAdminRole(opInfo.getRoles().contains(UserRoleCode.ADMIN));
        OrderFieldEnum.checkOrderField(request);
        OrderTypeEnum.checkOrderType(request);
        return CommonBeanUtils.copyListProperties(streamMapper.selectByCondition(request), InlongStreamBriefInfo::new);
    }

    @Override
    public PageResult<InlongStreamInfo> listAll(InlongStreamPageRequest request) {
        LOGGER.debug("begin to list full inlong stream page by {}", request);
        Preconditions.expectNotNull(request, "request is empty");
        String groupId = request.getInlongGroupId();
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(groupId);
        Preconditions.expectNotNull(groupEntity, "inlong group not found by groupId=" + groupId);

        // the person in charge of the inlong group has the authority of all inlong streams,
        // so do not filter by in charge person
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        OrderFieldEnum.checkOrderField(request);
        OrderTypeEnum.checkOrderType(request);
        Page<InlongStreamEntity> page = (Page<InlongStreamEntity>) streamMapper.selectByCondition(request);
        List<InlongStreamInfo> streamInfoList = CommonBeanUtils.copyListProperties(page, InlongStreamInfo::new);

        // Convert and encapsulate the paged results
        for (InlongStreamInfo streamInfo : streamInfoList) {
            // Set the field information of the inlong stream
            String streamId = streamInfo.getInlongStreamId();
            unpackExtParams(streamInfo);
            List<StreamField> streamFields = getStreamFields(groupId, streamId);
            streamInfo.setFieldList(streamFields);
            List<InlongStreamExtEntity> extEntities = streamExtMapper.selectByRelatedId(groupId, streamId);
            List<InlongStreamExtInfo> streamExtInfos = CommonBeanUtils.copyListProperties(
                    extEntities, InlongStreamExtInfo::new);
            streamInfo.setExtList(streamExtInfos);

            // query all valid stream sources
            List<StreamSource> sourceList = sourceService.listSource(groupId, streamId);
            streamInfo.setSourceList(sourceList);

            // query all valid stream sinks and its extended info, field info
            List<StreamSink> sinkList = sinkService.listSink(groupId, streamId);
            streamInfo.setSinkList(sinkList);
        }

        PageResult<InlongStreamInfo> pageResult = new PageResult<>(streamInfoList, page.getTotal(),
                page.getPageNum(), page.getPageSize());

        LOGGER.debug("success to list full inlong stream info by {}", request);
        return pageResult;
    }

    @Override
    public List<InlongStreamBriefInfo> listBriefWithSink(String groupId) {
        LOGGER.debug("begin to get inlong stream brief list by groupId={}", groupId);
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);

        List<InlongStreamEntity> entityList = streamMapper.selectByGroupId(groupId);
        List<InlongStreamBriefInfo> briefInfoList = CommonBeanUtils
                .copyListProperties(entityList, InlongStreamBriefInfo::new);

        // query stream sinks based on groupId and streamId
        for (InlongStreamBriefInfo briefInfo : briefInfoList) {
            String streamId = briefInfo.getInlongStreamId();
            List<SinkBriefInfo> sinkList = sinkService.listBrief(groupId, streamId);
            briefInfo.setSinkList(sinkList);
        }

        LOGGER.info("success to get inlong stream brief list for groupId={}", groupId);
        return briefInfoList;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean update(InlongStreamRequest request, String operator) {
        LOGGER.debug("begin to update inlong stream info={}", request);
        Preconditions.expectNotNull(request, "inlong stream request is empty");
        String groupId = request.getInlongGroupId();
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        String streamId = request.getInlongStreamId();
        Preconditions.expectNotBlank(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY);

        // Check if it can be modified
        this.checkGroupStatusIsTemp(groupId);

        return this.updateWithoutCheck(request, operator);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean update(InlongStreamRequest request, UserInfo opInfo) {
        InlongGroupEntity entity = groupMapper.selectByGroupId(request.getInlongGroupId());
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }
        // check record version
        Preconditions.expectEquals(entity.getVersion(), request.getVersion(),
                ErrorCodeEnum.CONFIG_EXPIRED,
                String.format("record has expired with record version=%d, request version=%d",
                        entity.getVersion(), request.getVersion()));
        // only the person in charges can query
        userService.checkUser(entity.getInCharges(), opInfo.getName(),
                ErrorCodeEnum.GROUP_PERMISSION_DENIED.getMessage());
        // Add/modify/delete is not allowed under temporary inlong group status
        GroupStatus curState = GroupStatus.forCode(entity.getStatus());
        if (GroupStatus.isTempStatus(curState)) {
            throw new BusinessException(ErrorCodeEnum.STREAM_OPT_NOT_ALLOWED,
                    String.format("inlong groupId=%s status=%s was not allowed to add/update/delete stream",
                            request.getInlongGroupId(), curState));
        }
        // check stream status
        InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(
                request.getInlongGroupId(), request.getInlongStreamId());
        if (streamEntity == null) {
            throw new BusinessException(ErrorCodeEnum.STREAM_NOT_FOUND,
                    String.format("inlong stream not found by groupId=%s, streamId=%s",
                            request.getInlongGroupId(), request.getInlongStreamId()));
        }
        if (!Objects.equals(streamEntity.getVersion(), request.getVersion())) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED,
                    String.format("stream has already updated with groupId=%s, streamId=%s, curVersion=%s",
                            streamEntity.getInlongGroupId(), streamEntity.getInlongStreamId(), request.getVersion()));
        }
        // Processing extended attributes
        String extParams = packExtParams(request);
        request.setExtParams(extParams);
        // update record
        CommonBeanUtils.copyProperties(request, streamEntity, true);
        streamEntity.setModifier(opInfo.getName());
        if (InlongConstants.AFFECTED_ONE_ROW != streamMapper.updateByIdentifierSelective(streamEntity)) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        // update stream fields
        updateField(request.getInlongGroupId(), request.getInlongStreamId(), request.getFieldList());
        // update stream extension infos
        List<InlongStreamExtInfo> extList = request.getExtList();
        saveOrUpdateExt(request.getInlongGroupId(), request.getInlongStreamId(), extList);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean updateWithoutCheck(InlongStreamRequest request, String operator) {
        LOGGER.debug("begin to update inlong stream without check, request={}", request);
        // make sure the stream was exists
        String groupId = request.getInlongGroupId();
        String streamId = request.getInlongStreamId();
        InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(groupId, streamId);
        if (streamEntity == null) {
            LOGGER.error("inlong stream not found by groupId={}, streamId={}", groupId, streamId);
            throw new BusinessException(ErrorCodeEnum.STREAM_NOT_FOUND);
        }

        String errMsg = String.format("stream has already updated with groupId=%s, streamId=%s, curVersion=%s",
                streamEntity.getInlongGroupId(), streamEntity.getInlongStreamId(), request.getVersion());
        if (!Objects.equals(streamEntity.getVersion(), request.getVersion())) {
            LOGGER.error(errMsg);
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        // Processing extended attributes
        String extParams = packExtParams(request);
        request.setExtParams(extParams);
        CommonBeanUtils.copyProperties(request, streamEntity, true);
        streamEntity.setModifier(operator);
        int rowCount = streamMapper.updateByIdentifierSelective(streamEntity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            LOGGER.error(errMsg);
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        // update stream fields
        updateField(groupId, streamId, request.getFieldList());
        // update stream extension infos
        List<InlongStreamExtInfo> extList = request.getExtList();
        saveOrUpdateExt(groupId, streamId, extList);

        LOGGER.info("success to update inlong stream without check for groupId={} streamId={}", groupId, streamId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean delete(String groupId, String streamId, String operator) {
        LOGGER.debug("begin to delete inlong stream, groupId={}, streamId={}", groupId, streamId);
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        Preconditions.expectNotBlank(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY);

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
        int rowCount = streamMapper.updateByPrimaryKey(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            LOGGER.error("stream has already updated with group id={}, stream id={}, curVersion={}",
                    entity.getInlongGroupId(), entity.getInlongStreamId(), entity.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        // Logically delete the associated field table
        LOGGER.debug("begin to delete inlong stream field, streamId={}", streamId);
        streamFieldMapper.logicDeleteAllByIdentifier(groupId, streamId);
        streamExtMapper.logicDeleteAllByRelatedId(groupId, streamId);

        LOGGER.info("success to delete inlong stream, ext property and fields for groupId={}", groupId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean delete(String groupId, String streamId, UserInfo opInfo) {
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(groupId);
        if (groupEntity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }
        // only the person in charges can query
        userService.checkUser(groupEntity.getInCharges(), opInfo.getName(),
                ErrorCodeEnum.GROUP_PERMISSION_DENIED.getMessage());
        // Add/modify/delete is not allowed under temporary inlong group status
        GroupStatus curState = GroupStatus.forCode(groupEntity.getStatus());
        if (GroupStatus.isTempStatus(curState)) {
            throw new BusinessException(ErrorCodeEnum.STREAM_OPT_NOT_ALLOWED,
                    String.format("inlong groupId=%s status=%s was not allowed to add/update/delete stream", groupId,
                            curState));
        }
        // Check if steam record exists
        InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(groupId, streamId);
        if (streamEntity == null) {
            throw new BusinessException(ErrorCodeEnum.STREAM_NOT_FOUND);
        }
        // If there is undeleted stream source, the deletion fails
        if (sourceService.getCount(groupId, streamId) > 0) {
            throw new BusinessException(ErrorCodeEnum.STREAM_DELETE_HAS_SOURCE);
        }
        // If there is undeleted stream sink, the deletion fails
        if (sinkService.getCount(groupId, streamId) > 0) {
            throw new BusinessException(ErrorCodeEnum.STREAM_DELETE_HAS_SINK);
        }
        // delete record
        streamEntity.setIsDeleted(streamEntity.getId());
        streamEntity.setModifier(opInfo.getName());
        if (streamMapper.updateByPrimaryKey(streamEntity) != InlongConstants.AFFECTED_ONE_ROW) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        // Logically delete the associated field table
        streamFieldMapper.logicDeleteAllByIdentifier(groupId, streamId);
        streamExtMapper.logicDeleteAllByRelatedId(groupId, streamId);
        return true;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public Boolean logicDeleteAll(String groupId, String operator) {
        LOGGER.debug("begin to delete all inlong stream by groupId={}", groupId);
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);

        // Check if it can be deleted
        this.checkGroupStatusIsTemp(groupId);

        List<InlongStreamEntity> entityList = streamMapper.selectByGroupId(groupId);
        if (CollectionUtils.isEmpty(entityList)) {
            LOGGER.info("inlong stream not found by groupId={}", groupId);
            return true;
        }

        for (InlongStreamEntity entity : entityList) {
            entity.setIsDeleted(entity.getId());
            entity.setModifier(operator);

            int rowCount = streamMapper.updateByIdentifierSelective(entity);
            if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
                LOGGER.error("stream has already updated with group id={}, stream id={}, curVersion={}",
                        entity.getInlongGroupId(), entity.getInlongStreamId(), entity.getVersion());
                throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
            }
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
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);

        List<InlongStreamBriefInfo> topicList = streamMapper.selectBriefList(groupId);
        LOGGER.debug("success to get topic list by groupId={}, result size={}", groupId, topicList.size());
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
            groupId = info.getInlongGroupId(); // these groupIds are all the same
            // Modify the inlong stream info after approve
            InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(groupId, info.getInlongStreamId());
            streamEntity.setStatus(StreamStatus.CONFIG_ING.getCode());

            int rowCount = streamMapper.updateByIdentifierSelective(streamEntity);
            if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
                LOGGER.error("stream has already updated with group id={}, stream id={}, curVersion={}",
                        streamEntity.getInlongGroupId(), streamEntity.getInlongStreamId(), streamEntity.getVersion());
                throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
            }
            // Modify the sink info after approve, such as update cluster info
            sinkService.updateAfterApprove(info.getSinkList(), operator);
        }

        LOGGER.info("success to update stream after approve for groupId={}", groupId);
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean updateStatus(String groupId, String streamId, Integer status, String operator) {
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
        streamEntity.setCreator(operator);
        streamEntity.setModifier(operator);

        streamMapper.insert(streamEntity);
    }

    @Override
    public void logicDeleteDlqOrRlq(String groupId, String topicName, String operator) {
        streamMapper.logicDeleteDlqOrRlq(groupId, topicName, operator);
        LOGGER.info("success to logic delete dlq or rlq by groupId={}, topicName={}", groupId, topicName);
    }

    @Override
    public List<StreamField> parseFields(ParseFieldRequest parseFieldRequest) {
        try {
            String method = parseFieldRequest.getMethod();
            String statement = parseFieldRequest.getStatement();

            switch (method) {
                case STATEMENT_TYPE_JSON:
                    return parseFieldsByJson(statement);
                case STATEMENT_TYPE_SQL:
                    return parseFieldsBySql(statement);
                case STATEMENT_TYPE_CSV:
                    return parseFieldsByCsv(statement);
                default:
                    throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                            String.format("Unsupported parse field mode: %s", method));
            }
        } catch (Exception e) {
            LOGGER.error("parse inlong stream fields error", e);
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    String.format("parse stream fields error : %s", e.getMessage()));
        }
    }

    @Override
    public List<StreamField> parseFields(MultipartFile file) {
        InputStream inputStream;
        try {
            inputStream = file.getInputStream();
        } catch (IOException e) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "Can not properly read update file");
        }
        List<StreamField> data = null;
        try {
            data = ExcelTool.read(inputStream, StreamField.class);
        } catch (IOException | IllegalAccessException | InstantiationException | NoSuchMethodException e) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "Can not properly parse excel, message: " + e.getClass().getName() + ":" + e.getMessage());
        }
        if (CollectionUtils.isEmpty(data)) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "The content of uploaded Excel file is empty, please check!");
        }
        return data;
    }

    /**
     * Parse fields from CSV format
     * @param statement CSV statement
     * @return List of StreamField
     */
    private List<StreamField> parseFieldsByCsv(String statement) {
        String[] lines = statement.split(InlongConstants.NEW_LINE);
        List<StreamField> fields = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (StringUtils.isBlank(line)) {
                continue;
            }

            String[] cols = line.split(PARSE_FIELD_CSV_SPLITTER, PARSE_FIELD_CSV_MAX_COLUMNS);
            if (cols.length < PARSE_FIELD_CSV_MIN_COLUMNS) {
                throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                        "At least two fields are required, line number is " + (i + 1));
            }
            String fieldName = cols[0];
            if (!PATTERN_NORMAL_CHARACTERS.matcher(fieldName).matches()) {
                throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "Field names in line " + (i + 1) +
                        " can only contain letters, underscores or numbers");
            }
            String fieldType = cols[1];
            if (!STREAM_FIELD_TYPES.contains(fieldType)) {
                throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "The field type in line" + (i + 1) +
                        " must be one of " + STREAM_FIELD_TYPES);
            }

            String comment = null;
            if (cols.length == PARSE_FIELD_CSV_MAX_COLUMNS) {
                comment = cols[PARSE_FIELD_CSV_MAX_COLUMNS - 1];
            }

            StreamField field = new StreamField();
            field.setFieldName(fieldName);
            field.setFieldType(fieldType);
            field.setFieldComment(comment);
            fields.add(field);
        }
        return fields;
    }
    private List<StreamField> parseFieldsBySql(String sql) throws JSQLParserException {
        CCJSqlParserManager pm = new CCJSqlParserManager();
        Statement statement = pm.parse(new StringReader(sql));
        List<StreamField> fields = new ArrayList<>();
        if (!(statement instanceof CreateTable)) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "The SQL statement must be a table creation statement");
        }
        CreateTable createTable = (CreateTable) statement;
        List<ColumnDefinition> columnDefinitions = createTable.getColumnDefinitions();
        // get column definition
        for (int i = 0; i < columnDefinitions.size(); i++) {
            ColumnDefinition definition = columnDefinitions.get(i);
            StreamField streamField = new StreamField();
            // get field name
            String columnName = definition.getColumnName();
            streamField.setFieldName(columnName);

            ColDataType colDataType = definition.getColDataType();
            String sqlDataType = colDataType.getDataType();
            // convert SQL type to Java type
            Class<?> clazz = FieldInfoUtils.sqlTypeToJavaType(sqlDataType);
            if (clazz == Object.class) {
                throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                        "Unrecognized SQL field type, line: " + (i + 1) + ", type: " + sqlDataType);
            }
            String type = clazz.getSimpleName().toLowerCase();
            streamField.setFieldType(type);
            // get field comment
            List<String> columnSpecs = definition.getColumnSpecs();
            if (CollectionUtils.isNotEmpty(columnSpecs)) {
                int commentIndex = -1;
                for (int csIndex = 0; csIndex < columnSpecs.size(); csIndex++) {
                    String spec = columnSpecs.get(csIndex);
                    if (spec.toUpperCase().startsWith("COMMENT")) {
                        commentIndex = csIndex;
                        break;
                    }
                }
                String comment = null;
                if (-1 != commentIndex && columnSpecs.size() > commentIndex + 1) {
                    comment = columnSpecs.get(commentIndex + 1).replaceAll("['\"]", "");
                }
                streamField.setFieldComment(comment);
            }
            fields.add(streamField);
        }
        return fields;
    }

    private List<StreamField> parseFieldsByJson(String statement) throws JsonProcessingException {
        return objectMapper.readValue(statement, new TypeReference<List<Map<String, String>>>() {
        }).stream().map(line -> {
            String name = line.get(BATCH_PARSING_FILED_JSON_NAME_PROP);
            String type = line.get(BATCH_PARSING_FILED_JSON_TYPE_PROP);
            String desc = line.get(BATCH_PARSING_FILED_JSON_COMMENT_PROP);
            StreamField streamField = new StreamField();
            streamField.setFieldName(name);
            streamField.setFieldType(type);
            streamField.setFieldComment(desc);
            return streamField;
        }).collect(Collectors.toList());
    }

    /**
     * Update field information
     * <p/>
     * First physically delete the existing field information, and then add the field information of this batch
     */
    @Transactional(rollbackFor = Throwable.class)
    public void updateField(String groupId, String streamId, List<StreamField> fieldList) {
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
    public void saveField(String groupId, String streamId, List<StreamField> infoList) {
        if (CollectionUtils.isEmpty(infoList)) {
            return;
        }
        infoList.forEach(streamField -> streamField.setId(null));
        List<InlongStreamFieldEntity> list = CommonBeanUtils.copyListProperties(infoList,
                InlongStreamFieldEntity::new);
        for (InlongStreamFieldEntity entity : list) {
            entity.setInlongGroupId(groupId);
            entity.setInlongStreamId(streamId);
            entity.setIsDeleted(InlongConstants.UN_DELETED);
        }
        streamFieldMapper.insertAll(list);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveOrUpdateExt(String groupId, String streamId, List<InlongStreamExtInfo> extInfos) {
        LOGGER.info("begin to save or update inlong stream ext info, groupId={}, streamId={}, ext={}", groupId,
                streamId, extInfos);
        if (CollectionUtils.isEmpty(extInfos)) {
            return;
        }

        List<InlongStreamExtEntity> entityList =
                CommonBeanUtils.copyListProperties(extInfos, InlongStreamExtEntity::new);
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
        Preconditions.expectNotNull(entity, "groupId is invalid");
        // Add/modify/delete is not allowed under temporary inlong group status
        GroupStatus curState = GroupStatus.forCode(entity.getStatus());
        if (GroupStatus.isTempStatus(curState)) {
            LOGGER.error("inlong groupId={} status={} was not allowed to add/update/delete stream", groupId, curState);
            throw new BusinessException(ErrorCodeEnum.STREAM_OPT_NOT_ALLOWED);
        }

        return entity;
    }

}
