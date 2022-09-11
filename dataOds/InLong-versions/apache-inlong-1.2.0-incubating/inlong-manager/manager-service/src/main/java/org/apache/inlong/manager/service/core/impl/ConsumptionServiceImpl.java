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
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.beans.ClusterBean;
import org.apache.inlong.manager.common.enums.ConsumptionStatus;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GlobalConstants;
import org.apache.inlong.manager.common.enums.MQType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.common.CountInfo;
import org.apache.inlong.manager.common.pojo.consumption.ConsumptionInfo;
import org.apache.inlong.manager.common.pojo.consumption.ConsumptionListVo;
import org.apache.inlong.manager.common.pojo.consumption.ConsumptionMqExtBase;
import org.apache.inlong.manager.common.pojo.consumption.ConsumptionPulsarInfo;
import org.apache.inlong.manager.common.pojo.consumption.ConsumptionQuery;
import org.apache.inlong.manager.common.pojo.consumption.ConsumptionSummary;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupTopicInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamTopicInfo;
import org.apache.inlong.manager.common.pojo.user.UserRoleCode;
import org.apache.inlong.manager.common.settings.InlongGroupSettings;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.LoginUserUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.ConsumptionEntity;
import org.apache.inlong.manager.dao.entity.ConsumptionPulsarEntity;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.mapper.ConsumptionEntityMapper;
import org.apache.inlong.manager.dao.mapper.ConsumptionPulsarEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.service.core.ConsumptionService;
import org.apache.inlong.manager.service.core.InlongStreamService;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data consumption service
 */
@Slf4j
@Service
public class ConsumptionServiceImpl implements ConsumptionService {

    private static final String PREFIX_DLQ = "dlq"; // prefix of the Topic of the dead letter queue

    private static final String PREFIX_RLQ = "rlq"; // prefix of the Topic of the retry letter queue

    @Autowired
    private ClusterBean clusterBean;
    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private ConsumptionEntityMapper consumptionMapper;
    @Autowired
    private ConsumptionPulsarEntityMapper consumptionPulsarMapper;
    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private InlongStreamService streamService;

    @Override
    public ConsumptionSummary getSummary(ConsumptionQuery query) {
        Map<String, Integer> countMap = consumptionMapper.countByQuery(query)
                .stream()
                .collect(Collectors.toMap(CountInfo::getKey, CountInfo::getValue));

        return ConsumptionSummary.builder()
                .totalCount(countMap.values().stream().mapToInt(c -> c).sum())
                .waitingAssignCount(countMap.getOrDefault(ConsumptionStatus.WAIT_ASSIGN.getStatus() + "", 0))
                .waitingApproveCount(countMap.getOrDefault(ConsumptionStatus.WAIT_APPROVE.getStatus() + "", 0))
                .rejectedCount(countMap.getOrDefault(ConsumptionStatus.REJECTED.getStatus() + "", 0)).build();
    }

    @Override
    public PageInfo<ConsumptionListVo> list(ConsumptionQuery query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        query.setIsAdminRole(LoginUserUtils.getLoginUserDetail().getRoles().contains(UserRoleCode.ADMIN));
        Page<ConsumptionEntity> pageResult = (Page<ConsumptionEntity>) consumptionMapper.listByQuery(query);
        PageInfo<ConsumptionListVo> pageInfo = pageResult
                .toPageInfo(entity -> CommonBeanUtils.copyProperties(entity, ConsumptionListVo::new));
        pageInfo.setTotal(pageResult.getTotal());
        return pageInfo;
    }

    @Override
    public ConsumptionInfo get(Integer id) {
        Preconditions.checkNotNull(id, "consumption id cannot be null");
        ConsumptionEntity entity = consumptionMapper.selectByPrimaryKey(id);
        Preconditions.checkNotNull(entity, "consumption not exist with id:" + id);

        ConsumptionInfo info = CommonBeanUtils.copyProperties(entity, ConsumptionInfo::new);

        MQType mqType = MQType.forType(info.getMqType());
        if (mqType == MQType.PULSAR || mqType == MQType.TDMQ_PULSAR) {
            ConsumptionPulsarEntity pulsarEntity = consumptionPulsarMapper.selectByConsumptionId(info.getId());
            Preconditions.checkNotNull(pulsarEntity, "Pulsar consumption cannot be empty, as the middleware is Pulsar");
            ConsumptionPulsarInfo pulsarInfo = CommonBeanUtils.copyProperties(pulsarEntity, ConsumptionPulsarInfo::new);
            info.setMqExtInfo(pulsarInfo);

            info.setTopic(this.getFullPulsarTopic(info.getInlongGroupId(), info.getTopic()));
        }

        return info;
    }

