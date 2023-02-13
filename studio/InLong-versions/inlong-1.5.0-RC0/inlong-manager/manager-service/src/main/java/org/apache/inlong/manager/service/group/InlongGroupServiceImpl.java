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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.auth.Authentication.AuthType;
import org.apache.inlong.manager.common.auth.SecretTokenAuthentication;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.InlongGroupExtEntity;
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupExtEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSourceEntityMapper;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.common.OrderFieldEnum;
import org.apache.inlong.manager.pojo.common.OrderTypeEnum;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.group.InlongGroupApproveRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupBriefInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupCountResponse;
import org.apache.inlong.manager.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupPageRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicRequest;
import org.apache.inlong.manager.pojo.sort.BaseSortConf;
import org.apache.inlong.manager.pojo.sort.BaseSortConf.SortType;
import org.apache.inlong.manager.pojo.sort.FlinkSortConf;
import org.apache.inlong.manager.pojo.sort.UserDefinedSortConf;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.source.SourceOperatorFactory;
import org.apache.inlong.manager.service.source.StreamSourceOperator;
import org.apache.inlong.manager.service.stream.InlongStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.inlong.common.constant.ClusterSwitch.BACKUP_CLUSTER_TAG;
import static org.apache.inlong.manager.pojo.common.PageRequest.MAX_PAGE_SIZE;

/**
 * Inlong group service layer implementation
 */
