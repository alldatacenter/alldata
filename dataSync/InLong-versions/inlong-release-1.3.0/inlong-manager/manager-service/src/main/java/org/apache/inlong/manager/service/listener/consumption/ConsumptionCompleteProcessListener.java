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

package org.apache.inlong.manager.service.listener.consumption;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ConsumptionStatus;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.consts.MQType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.ConsumptionEntity;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.mapper.ConsumptionEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterInfo;
import org.apache.inlong.manager.pojo.cluster.tubemq.TubeClusterInfo;
import org.apache.inlong.manager.pojo.queue.pulsar.PulsarTopicBean;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyConsumptionProcessForm;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.resource.queue.pulsar.PulsarOperator;
import org.apache.inlong.manager.service.resource.queue.pulsar.PulsarUtils;
import org.apache.inlong.manager.service.resource.queue.tubemq.TubeMQOperator;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEvent;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Added data consumption process complete archive event listener
 */
@Slf4j
@Component
public class ConsumptionCompleteProcessListener implements ProcessEventListener {

    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private ConsumptionEntityMapper consumptionMapper;
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
        ApplyConsumptionProcessForm consumptionForm = (ApplyConsumptionProcessForm) context.getProcessForm();

        // Real-time query of consumption information
        Integer consumptionId = consumptionForm.getConsumptionInfo().getId();
        ConsumptionEntity entity = consumptionMapper.selectByPrimaryKey(consumptionId);
        if (entity == null) {
            throw new WorkflowListenerException("consumption not exits for id=" + consumptionId);
        }

        String mqType = entity.getMqType();
        if (MQType.TUBEMQ.equals(mqType)) {
            this.createTubeConsumerGroup(entity, context.getOperator());
            return ListenerResult.success("Create TubeMQ consumer group successful");
        } else if (MQType.PULSAR.equals(mqType) || MQType.TDMQ_PULSAR.equals(mqType)) {
            this.createPulsarSubscription(entity);
        } else {
            throw new WorkflowListenerException("Unsupported MQ type " + mqType);
        }

        this.updateConsumerInfo(consumptionId, entity.getConsumerGroup());
        return ListenerResult.success("Create MQ consumer group successful");
    }

    /**
     * Update consumption after approve
     */
    private void updateConsumerInfo(Integer consumptionId, String consumerGroup) {
        ConsumptionEntity update = consumptionMapper.selectByPrimaryKey(consumptionId);
        update.setStatus(ConsumptionStatus.APPROVED.getStatus());
        update.setConsumerGroup(consumerGroup);
        int rowCount = consumptionMapper.updateByPrimaryKeySelective(update);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            log.error("consumption information has already updated, id={}, curVersion={}",
                    update.getId(), update.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
    }

    /**
     * Create Pulsar subscription
     */
    private void createPulsarSubscription(ConsumptionEntity entity) {
        String groupId = entity.getInlongGroupId();
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(groupId);
        Preconditions.checkNotNull(groupEntity, "inlong group not found for groupId=" + groupId);
        String mqResource = groupEntity.getMqResource();
        Preconditions.checkNotNull(mqResource, "mq resource cannot empty for groupId=" + groupId);

        String clusterTag = groupEntity.getInlongClusterTag();
        ClusterInfo clusterInfo = clusterService.getOne(clusterTag, null, ClusterType.PULSAR);
        PulsarClusterInfo pulsarCluster = (PulsarClusterInfo) clusterInfo;
        try (PulsarAdmin pulsarAdmin = PulsarUtils.getPulsarAdmin(pulsarCluster)) {
            PulsarTopicBean topicMessage = new PulsarTopicBean();
            String tenant = pulsarCluster.getTenant();
            if (StringUtils.isEmpty(tenant)) {
                tenant = InlongConstants.DEFAULT_PULSAR_TENANT;
            }
            topicMessage.setTenant(tenant);
            topicMessage.setNamespace(mqResource);

            String consumerGroup = entity.getConsumerGroup();
            List<String> topics = Arrays.asList(entity.getTopic().split(","));
            this.createPulsarSubscription(pulsarAdmin, consumerGroup, topicMessage, topics);
        } catch (Exception e) {
            log.error("create pulsar topic failed", e);
            throw new WorkflowListenerException("failed to create pulsar topic for groupId=" + groupId + ", reason: "
                    + e.getMessage());
        }
    }

    private void createPulsarSubscription(PulsarAdmin pulsarAdmin, String subscription, PulsarTopicBean topicBean,
            List<String> topics) {
        try {
            pulsarOperator.createSubscriptions(pulsarAdmin, subscription, topicBean, topics);
        } catch (Exception e) {
            log.error("create pulsar consumer group failed", e);
            throw new WorkflowListenerException("failed to create pulsar consumer group");
        }
    }

    /**
     * Create TubeMQ consumer group
     */
    private void createTubeConsumerGroup(ConsumptionEntity entity, String operator) {
        String groupId = entity.getInlongGroupId();
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(groupId);
        Preconditions.checkNotNull(groupEntity, "inlong group not found for groupId=" + groupId);
        String mqResource = groupEntity.getMqResource();
        Preconditions.checkNotNull(mqResource, "mq resource cannot empty for groupId=" + groupId);

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
