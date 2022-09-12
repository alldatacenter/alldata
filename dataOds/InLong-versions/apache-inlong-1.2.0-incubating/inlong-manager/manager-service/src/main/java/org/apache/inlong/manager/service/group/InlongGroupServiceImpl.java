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

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.SourceType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.group.InlongGroupApproveRequest;
import org.apache.inlong.manager.common.pojo.group.InlongGroupCountResponse;
import org.apache.inlong.manager.common.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupListResponse;
import org.apache.inlong.manager.common.pojo.group.InlongGroupPageRequest;
import org.apache.inlong.manager.common.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.common.pojo.group.InlongGroupTopicInfo;
import org.apache.inlong.manager.common.pojo.source.SourceListResponse;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.InlongGroupExtEntity;
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupExtEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSourceEntityMapper;
import org.apache.inlong.manager.service.core.InlongStreamService;
import org.apache.inlong.manager.service.source.SourceOperationFactory;
import org.apache.inlong.manager.service.source.StreamSourceOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Inlong group service layer implementation
 */
@Service
public class InlongGroupServiceImpl implements InlongGroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongGroupServiceImpl.class);
    private static final Integer MAX_PAGE_SIZE = 100;

    @Autowired
    private InlongGroupOperatorFactory groupOperatorFactory;
    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private InlongGroupExtEntityMapper groupExtMapper;
    @Autowired
    private StreamSourceEntityMapper streamSourceMapper;
    @Autowired
    private SourceOperationFactory sourceOperationFactory;
    @Autowired
    private InlongStreamService streamService;

    /**
     * Check whether modification is supported under the current group status, and which fields can be modified.
     *
     * @param entity original inlong group entity
     * @param request request of updated
     * @param operator current operator
     */
    private static void checkGroupCanUpdate(InlongGroupEntity entity, InlongGroupRequest request, String operator) {
        if (entity == null || request == null) {
            return;
        }

        // only the person in charges can update
        List<String> inCharges = Arrays.asList(entity.getInCharges().split(","));
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
        if (!entity.getMqType().equals(request.getMqType()) && GroupStatus.notAllowedUpdateMQ(curStatus)) {
            String errMsg = String.format("Current status=%s is not allowed to update MQ type", curStatus);
            LOGGER.error(errMsg);
            throw new BusinessException(ErrorCodeEnum.GROUP_UPDATE_NOT_ALLOWED, errMsg);
        }
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public String save(InlongGroupRequest request, String operator) {
        LOGGER.debug("begin to save inlong group={} by user={}", request, operator);
        Preconditions.checkNotNull(request, "inlong group request cannot be empty");
        request.checkParams();

        String groupId = request.getInlongGroupId();
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        if (entity != null) {
            LOGGER.error("groupId {} has already exists", groupId);
            throw new BusinessException(ErrorCodeEnum.GROUP_DUPLICATE);
        }

        InlongGroupOperator instance = groupOperatorFactory.getInstance(request.getMqType());
        groupId = instance.saveOpt(request, operator);

        // save ext info
        this.saveOrUpdateExt(groupId, request.getExtList());

        LOGGER.info("success to save inlong group for groupId={} by user={}", groupId, operator);
        return groupId;
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

        LOGGER.debug("success to get inlong group for groupId={}", groupId);
        return groupInfo;
    }

    @Override
    public PageInfo<InlongGroupListResponse> listByPage(InlongGroupPageRequest request) {
        if (request.getPageSize() > MAX_PAGE_SIZE) {
            LOGGER.warn("list group info, but page size is {}, change to {}", request.getPageSize(), MAX_PAGE_SIZE);
            request.setPageSize(MAX_PAGE_SIZE);
        }
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<InlongGroupEntity> entityPage = (Page<InlongGroupEntity>) groupMapper.selectByCondition(request);

        List<InlongGroupListResponse> groupResponseList = CommonBeanUtils.copyListProperties(entityPage,
                InlongGroupListResponse::new);

        // need to list all related sources
        if (request.isListSources() && CollectionUtils.isNotEmpty(groupResponseList)) {
            Set<String> groupIds = groupResponseList.stream().map(InlongGroupListResponse::getInlongGroupId)
                    .collect(Collectors.toSet());
            List<StreamSourceEntity> sourceEntities = streamSourceMapper.selectByGroupIds(new ArrayList<>(groupIds));
            Map<String, List<SourceListResponse>> sourceMap = Maps.newHashMap();
            sourceEntities.forEach(sourceEntity -> {
                SourceType sourceType = SourceType.forType(sourceEntity.getSourceType());
                StreamSourceOperation operation = sourceOperationFactory.getInstance(sourceType);
                SourceListResponse sourceListResponse = operation.getFromEntity(sourceEntity, SourceListResponse::new);
                sourceMap.computeIfAbsent(sourceEntity.getInlongGroupId(), k -> Lists.newArrayList())
                        .add(sourceListResponse);
            });
            groupResponseList.forEach(group -> {
                List<SourceListResponse> sourceListResponses = sourceMap.getOrDefault(group.getInlongGroupId(),
                        Lists.newArrayList());
                group.setSourceResponses(sourceListResponses);
            });
        }
        PageInfo<InlongGroupListResponse> page = new PageInfo<>(groupResponseList);
        page.setTotal(entityPage.getTotal());
        LOGGER.debug("success to list inlong group for {}", request);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ,
            propagation = Propagation.REQUIRES_NEW)
    public String update(InlongGroupRequest request, String operator) {
        LOGGER.debug("begin to update inlong group={} by user={}", request, operator);
        Preconditions.checkNotNull(request, "inlong group request cannot be empty");
        request.checkParams();

        String groupId = request.getInlongGroupId();
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        if (entity == null) {
            LOGGER.error("inlong group not found by groupId={}", groupId);
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }

        // check whether the current status can be modified
        checkGroupCanUpdate(entity, request, operator);

        InlongGroupOperator instance = groupOperatorFactory.getInstance(request.getMqType());
        instance.updateOpt(request, operator);

        // save ext info
        this.saveOrUpdateExt(groupId, request.getExtList());

        LOGGER.info("success to update inlong group for groupId={} by user={}", groupId, operator);
        return groupId;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ,
            propagation = Propagation.REQUIRES_NEW)
    public boolean updateStatus(String groupId, Integer status, String operator) {
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

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public boolean delete(String groupId, String operator) {
        LOGGER.info("begin to delete inlong group for groupId={} by user={}", groupId, operator);
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());

        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        if (entity == null) {
            LOGGER.error("inlong group not found by groupId={}", groupId);
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }

        // Determine whether the current status can be deleted
        GroupStatus curState = GroupStatus.forCode(entity.getStatus());
        if (GroupStatus.notAllowedTransition(curState, GroupStatus.DELETED)) {
            String errMsg = String.format("Current status=%s was not allowed to delete", curState);
            LOGGER.error(errMsg);
            throw new BusinessException(ErrorCodeEnum.GROUP_DELETE_NOT_ALLOWED, errMsg);
        }

        /*
         If the status allowed logic delete, all associated data can be logically deleted.
         In other status, you need to delete the related "inlong stream" first.
         When deleting a related inlong stream, you also need to check whether
         there are some related "stream source" and "stream sink"
         */
        if (GroupStatus.allowedLogicDelete(curState)) {
            streamService.logicDeleteAll(entity.getInlongGroupId(), operator);
        } else {
            int count = streamService.selectCountByGroupId(groupId);
            if (count >= 1) {
                LOGGER.error("groupId={} have [{}] inlong streams, deleted failed", groupId, count);
                throw new BusinessException(ErrorCodeEnum.GROUP_HAS_STREAM);
            }
        }

        // update the group after deleting related info
        entity.setIsDeleted(entity.getId());
        entity.setStatus(GroupStatus.DELETED.getCode());
        entity.setModifier(operator);
        groupMapper.updateByIdentifierSelective(entity);

        // logically delete the associated extension info
        groupExtMapper.logicDeleteAllByGroupId(groupId);

        LOGGER.info("success to delete group and group ext property for groupId={} by user={}", groupId, operator);
        return true;
    }

    @Override
    public Boolean exist(String groupId) {
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        LOGGER.debug("success to check inlong group {}, exist? {}", groupId, entity != null);
        return entity != null;
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

        InlongGroupOperator instance = groupOperatorFactory.getInstance(groupInfo.getMqType());
        InlongGroupTopicInfo topicInfo = instance.getTopic(groupInfo);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("success to get topic for groupId={}, result=" + topicInfo, groupId);
        }
        return topicInfo;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    public boolean updateAfterApprove(InlongGroupApproveRequest approveInfo, String operator) {
        LOGGER.debug("begin to update inlong group after approve={}", approveInfo);

        // Save the dataSchema, Topic and other information of the inlong group
        Preconditions.checkNotNull(approveInfo, "InlongGroupApproveRequest is empty");
        String groupId = approveInfo.getInlongGroupId();
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        String mqType = approveInfo.getMqType();
        Preconditions.checkNotNull(mqType, "MQ type cannot by empty");

        // Update status to [GROUP_APPROVE_PASSED]
        this.updateStatus(groupId, GroupStatus.APPROVE_PASSED.getCode(), operator);

        // update other info for inlong group after approve
        if (StringUtils.isNotBlank(approveInfo.getInlongClusterTag())) {
            InlongGroupEntity entity = new InlongGroupEntity();
            entity.setInlongGroupId(approveInfo.getInlongGroupId());
            entity.setInlongClusterTag(approveInfo.getInlongClusterTag());
            entity.setModifier(operator);
            groupMapper.updateByIdentifierSelective(entity);
        }

        LOGGER.info("success to update inlong group status after approve for groupId={}", groupId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void saveOrUpdateExt(String groupId, List<InlongGroupExtInfo> exts) {
        LOGGER.info("begin to save or update inlong group ext info, groupId={}, ext={}", groupId, exts);
        if (CollectionUtils.isEmpty(exts)) {
            return;
        }

        List<InlongGroupExtEntity> entityList = CommonBeanUtils.copyListProperties(exts, InlongGroupExtEntity::new);
        for (InlongGroupExtEntity entity : entityList) {
            entity.setInlongGroupId(groupId);
        }
        groupExtMapper.insertOnDuplicateKeyUpdate(entityList);
        LOGGER.info("success to save or update inlong group ext for groupId={}", groupId);
    }

}