    @Override
    public boolean isConsumerGroupExists(String consumerGroup, Integer excludeSelfId) {
        ConsumptionQuery consumptionQuery = new ConsumptionQuery();
        consumptionQuery.setConsumerGroup(consumerGroup);
        consumptionQuery.setIsAdminRole(true);
        List<ConsumptionEntity> result = consumptionMapper.listByQuery(consumptionQuery);
        if (excludeSelfId != null) {
            result = result.stream().filter(consumer -> !excludeSelfId.equals(consumer.getId()))
                    .collect(Collectors.toList());
        }
        return !CollectionUtils.isEmpty(result);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Integer save(ConsumptionInfo info, String operator) {
        fullConsumptionInfo(info);
        Date now = new Date();
        ConsumptionEntity entity = this.saveConsumption(info, operator, now);
        MQType mqType = MQType.forType(entity.getMqType());
        if (mqType == MQType.PULSAR || mqType == MQType.TDMQ_PULSAR) {
            savePulsarInfo(info.getMqExtInfo(), entity);
        }

        return entity.getId();
    }

    /**
     * Save Pulsar consumption info
     */
    private void savePulsarInfo(ConsumptionMqExtBase mqExtBase, ConsumptionEntity entity) {
        Preconditions.checkNotNull(mqExtBase, "Pulsar info cannot be empty, as the middleware is Pulsar");
        // If it is transmitted from the web without specifying consumptionpulsarinfo,
        // the mqextbase is not consumptionpulsarinfo and cannot be converted directly
        ConsumptionPulsarInfo pulsarInfo;
        if (mqExtBase instanceof ConsumptionPulsarInfo) {
            pulsarInfo = (ConsumptionPulsarInfo) mqExtBase;
        } else {
            pulsarInfo = new ConsumptionPulsarInfo();
        }

        // Prerequisite for RLQ to be turned on: DLQ must be turned on
        boolean dlqEnable = (pulsarInfo.getIsDlq() != null && pulsarInfo.getIsDlq() == 1);
        boolean rlqEnable = (pulsarInfo.getIsRlq() != null && pulsarInfo.getIsRlq() == 1);
        if (rlqEnable && !dlqEnable) {
            throw new BusinessException(ErrorCodeEnum.PULSAR_DLQ_RLQ_ERROR);
        }

        // When saving, the DLQ / RLQ under the same groupId cannot be repeated;
        // when closing, delete the related configuration
        String groupId = entity.getInlongGroupId();
        if (dlqEnable) {
            String dlqTopic = PREFIX_DLQ + "_" + pulsarInfo.getDeadLetterTopic();
            Boolean exist = streamService.exist(groupId, dlqTopic);
            if (exist) {
                throw new BusinessException(ErrorCodeEnum.PULSAR_DLQ_DUPLICATED);
            }
        } else {
            pulsarInfo.setIsDlq(0);
            pulsarInfo.setDeadLetterTopic(null);
        }
        if (rlqEnable) {
            String rlqTopic = PREFIX_RLQ + "_" + pulsarInfo.getRetryLetterTopic();
            Boolean exist = streamService.exist(groupId, rlqTopic);
            if (exist) {
                throw new BusinessException(ErrorCodeEnum.PULSAR_RLQ_DUPLICATED);
            }
        } else {
            pulsarInfo.setIsRlq(0);
            pulsarInfo.setRetryLetterTopic(null);
        }

        ConsumptionPulsarEntity pulsar = CommonBeanUtils.copyProperties(pulsarInfo, ConsumptionPulsarEntity::new);
        Integer consumptionId = entity.getId();
        pulsar.setConsumptionId(consumptionId);
        pulsar.setInlongGroupId(groupId);
        pulsar.setConsumerGroup(entity.getConsumerGroup());
        pulsar.setIsDeleted(0);

        // Pulsar consumer information may already exist, update if it exists, add if it does not exist
        ConsumptionPulsarEntity exists = consumptionPulsarMapper.selectByConsumptionId(consumptionId);
        if (exists == null) {
            consumptionPulsarMapper.insert(pulsar);
        } else {
            pulsar.setId(exists.getId());
            consumptionPulsarMapper.updateByPrimaryKey(pulsar);
        }
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean update(ConsumptionInfo info, String operator) {
        Preconditions.checkNotNull(info, "consumption info cannot be null");
        Integer consumptionId = info.getId();
        Preconditions.checkNotNull(consumptionId, "consumption id cannot be null");

        ConsumptionEntity exists = consumptionMapper.selectByPrimaryKey(consumptionId);
        Preconditions.checkNotNull(exists, "consumption not exist with id " + consumptionId);
        Preconditions.checkTrue(exists.getInCharges().contains(operator),
                "operator" + operator + " has no privilege for the consumption");

        ConsumptionEntity entity = new ConsumptionEntity();
        Date now = new Date();
        CommonBeanUtils.copyProperties(info, entity, true);
        entity.setModifier(operator);
        entity.setModifyTime(now);

        // Modify Pulsar consumption info
        MQType mqType = MQType.forType(info.getMqType());
        if (mqType == MQType.PULSAR || mqType == MQType.TDMQ_PULSAR) {
            ConsumptionPulsarEntity pulsarEntity = consumptionPulsarMapper.selectByConsumptionId(consumptionId);
            Preconditions.checkNotNull(pulsarEntity, "Pulsar consumption cannot be null");
            pulsarEntity.setConsumerGroup(info.getConsumerGroup());

            // Whether DLQ / RLQ is turned on or off
            ConsumptionPulsarInfo update = (ConsumptionPulsarInfo) info.getMqExtInfo();
            boolean dlqEnable = (update.getIsDlq() != null && update.getIsDlq() == 1);
            boolean rlqEnable = (update.getIsRlq() != null && update.getIsRlq() == 1);

            // DLQ is closed, RLQ cannot exist alone and must be closed
            if (rlqEnable && !dlqEnable) {
                throw new BusinessException(ErrorCodeEnum.PULSAR_TOPIC_CREATE_FAILED);
            }

            // If the consumption has been approved, then close/open DLQ or RLQ, it is necessary to
            // add/remove inlong streams in the inlong group
            if (ConsumptionStatus.APPROVED.getStatus() == exists.getStatus()) {
                String groupId = info.getInlongGroupId();
                String dlqNameOld = pulsarEntity.getDeadLetterTopic();
                String dlqNameNew = update.getDeadLetterTopic();
                if (!dlqEnable) {
                    pulsarEntity.setIsDlq(0);
                    pulsarEntity.setDeadLetterTopic(null);
                    streamService.logicDeleteDlqOrRlq(groupId, dlqNameOld, operator);
                } else if (!Objects.equals(dlqNameNew, dlqNameOld)) {
                    pulsarEntity.setIsDlq(1);
                    String topic = PREFIX_DLQ + "_" + dlqNameNew;
                    topic = topic.toLowerCase(Locale.ROOT);
                    pulsarEntity.setDeadLetterTopic(topic);
                    streamService.insertDlqOrRlq(groupId, topic, operator);
                }

                String rlqNameOld = pulsarEntity.getRetryLetterTopic();
                String rlqNameNew = update.getRetryLetterTopic();
                if (!rlqEnable) {
                    pulsarEntity.setIsRlq(0);
                    pulsarEntity.setRetryLetterTopic(null);
                    streamService.logicDeleteDlqOrRlq(groupId, rlqNameOld, operator);
                } else if (!Objects.equals(rlqNameNew, pulsarEntity.getRetryLetterTopic())) {
                    pulsarEntity.setIsRlq(1);
                    String topic = PREFIX_RLQ + "_" + rlqNameNew;
                    topic = topic.toLowerCase(Locale.ROOT);
                    pulsarEntity.setRetryLetterTopic(topic);
                    streamService.insertDlqOrRlq(groupId, topic, operator);
                }
            }

            consumptionPulsarMapper.updateByConsumptionId(pulsarEntity);
        }

        consumptionMapper.updateByPrimaryKeySelective(entity);
        return true;
    }

    /**
     * According to groupId and topic, stitch the full path of Pulsar Topic
     * TODO: save full topic of Pulsar in consumption info
     */
    private String getFullPulsarTopic(String groupId, String topic) {
        InlongGroupEntity inlongGroupEntity = groupMapper.selectByGroupId(groupId);
        String tenant = clusterBean.getDefaultTenant();
        String namespace = inlongGroupEntity.getMqResource();
        return String.format(InlongGroupSettings.PULSAR_TOPIC_FORMAT, tenant, namespace, topic);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean delete(Integer id, String operator) {
        ConsumptionEntity consumptionEntity = consumptionMapper.selectByPrimaryKey(id);
        Preconditions.checkNotNull(consumptionEntity, "consumption not exist with id: " + id);
        consumptionMapper.deleteByPrimaryKey(id);

        consumptionPulsarMapper.deleteByConsumptionId(id);
        return true;
    }

    @Override
    public void saveSortConsumption(InlongGroupInfo groupInfo, String topic, String consumerGroup) {
        String groupId = groupInfo.getInlongGroupId();
        ConsumptionEntity exists = consumptionMapper.selectConsumptionExists(groupId, topic, consumerGroup);
        if (exists != null) {
            log.warn("consumption with groupId={}, topic={}, consumer group={} already exists, skip to create",
                    groupId, topic, consumerGroup);
            return;
        }

        log.debug("begin to save consumption, groupId={}, topic={}, consumer group={}", groupId, topic, consumerGroup);
        MQType mqType = MQType.forType(groupInfo.getMqType());
        ConsumptionEntity entity = new ConsumptionEntity();
        entity.setInlongGroupId(groupId);
        entity.setMqType(mqType.getType());
        entity.setTopic(topic);
        entity.setConsumerGroup(consumerGroup);
        entity.setInCharges(groupInfo.getInCharges());
        entity.setFilterEnabled(0);

        entity.setStatus(ConsumptionStatus.APPROVED.getStatus());
        entity.setIsDeleted(GlobalConstants.UN_DELETED);
        entity.setCreator(groupInfo.getCreator());
        entity.setCreateTime(new Date());

        consumptionMapper.insert(entity);

        if (mqType == MQType.PULSAR || mqType == MQType.TDMQ_PULSAR) {
            ConsumptionPulsarEntity pulsarEntity = new ConsumptionPulsarEntity();
            pulsarEntity.setConsumptionId(entity.getId());
            pulsarEntity.setConsumerGroup(consumerGroup);
            pulsarEntity.setInlongGroupId(groupId);
            pulsarEntity.setIsDeleted(GlobalConstants.UN_DELETED);
            consumptionPulsarMapper.insert(pulsarEntity);
        }

        log.debug("success save consumption, groupId={}, topic={}, consumer group={}", groupId, topic, consumerGroup);
    }

    private ConsumptionEntity saveConsumption(ConsumptionInfo info, String operator, Date now) {
        ConsumptionEntity entity = CommonBeanUtils.copyProperties(info, ConsumptionEntity::new);
        entity.setStatus(ConsumptionStatus.WAIT_ASSIGN.getStatus());
        entity.setIsDeleted(0);
        entity.setCreator(operator);
        entity.setModifier(operator);
        entity.setCreateTime(now);
        entity.setModifyTime(now);

        if (info.getId() != null) {
            consumptionMapper.updateByPrimaryKey(entity);
        } else {
            consumptionMapper.insert(entity);
        }

        Preconditions.checkNotNull(entity.getId(), "save consumption failed");
        return entity;
    }

    /**
     * Fill in consumer information
     */
    private void fullConsumptionInfo(ConsumptionInfo info) {
        Preconditions.checkNotNull(info, "consumption info cannot be null");
        Preconditions.checkFalse(isConsumerGroupExists(info.getConsumerGroup(), info.getId()),
                "consumer group " + info.getConsumerGroup() + " already exist");

        if (info.getId() != null) {
            ConsumptionEntity consumptionEntity = consumptionMapper.selectByPrimaryKey(info.getId());
            Preconditions.checkNotNull(consumptionEntity, "consumption not exist with id: " + info.getId());

            ConsumptionStatus consumptionStatus = ConsumptionStatus.fromStatus(consumptionEntity.getStatus());
            Preconditions.checkTrue(ConsumptionStatus.ALLOW_SAVE_UPDATE_STATUS.contains(consumptionStatus),
                    "consumption not allow update when status is " + consumptionStatus.name());
        }

        // Determine whether the consumed topic belongs to this groupId or the inlong stream under it
        Preconditions.checkNotNull(info.getTopic(), "consumption topic cannot be empty");

        String groupId = info.getInlongGroupId();
        InlongGroupTopicInfo topicVO = groupService.getTopic(groupId);
        Preconditions.checkNotNull(topicVO, "inlong group not exist: " + groupId);

        // Tube’s topic is the inlong group level, one inlong group, one Tube topic
        MQType mqType = MQType.forType(topicVO.getMqType());
        if (mqType == MQType.TUBE) {
            String mqResource = topicVO.getMqResource();
            Preconditions.checkTrue(mqResource == null || mqResource.equals(info.getTopic()),
                    "topic [" + info.getTopic() + "] not belong to inlong group " + groupId);
        } else if (mqType == MQType.PULSAR || mqType == MQType.TDMQ_PULSAR) {
            // Pulsar's topic is the inlong stream level.
            // There will be multiple inlong streams under one inlong group, and there will be multiple topics
            List<InlongStreamTopicInfo> streamTopics = topicVO.getStreamTopics();
            if (streamTopics != null && streamTopics.size() > 0) {
                Set<String> topicSet = new HashSet<>(Arrays.asList(info.getTopic().split(",")));
                streamTopics.forEach(stream -> topicSet.remove(stream.getMqResource()));
                Preconditions.checkEmpty(topicSet, "topic [" + topicSet + "] not belong to inlong group " + groupId);
            }
        }
        info.setMqType(mqType.getType());
    }

}
