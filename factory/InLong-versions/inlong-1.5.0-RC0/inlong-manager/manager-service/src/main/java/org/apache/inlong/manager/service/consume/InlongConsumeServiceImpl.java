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

package org.apache.inlong.manager.service.consume;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ConsumeStatus;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongConsumeEntity;
import org.apache.inlong.manager.dao.mapper.InlongConsumeEntityMapper;
import org.apache.inlong.manager.pojo.common.CountInfo;
import org.apache.inlong.manager.pojo.common.OrderFieldEnum;
import org.apache.inlong.manager.pojo.common.OrderTypeEnum;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.consume.InlongConsumeBriefInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeCountInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumePageRequest;
import org.apache.inlong.manager.pojo.consume.InlongConsumeRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.inlong.manager.pojo.common.PageRequest.MAX_PAGE_SIZE;

/**
 * Inlong consume service layer implementation
 */
@Service
public class InlongConsumeServiceImpl implements InlongConsumeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongConsumeServiceImpl.class);
    private static final String AUTO_CREATE_MSG = "auto_create_by_system";

    @Autowired
    private InlongConsumeEntityMapper consumeMapper;
    @Autowired
    private InlongConsumeOperatorFactory consumeOperatorFactory;

    @Override
    public Integer save(InlongConsumeRequest request, String operator) {
        LOGGER.debug("begin to save inlong consume={} by user={}", request, operator);
        Preconditions.checkNotNull(request, "inlong consume request cannot be null");
        Preconditions.checkNotNull(request.getTopic(), "inlong consume topic cannot be null");
        String consumerGroup = request.getConsumerGroup();
        Preconditions.checkNotNull(consumerGroup, "inlong consume topic cannot be null");
        if (consumerGroupExists(consumerGroup, request.getId())) {
            throw new BusinessException(String.format("consumer group %s already exist", consumerGroup));
        }

        InlongConsumeOperator consumeOperator = consumeOperatorFactory.getInstance(request.getMqType());
        Integer id = consumeOperator.saveOpt(request, operator);

        LOGGER.info("success to save inlong consume for consumer group={} by user={}", consumerGroup, operator);
        return id;
    }

    @Override
    public Integer saveBySystem(InlongGroupInfo groupInfo, String topic, String consumerGroup) {
        String groupId = groupInfo.getInlongGroupId();
        InlongConsumeEntity existEntity = consumeMapper.selectExists(consumerGroup, topic, groupId);
        if (existEntity != null) {
            LOGGER.warn("inlong consume already exists for groupId={} topic={} consumerGroup={}, skip to create",
                    groupId, topic, consumerGroup);
            return existEntity.getId();
        }

        LOGGER.debug("begin to save inlong consume for groupId={} topic={} group={}", groupId, topic, consumerGroup);
        InlongConsumeEntity entity = new InlongConsumeEntity();
        entity.setConsumerGroup(consumerGroup);
        entity.setDescription(AUTO_CREATE_MSG);
        entity.setMqType(groupInfo.getMqType());
        entity.setTopic(topic);
        entity.setInlongGroupId(groupId);
        entity.setFilterEnabled(0);

        entity.setInCharges(groupInfo.getInCharges());
        entity.setStatus(ConsumeStatus.APPROVE_PASSED.getCode());
        String operator = groupInfo.getCreator();
        entity.setCreator(operator);
        entity.setModifier(operator);

        consumeMapper.insert(entity);
        LOGGER.debug("success save inlong consume for groupId={} topic={} group={}", groupId, topic, consumerGroup);
        return entity.getId();
    }

    @Override
    public boolean consumerGroupExists(String consumerGroup, Integer excludeSelfId) {
        InlongConsumePageRequest request = InlongConsumePageRequest.builder()
                .consumerGroup(consumerGroup)
                .isAdminRole(true)
                .build();
        List<InlongConsumeEntity> result = consumeMapper.selectByCondition(request);
        if (excludeSelfId != null) {
            result = result.stream()
                    .filter(consumer -> !excludeSelfId.equals(consumer.getId()))
                    .collect(Collectors.toList());
        }
        return CollectionUtils.isNotEmpty(result);
    }

    @Override
    public InlongConsumeInfo get(Integer id) {
        Preconditions.checkNotNull(id, "inlong consume id cannot be null");
        InlongConsumeEntity entity = consumeMapper.selectById(id);
        if (entity == null) {
            LOGGER.error("inlong consume not found with id={}", id);
            throw new BusinessException(ErrorCodeEnum.CONSUME_NOT_FOUND);
        }

        InlongConsumeOperator consumeOperator = consumeOperatorFactory.getInstance(entity.getMqType());
        InlongConsumeInfo consumeInfo = consumeOperator.getFromEntity(entity);

        LOGGER.debug("success to get inlong consume for id={}", id);
        return consumeInfo;
    }

    @Override
    public InlongConsumeCountInfo countStatus(String username) {
        List<CountInfo> countInfoList = consumeMapper.countByUser(username);
        InlongConsumeCountInfo result = new InlongConsumeCountInfo();
        for (CountInfo countInfo : countInfoList) {
            int status = Integer.parseInt(countInfo.getKey());
            int count = countInfo.getValue();
            result.setTotalCount(result.getTotalCount() + count);
            if (status == ConsumeStatus.TO_BE_SUBMIT.getCode()) {
                result.setWaitAssignCount(result.getWaitAssignCount() + count);
            } else if (status == ConsumeStatus.TO_BE_APPROVAL.getCode()) {
                result.setWaitApproveCount(result.getWaitApproveCount() + count);
            } else if (status == ConsumeStatus.APPROVE_REJECTED.getCode()) {
                result.setRejectCount(result.getRejectCount() + count);
            }
        }

        LOGGER.debug("success to count inlong consume for user={}", username);
        return result;
    }

    @Override
    public PageResult<InlongConsumeBriefInfo> list(InlongConsumePageRequest request) {
        if (request.getPageSize() > MAX_PAGE_SIZE) {
            LOGGER.warn("list inlong consumes, change page size from {} to {}", request.getPageSize(), MAX_PAGE_SIZE);
            request.setPageSize(MAX_PAGE_SIZE);
        }
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        OrderFieldEnum.checkOrderField(request);
        OrderTypeEnum.checkOrderType(request);
        Page<InlongConsumeBriefInfo> briefInfos = (Page<InlongConsumeBriefInfo>) consumeMapper.selectBriefList(request);
        PageResult<InlongConsumeBriefInfo> pageResult = new PageResult<>(briefInfos,
                briefInfos.getTotal(), briefInfos.getPageNum(), briefInfos.getPageSize());

        LOGGER.debug("success to list inlong consume for {}", request);
        return pageResult;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRES_NEW)
    public Integer update(InlongConsumeRequest request, String operator) {
        LOGGER.debug("begin to update inlong consume={} by user={}", request, operator);
        Preconditions.checkNotNull(request, "inlong consume request cannot be null");

        // check if it can be modified
        Integer consumeId = request.getId();
        InlongConsumeEntity existEntity = consumeMapper.selectById(consumeId);
        Preconditions.checkNotNull(existEntity, "inlong consume not exist with id " + consumeId);
        Preconditions.checkTrue(existEntity.getInCharges().contains(operator),
                "operator" + operator + " has no privilege for the inlong consume");

        if (!Objects.equals(existEntity.getVersion(), request.getVersion())) {
            LOGGER.error(String.format("inlong consume has already updated, id=%s, curVersion=%s",
                    existEntity.getId(), request.getVersion()));
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }

        ConsumeStatus consumeStatus = ConsumeStatus.forCode(existEntity.getStatus());
        Preconditions.checkTrue(ConsumeStatus.allowedUpdate(consumeStatus),
                "inlong consume not allowed update when status is " + consumeStatus.name());

        InlongConsumeOperator consumeOperator = consumeOperatorFactory.getInstance(request.getMqType());
        consumeOperator.updateOpt(request, operator);

        LOGGER.info("success to update inlong consume={} by user={}", request, operator);
        return consumeId;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRES_NEW)
    public Boolean updateStatus(Integer id, Integer status, String operator) {
        LOGGER.info("begin to update consume status to [{}] for id={} by user={}", status, id, operator);
        Preconditions.checkNotNull(id, ErrorCodeEnum.ID_IS_EMPTY.getMessage());
        InlongConsumeEntity entity = consumeMapper.selectById(id);
        if (entity == null) {
            LOGGER.error("inlong consume not found by id={}", id);
            throw new BusinessException(ErrorCodeEnum.CONSUME_NOT_FOUND);
        }

        ConsumeStatus curStatus = ConsumeStatus.forCode(entity.getStatus());
        ConsumeStatus nextStatus = ConsumeStatus.forCode(status);
        if (ConsumeStatus.notAllowedTransfer(curStatus, nextStatus)) {
            String errorMsg = String.format("Current status=%s cannot transfer to status=%s", curStatus, nextStatus);
            LOGGER.error(errorMsg);
            throw new BusinessException(errorMsg);
        }

        consumeMapper.updateStatus(id, status, operator);
        LOGGER.info("success to update consume status to [{}] for id={} by user={}", status, id, operator);
        return true;
    }

    @Override
    public Boolean delete(Integer id, String operator) {
        LOGGER.info("begin to delete inlong consume for id={} by user={}", id, operator);
        Preconditions.checkNotNull(id, "inlong consume id cannot be null");
        InlongConsumeEntity entity = consumeMapper.selectById(id);
        Preconditions.checkNotNull(entity, "inlong consume not exist with id " + id);

        entity.setIsDeleted(id);
        entity.setStatus(ConsumeStatus.DELETED.getCode());
        entity.setModifier(operator);

        int rowCount = consumeMapper.updateByIdSelective(entity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            LOGGER.error("inlong consume has already updated with id={}, curVersion={}", id, entity.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }

        LOGGER.info("success to delete inlong consume for id={} by user={}", id, operator);
        return true;
    }

}