@Service
@Validated
public class InlongGroupServiceImpl implements InlongGroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongGroupServiceImpl.class);

    @Value("${sort.enable.zookeeper:false}")
    private boolean enableZookeeper;

    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private InlongGroupExtEntityMapper groupExtMapper;
    @Autowired
    private InlongStreamService streamService;
    @Autowired
    private StreamSourceEntityMapper streamSourceMapper;
    @Autowired
    private InlongClusterService clusterService;

    @Autowired
    private InlongGroupOperatorFactory groupOperatorFactory;
    @Autowired
    private SourceOperatorFactory sourceOperatorFactory;

    /**
     * Check whether modification is supported under the current group status, and which fields can be modified.
     *
     * @param entity original inlong group entity
     * @param request request of updated
     * @param operator current operator
     */
    private static void doUpdateCheck(InlongGroupEntity entity, InlongGroupRequest request, String operator) {
        if (entity == null || request == null) {
            return;
        }

        // only the person in charges can update
        List<String> inCharges = Arrays.asList(entity.getInCharges().split(InlongConstants.COMMA));
        if (!inCharges.contains(operator)) {
            LOGGER.error("user [{}] has no privilege for the inlong group", operator);
            throw new BusinessException(ErrorCodeEnum.GROUP_PERMISSION_DENIED);
        }

        // check whether the current status supports modification
        GroupStatus curStatus = GroupStatus.forCode(entity.getStatus());
        if (GroupStatus.notAllowedUpdate(curStatus)) {
            String errMsg = String.format("Current status=%s is not allowed to update", curStatus);
            LOGGER.error(errMsg);
            throw new BusinessException(ErrorCodeEnum.GROUP_UPDATE_NOT_ALLOWED, errMsg);
        }

        // mq type cannot be changed
        if (!entity.getMqType().equals(request.getMqType()) && !GroupStatus.allowedUpdateMQ(curStatus)) {
            String errMsg = String.format("Current status=%s is not allowed to update MQ type", curStatus);
            LOGGER.error(errMsg);
            throw new BusinessException(ErrorCodeEnum.GROUP_UPDATE_NOT_ALLOWED, errMsg);
        }
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public String save(InlongGroupRequest request, String operator) {
        LOGGER.debug("begin to save inlong group={} by user={}", request, operator);
        Preconditions.checkNotNull(request, "inlong group request cannot be empty");

        String groupId = request.getInlongGroupId();
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        if (entity != null) {
            LOGGER.error("groupId={} has already exists", groupId);
            throw new BusinessException(ErrorCodeEnum.GROUP_DUPLICATE);
        }

        request.setEnableZookeeper(enableZookeeper ? InlongConstants.ENABLE_ZK : InlongConstants.DISABLE_ZK);
        InlongGroupOperator instance = groupOperatorFactory.getInstance(request.getMqType());
        groupId = instance.saveOpt(request, operator);

        // save ext info
        this.saveOrUpdateExt(groupId, request.getExtList());

        LOGGER.info("success to save inlong group for groupId={} by user={}", groupId, operator);
        return groupId;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public String save(InlongGroupRequest request, UserInfo opInfo) {
        // check parameter
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.REQUEST_IS_EMPTY,
                    "inlong group request cannot be empty");
        }
        // check operator info
        if (opInfo == null) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_USER_EMPTY);
        }
        String groupId = request.getInlongGroupId();
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        if (entity != null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_DUPLICATE);
        }
        request.setEnableZookeeper(enableZookeeper ? InlongConstants.ENABLE_ZK : InlongConstants.DISABLE_ZK);
        InlongGroupOperator instance = groupOperatorFactory.getInstance(request.getMqType());
        groupId = instance.saveOpt(request, opInfo.getName());
        // save ext info
        this.saveOrUpdateExt(groupId, request.getExtList());
        return groupId;
    }

    @Override
    public Boolean exist(String groupId) {
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        LOGGER.debug("success to check inlong group {}, exist? {}", groupId, entity != null);
        return entity != null;
    }

    @Override
    public InlongGroupInfo get(String groupId) {
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        if (entity == null) {
            LOGGER.error("inlong group not found by groupId={}", groupId);
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }

        InlongGroupOperator instance = groupOperatorFactory.getInstance(entity.getMqType());
        InlongGroupInfo groupInfo = instance.getFromEntity(entity);

        // get all ext info
        List<InlongGroupExtEntity> extEntityList = groupExtMapper.selectByGroupId(groupId);
        List<InlongGroupExtInfo> extList = CommonBeanUtils.copyListProperties(extEntityList, InlongGroupExtInfo::new);
        groupInfo.setExtList(extList);
        BaseSortConf sortConf = buildSortConfig(extList);
        groupInfo.setSortConf(sortConf);

        LOGGER.debug("success to get inlong group for groupId={}", groupId);
        return groupInfo;
    }

    @Override
    public InlongGroupInfo get(String groupId, UserInfo opInfo) {
        // check group id
        if (StringUtils.isBlank(groupId)) {
            throw new BusinessException(ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        }
        // check operator info
        if (opInfo == null) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_USER_EMPTY);
        }
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }
        // only the person in charges can query
        if (!opInfo.getRoles().contains(UserTypeEnum.ADMIN.name())) {
            List<String> inCharges = Arrays.asList(entity.getInCharges().split(InlongConstants.COMMA));
            if (!inCharges.contains(opInfo.getName())) {
                throw new BusinessException(ErrorCodeEnum.GROUP_PERMISSION_DENIED);
            }
        }
        // query mq information
        InlongGroupOperator instance = groupOperatorFactory.getInstance(entity.getMqType());
        InlongGroupInfo groupInfo = instance.getFromEntity(entity);
        // get all ext info
        List<InlongGroupExtEntity> extEntityList = groupExtMapper.selectByGroupId(groupId);
        List<InlongGroupExtInfo> extList = CommonBeanUtils.copyListProperties(extEntityList, InlongGroupExtInfo::new);
        groupInfo.setExtList(extList);
        BaseSortConf sortConf = buildSortConfig(extList);
        groupInfo.setSortConf(sortConf);
        return groupInfo;
    }

    @Override
    public InlongGroupCountResponse countGroupByUser(String operator) {
        InlongGroupCountResponse countVO = new InlongGroupCountResponse();
        List<Map<String, Object>> statusCount = groupMapper.countGroupByUser(operator);
        for (Map<String, Object> map : statusCount) {
            int status = (Integer) map.get("status");
            long count = (Long) map.get("count");
            countVO.setTotalCount(countVO.getTotalCount() + count);
            if (status == GroupStatus.CONFIG_ING.getCode()) {
                countVO.setWaitAssignCount(countVO.getWaitAssignCount() + count);
            } else if (status == GroupStatus.TO_BE_APPROVAL.getCode()) {
                countVO.setWaitApproveCount(countVO.getWaitApproveCount() + count);
            } else if (status == GroupStatus.APPROVE_REJECTED.getCode()) {
                countVO.setRejectCount(countVO.getRejectCount() + count);
            }
        }

        LOGGER.debug("success to count inlong group for operator={}", operator);
        return countVO;
    }

    @Override
    public InlongGroupTopicInfo getTopic(String groupId) {
        // the group info will not null in get() method
        InlongGroupInfo groupInfo = this.get(groupId);
        InlongGroupOperator groupOperator = groupOperatorFactory.getInstance(groupInfo.getMqType());
        InlongGroupTopicInfo topicInfo = groupOperator.getTopic(groupInfo);

        // set the base params
        topicInfo.setInlongGroupId(groupId);
        String clusterTag = groupInfo.getInlongClusterTag();
        topicInfo.setInlongClusterTag(clusterTag);

        // assert: each MQ type has a corresponding type of cluster
        List<ClusterInfo> clusterInfos = clusterService.listByTagAndType(clusterTag, groupInfo.getMqType());
        topicInfo.setClusterInfos(clusterInfos);

        LOGGER.debug("success to get topic for groupId={}, result={}", groupId, topicInfo);
        return topicInfo;
    }

    @Override
    public InlongGroupTopicInfo getBackupTopic(String groupId) {
        // backup topic info saved in the ext table
        InlongGroupExtEntity extEntity = groupExtMapper.selectByUniqueKey(groupId, BACKUP_CLUSTER_TAG);
        if (extEntity == null || StringUtils.isBlank(extEntity.getKeyValue())) {
            LOGGER.warn("not found any backup topic for groupId={}", groupId);
            return null;
        }

        // the group info will not null in get() method
        InlongGroupInfo groupInfo = this.get(groupId);
        InlongGroupOperator groupOperator = groupOperatorFactory.getInstance(groupInfo.getMqType());
        InlongGroupTopicInfo backupTopicInfo = groupOperator.getBackupTopic(groupInfo);

        // set the base params
        backupTopicInfo.setInlongGroupId(groupId);
        String backupClusterTag = extEntity.getKeyValue();
        backupTopicInfo.setInlongClusterTag(backupClusterTag);

        // set backup cluster info
        // assert: each MQ type has a corresponding type of cluster
        List<ClusterInfo> clusterInfos = clusterService.listByTagAndType(backupClusterTag, groupInfo.getMqType());
        backupTopicInfo.setClusterInfos(clusterInfos);

        LOGGER.debug("success to get backup topic for groupId={}, result={}", groupId, backupTopicInfo);
        return backupTopicInfo;
    }

    @Override
    public PageResult<InlongGroupBriefInfo> listBrief(InlongGroupPageRequest request) {
        if (request.getPageSize() > MAX_PAGE_SIZE) {
            LOGGER.warn("list inlong groups, change page size from {} to {}", request.getPageSize(), MAX_PAGE_SIZE);
            request.setPageSize(MAX_PAGE_SIZE);
        }
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        OrderFieldEnum.checkOrderField(request);
        OrderTypeEnum.checkOrderType(request);
        Page<InlongGroupEntity> entityPage = (Page<InlongGroupEntity>) groupMapper.selectByCondition(request);

        List<InlongGroupBriefInfo> briefInfos = CommonBeanUtils.copyListProperties(entityPage,
                InlongGroupBriefInfo::new);

        // list all related sources
        if (request.isListSources() && CollectionUtils.isNotEmpty(briefInfos)) {
            Set<String> groupIds = briefInfos.stream().map(InlongGroupBriefInfo::getInlongGroupId)
                    .collect(Collectors.toSet());
            List<StreamSourceEntity> sourceEntities = streamSourceMapper.selectByGroupIds(new ArrayList<>(groupIds));
            Map<String, List<StreamSource>> sourceMap = Maps.newHashMap();
            sourceEntities.forEach(sourceEntity -> {
                StreamSourceOperator operation = sourceOperatorFactory.getInstance(sourceEntity.getSourceType());
                StreamSource source = operation.getFromEntity(sourceEntity);
                sourceMap.computeIfAbsent(sourceEntity.getInlongGroupId(), k -> Lists.newArrayList()).add(source);
            });
            briefInfos.forEach(group -> {
                List<StreamSource> sources = sourceMap.getOrDefault(group.getInlongGroupId(), Lists.newArrayList());
                group.setStreamSources(sources);
            });
        }

        PageResult<InlongGroupBriefInfo> pageResult = new PageResult<>(briefInfos,
                entityPage.getTotal(), entityPage.getPageNum(), entityPage.getPageSize());

        LOGGER.debug("success to list inlong group for {}", request);
        return pageResult;
    }

    @Override
    public List<InlongGroupBriefInfo> listBrief(InlongGroupPageRequest request, UserInfo opInfo) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "group query request cannot be empty");
        }
        // check operator info
        if (opInfo == null) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_USER_EMPTY);
        }
        // filter records;
        List<InlongGroupEntity> filterGroupEntities = new ArrayList<>();
        for (InlongGroupEntity groupEntity : groupMapper.selectByCondition(request)) {
            // only the person in charges can query
            if (!opInfo.getRoles().contains(UserTypeEnum.ADMIN.name())) {
                List<String> inCharges = Arrays.asList(groupEntity.getInCharges().split(InlongConstants.COMMA));
                if (!inCharges.contains(opInfo.getName())) {
                    continue;
                }
            }
            filterGroupEntities.add(groupEntity);
        }
        List<InlongGroupBriefInfo> briefInfos =
                CommonBeanUtils.copyListProperties(filterGroupEntities, InlongGroupBriefInfo::new);
        // list all related sources
        if (request.isListSources() && CollectionUtils.isNotEmpty(briefInfos)) {
            Set<String> groupIds = briefInfos.stream().map(InlongGroupBriefInfo::getInlongGroupId)
                    .collect(Collectors.toSet());
            List<StreamSourceEntity> sourceEntities = streamSourceMapper.selectByGroupIds(new ArrayList<>(groupIds));
            Map<String, List<StreamSource>> sourceMap = Maps.newHashMap();
            sourceEntities.forEach(sourceEntity -> {
                StreamSourceOperator operation = sourceOperatorFactory.getInstance(sourceEntity.getSourceType());
                StreamSource source = operation.getFromEntity(sourceEntity);
                sourceMap.computeIfAbsent(sourceEntity.getInlongGroupId(), k -> Lists.newArrayList()).add(source);
            });
            briefInfos.forEach(group -> {
                List<StreamSource> sources = sourceMap.getOrDefault(group.getInlongGroupId(), Lists.newArrayList());
                group.setStreamSources(sources);
            });
        }
        return briefInfos;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRES_NEW)
    public String update(InlongGroupRequest request, String operator) {
        LOGGER.debug("begin to update inlong group={} by user={}", request, operator);

        String groupId = request.getInlongGroupId();
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        if (entity == null) {
            LOGGER.error("inlong group not found by groupId={}", groupId);
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }
        if (!Objects.equals(entity.getVersion(), request.getVersion())) {
            LOGGER.error("inlong group has already updated with groupId={}, curVersion={}",
                    groupId, request.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        // check whether the current status can be modified
        doUpdateCheck(entity, request, operator);

        request.setEnableZookeeper(enableZookeeper ? InlongConstants.ENABLE_ZK : InlongConstants.DISABLE_ZK);
        InlongGroupOperator instance = groupOperatorFactory.getInstance(request.getMqType());
        instance.updateOpt(request, operator);

        // save ext info
        this.saveOrUpdateExt(groupId, request.getExtList());

        LOGGER.info("success to update inlong group for groupId={} by user={}", groupId, operator);
        return groupId;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRES_NEW)
    public String update(InlongGroupRequest request, UserInfo opInfo) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER,
                    "group query request cannot be empty");
        }
        // check operator info
        if (opInfo == null) {
            throw new BusinessException(ErrorCodeEnum.LOGIN_USER_EMPTY);
        }
        String groupId = request.getInlongGroupId();
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }
        if (!Objects.equals(entity.getVersion(), request.getVersion())) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        // only the person in charges can query
        if (!opInfo.getRoles().contains(UserTypeEnum.ADMIN.name())) {
            List<String> inCharges = Arrays.asList(entity.getInCharges().split(InlongConstants.COMMA));
            if (!inCharges.contains(opInfo.getName())) {
                throw new BusinessException(ErrorCodeEnum.GROUP_PERMISSION_DENIED);
            }
        }
        // check whether the current status supports modification
        GroupStatus curStatus = GroupStatus.forCode(entity.getStatus());
        if (GroupStatus.notAllowedUpdate(curStatus)) {
            throw new BusinessException(ErrorCodeEnum.GROUP_UPDATE_NOT_ALLOWED,
                    String.format("Current status=%s is not allowed to update", curStatus));
        }
        // mq type cannot be changed
        if (!entity.getMqType().equals(request.getMqType()) && !GroupStatus.allowedUpdateMQ(curStatus)) {
            throw new BusinessException(ErrorCodeEnum.GROUP_UPDATE_NOT_ALLOWED,
                    String.format("Current status=%s is not allowed to update MQ type", curStatus));
        }
        // update record
        request.setEnableZookeeper(enableZookeeper ? InlongConstants.ENABLE_ZK : InlongConstants.DISABLE_ZK);
        InlongGroupOperator instance = groupOperatorFactory.getInstance(request.getMqType());
        instance.updateOpt(request, opInfo.getName());
        // save ext info
        this.saveOrUpdateExt(groupId, request.getExtList());
        return groupId;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRES_NEW)
    public Boolean updateStatus(String groupId, Integer status, String operator) {
        LOGGER.info("begin to update group status to [{}] for groupId={} by user={}", status, groupId, operator);
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        InlongGroupEntity entity = groupMapper.selectByGroupIdForUpdate(groupId);
        if (entity == null) {
            LOGGER.error("inlong group not found by groupId={}", groupId);
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }

        GroupStatus curState = GroupStatus.forCode(entity.getStatus());
        GroupStatus nextState = GroupStatus.forCode(status);
        if (GroupStatus.notAllowedTransition(curState, nextState)) {
            String errorMsg = String.format("Current status=%s is not allowed to transfer to state=%s",
                    curState, nextState);
            LOGGER.error(errorMsg);
            throw new BusinessException(errorMsg);
        }

        groupMapper.updateStatus(groupId, status, operator);
        LOGGER.info("success to update group status to [{}] for groupId={} by user={}", status, groupId, operator);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public void updateAfterApprove(InlongGroupApproveRequest approveRequest, String operator) {
        LOGGER.debug("begin to update inlong group after approve={}", approveRequest);
        String groupId = approveRequest.getInlongGroupId();

        // only the [TO_BE_APPROVAL] status allowed the passing operation
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        if (entity == null) {
            throw new BusinessException("inlong group not found with group id=" + groupId);
        }
        if (!Objects.equals(GroupStatus.TO_BE_APPROVAL.getCode(), entity.getStatus())) {
            throw new BusinessException("inlong group status [wait_approval] not allowed to approve again");
        }

        // bind cluster tag and update status to [GROUP_APPROVE_PASSED]
        if (StringUtils.isNotBlank(approveRequest.getInlongClusterTag())) {
            entity.setInlongGroupId(groupId);
            entity.setInlongClusterTag(approveRequest.getInlongClusterTag());
            entity.setStatus(GroupStatus.APPROVE_PASSED.getCode());
            if (approveRequest.getDataReportType() != null
                    && !Objects.equals(approveRequest.getDataReportType(), entity.getDataReportType())) {
                entity.setDataReportType(approveRequest.getDataReportType());
            }
            entity.setModifier(operator);
            int rowCount = groupMapper.updateByIdentifierSelective(entity);
            if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
                LOGGER.error("inlong group has already updated with group id={}, curVersion={}",
                        groupId, entity.getVersion());
                throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
            }
        } else {
            this.updateStatus(groupId, GroupStatus.APPROVE_PASSED.getCode(), operator);
        }

        LOGGER.info("success to update inlong group status after approve for groupId={}", groupId);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void saveOrUpdateExt(String groupId, List<InlongGroupExtInfo> exts) {
        if (CollectionUtils.isEmpty(exts)) {
            return;
        }
        List<InlongGroupExtEntity> entityList =
                CommonBeanUtils.copyListProperties(exts, InlongGroupExtEntity::new);
        for (InlongGroupExtEntity entity : entityList) {
            entity.setInlongGroupId(groupId);
        }
        groupExtMapper.insertOnDuplicateKeyUpdate(entityList);
    }

    @Override
    public List<InlongGroupTopicInfo> listTopics(InlongGroupTopicRequest request) {
        LOGGER.info("start to list group topic infos, request={}", request);
        Preconditions.checkNotEmpty(request.getClusterTag(), "cluster tag should not be empty");
        List<InlongGroupEntity> groupEntities = groupMapper.selectByTopicRequest(request);
        List<InlongGroupTopicInfo> topicInfos = new ArrayList<>();
        for (InlongGroupEntity entity : groupEntities) {
            topicInfos.add(this.getTopic(entity.getInlongGroupId()));
        }
        LOGGER.info("success list group topic infos under clusterTag={}, size={}",
                request.getClusterTag(), topicInfos.size());
        return topicInfos;
    }

    @Override
    public InlongGroupInfo doDeleteCheck(String groupId, String operator) {
        InlongGroupInfo groupInfo = this.get(groupId);
        // only the person in charges can update
        List<String> inCharges = Arrays.asList(groupInfo.getInCharges().split(InlongConstants.COMMA));
        if (!inCharges.contains(operator)) {
            throw new BusinessException(ErrorCodeEnum.GROUP_PERMISSION_DENIED,
                    String.format("user [%s] has no privilege for the inlong group", operator));
        }
        // determine whether the current status can be deleted
        GroupStatus curState = GroupStatus.forCode(groupInfo.getStatus());
        if (GroupStatus.notAllowedTransition(curState, GroupStatus.DELETING)) {
            throw new BusinessException(ErrorCodeEnum.GROUP_DELETE_NOT_ALLOWED,
                    String.format("current group status=%s was not allowed to delete", curState));
        }

        // If the status not allowed deleting directly, you need to delete the related "inlong_stream" first,
        // otherwise, all associated info will be logically deleted.
        if (GroupStatus.deleteStreamFirst(curState)) {
            int count = streamService.selectCountByGroupId(groupId);
            if (count >= 1) {
                throw new BusinessException(ErrorCodeEnum.GROUP_DELETE_HAS_STREAM,
                        String.format("groupId=%s have [%s] inlong streams, deleted failed", groupId, count));
            }
        }

        return groupInfo;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean delete(String groupId, String operator) {
        LOGGER.info("begin to delete inlong group for groupId={} by user={}", groupId, operator);
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        Preconditions.checkNotNull(entity, ErrorCodeEnum.GROUP_NOT_FOUND.getMessage());

        // before deleting an inlong group, delete all inlong streams, sources, sinks, and other info under it
        if (GroupStatus.allowedDeleteSubInfos(GroupStatus.forCode(entity.getStatus()))) {
            streamService.logicDeleteAll(groupId, operator);
        }

        entity.setIsDeleted(entity.getId());
        entity.setStatus(GroupStatus.DELETED.getCode());
        entity.setModifier(operator);
        int rowCount = groupMapper.updateByIdentifierSelective(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            LOGGER.error("inlong group has already updated for groupId={} curVersion={}", groupId, entity.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }

        // logically delete the associated extension info
        groupExtMapper.logicDeleteAllByGroupId(groupId);

        LOGGER.info("success to delete group and group ext property for groupId={} by user={}", groupId, operator);
        return true;
    }

    private BaseSortConf buildSortConfig(List<InlongGroupExtInfo> extInfos) {
        Map<String, String> extMap = new HashMap<>();
        extInfos.forEach(extInfo -> extMap.put(extInfo.getKeyName(), extInfo.getKeyValue()));
        String type = extMap.get(InlongConstants.SORT_TYPE);
        if (StringUtils.isBlank(type)) {
            return null;
        }
        SortType sortType = SortType.forType(type);
        switch (sortType) {
            case FLINK:
                return createFlinkSortConfig(extMap);
            case USER_DEFINED:
                return createUserDefinedSortConfig(extMap);
            default:
                LOGGER.warn("unsupported sort config for sortType: {}", sortType);
                return null;
        }
    }

    private FlinkSortConf createFlinkSortConfig(Map<String, String> extMap) {
        FlinkSortConf sortConf = new FlinkSortConf();
        sortConf.setServiceUrl(extMap.get(InlongConstants.SORT_URL));
        String properties = extMap.get(InlongConstants.SORT_PROPERTIES);
        if (StringUtils.isNotBlank(properties)) {
            sortConf.setProperties(JsonUtils.parseObject(properties,
                    new TypeReference<Map<String, String>>() {
                    }));
        } else {
            sortConf.setProperties(Maps.newHashMap());
        }
        String authenticationType = extMap.get(InlongConstants.SORT_AUTHENTICATION_TYPE);
        if (StringUtils.isNotBlank(authenticationType)) {
            AuthType authType = AuthType.forType(authenticationType);
            Preconditions.checkTrue(authType == AuthType.SECRET_AND_TOKEN,
                    "Only support SECRET_AND_TOKEN for flink sort auth");
            String authentication = extMap.get(InlongConstants.SORT_AUTHENTICATION);
            Map<String, String> authProperties = JsonUtils.parseObject(authentication,
                    new TypeReference<Map<String, String>>() {
                    });
            SecretTokenAuthentication secretTokenAuthentication = new SecretTokenAuthentication();
            secretTokenAuthentication.configure(authProperties);
            sortConf.setAuthentication(secretTokenAuthentication);
        }
        return sortConf;
    }

    private UserDefinedSortConf createUserDefinedSortConfig(Map<String, String> extMap) {
        UserDefinedSortConf sortConf = new UserDefinedSortConf();
        String sortName = extMap.get(InlongConstants.SORT_NAME);
        sortConf.setSortName(sortName);
        String properties = extMap.get(InlongConstants.SORT_PROPERTIES);
        if (StringUtils.isNotBlank(properties)) {
            sortConf.setProperties(JsonUtils.parseObject(properties,
                    new TypeReference<Map<String, String>>() {
                    }));
        } else {
            sortConf.setProperties(Maps.newHashMap());
        }
        return sortConf;
    }
}
