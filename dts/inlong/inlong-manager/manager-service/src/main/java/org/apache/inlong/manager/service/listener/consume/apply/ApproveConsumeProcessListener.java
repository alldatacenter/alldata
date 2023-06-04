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

package org.apache.inlong.manager.service.listener.consume.apply;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ConsumeStatus;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.ProcessEvent;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongConsumeEntity;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.mapper.InlongConsumeEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterInfo;
import org.apache.inlong.manager.pojo.cluster.tubemq.TubeClusterInfo;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarDTO;
import org.apache.inlong.manager.pojo.queue.pulsar.PulsarTopicInfo;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyConsumeProcessForm;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.resource.queue.pulsar.PulsarOperator;
import org.apache.inlong.manager.service.resource.queue.pulsar.PulsarUtils;
import org.apache.inlong.manager.service.resource.queue.tubemq.TubeMQOperator;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Inlong consume process complete archive event listener
 */
@Slf4j
@Component
public class ApproveConsumeProcessListener implements ProcessEventListener {

    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private InlongConsumeEntityMapper consumeMapper;
    @Autowired
    private InlongClusterService clusterService;
    @Autowired
    private PulsarOperator pulsarOperator;
    @Autowired
    private TubeMQOperator tubeMQOperator;

    @Override
    public ProcessEvent event() {
        return ProcessEvent.COMPLETE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        // query of inlong consume info from DB
        ApplyConsumeProcessForm consumeForm = (ApplyConsumeProcessForm) context.getProcessForm();
        Integer consumeId = consumeForm.getConsumeInfo().getId();
        InlongConsumeEntity entity = consumeMapper.selectById(consumeId);
        if (entity == null) {
            throw new WorkflowListenerException("inlong consume not exits for id=" + consumeId);
        }

        String mqType = entity.getMqType();
        if (MQType.TUBEMQ.equals(mqType)) {
            this.createTubeConsumerGroup(entity, context.getOperator());
        } else if (MQType.PULSAR.equals(mqType) || MQType.TDMQ_PULSAR.equals(mqType)) {
            this.createPulsarSubscription(entity);
        } else if (MQType.KAFKA.equals(mqType)) {
            // Kafka consumers do not need to register in advance
        } else {
            throw new WorkflowListenerException("Unsupported MQ type " + mqType);
        }

        // the consumer group may be changed when approving
        this.updateConsumerInfo(consumeId, entity.getConsumerGroup());
        return ListenerResult.success("Create MQ consumer group successful");
    }

    /**
     * Update consume info after approval
     */
    private void updateConsumerInfo(Integer consumeId, String consumerGroup) {
        InlongConsumeEntity existEntity = consumeMapper.selectById(consumeId);
        existEntity.setStatus(ConsumeStatus.APPROVE_PASSED.getCode());
        existEntity.setConsumerGroup(consumerGroup);
        int rowCount = consumeMapper.updateByIdSelective(existEntity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            log.error("inlong consume has already updated, id={}, curVersion={}",
                    existEntity.getId(), existEntity.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
    }

    /**
     * Create Pulsar subscription
     */
    private void createPulsarSubscription(InlongConsumeEntity entity) {
        String groupId = entity.getInlongGroupId();
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(groupId);
        Preconditions.expectNotNull(groupEntity, "inlong group not found for groupId=" + groupId);
        String mqResource = groupEntity.getMqResource();
        Preconditions.expectNotBlank(mqResource, ErrorCodeEnum.INVALID_PARAMETER,
                "mq resource cannot empty for groupId=" + groupId);

        String clusterTag = groupEntity.getInlongClusterTag();
        ClusterInfo clusterInfo = clusterService.getOne(clusterTag, null, ClusterType.PULSAR);
        PulsarClusterInfo pulsarCluster = (PulsarClusterInfo) clusterInfo;
        try (PulsarAdmin pulsarAdmin = PulsarUtils.getPulsarAdmin(pulsarCluster)) {
            InlongPulsarDTO pulsarDTO = InlongPulsarDTO.getFromJson(groupEntity.getExtParams());
            String tenant = pulsarDTO.getTenant();
            if (StringUtils.isBlank(tenant)) {
                tenant = pulsarCluster.getTenant();
            }
            PulsarTopicInfo topicMessage = new PulsarTopicInfo();
            topicMessage.setTenant(tenant);
            topicMessage.setNamespace(mqResource);

            List<String> topics = Arrays.asList(entity.getTopic().split(InlongConstants.COMMA));
            this.createPulsarSubscription(pulsarAdmin, entity.getConsumerGroup(), topicMessage, topics);
        } catch (Exception e) {
            log.error("create pulsar topic failed", e);
            throw new WorkflowListenerException("failed to create pulsar topic for groupId=" + groupId + ", reason: "
                    + e.getMessage());
        }
    }

    private void createPulsarSubscription(PulsarAdmin pulsarAdmin, String subscription, PulsarTopicInfo topicInfo,
            List<String> topics) {
        try {
            pulsarOperator.createSubscriptions(pulsarAdmin, subscription, topicInfo, topics);
        } catch (Exception e) {
            log.error("create pulsar consumer group failed", e);
            throw new WorkflowListenerException("failed to create pulsar consumer group");
        }
    }

    /**
     * Create TubeMQ consumer group
     */
    private void createTubeConsumerGroup(InlongConsumeEntity entity, String operator) {
        String groupId = entity.getInlongGroupId();
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(groupId);
        Preconditions.expectNotNull(groupEntity, "inlong group not found for groupId=" + groupId);
        String mqResource = groupEntity.getMqResource();
        Preconditions.expectNotBlank(mqResource, ErrorCodeEnum.INVALID_PARAMETER,
                "mq resource cannot empty for groupId=" + groupId);

        String clusterTag = groupEntity.getInlongClusterTag();
        TubeClusterInfo clusterInfo = (TubeClusterInfo) clusterService.getOne(clusterTag, null, ClusterType.TUBEMQ);
        try {
            tubeMQOperator.createConsumerGroup(clusterInfo, entity.getTopic(), entity.getConsumerGroup(), operator);
        } catch (Exception e) {
            log.error("failed to create tubemq consumer group: ", e);
            throw new WorkflowListenerException("failed to create tubemq consumer group: " + e.getMessage());
        }
    }

}
