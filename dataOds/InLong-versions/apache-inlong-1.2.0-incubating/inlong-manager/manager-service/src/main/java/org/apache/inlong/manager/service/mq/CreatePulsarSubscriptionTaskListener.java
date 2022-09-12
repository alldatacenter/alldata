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

package org.apache.inlong.manager.service.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.pojo.cluster.InlongClusterInfo;
import org.apache.inlong.manager.common.pojo.cluster.pulsar.PulsarClusterInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.pulsar.PulsarTopicBean;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.StreamResourceProcessForm;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.core.ConsumptionService;
import org.apache.inlong.manager.service.mq.util.PulsarOperator;
import org.apache.inlong.manager.service.mq.util.PulsarUtils;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.QueueOperateListener;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Create a subscription group for a single inlong stream
 */
@Slf4j
@Component
public class CreatePulsarSubscriptionTaskListener implements QueueOperateListener {

    @Autowired
    private InlongClusterService clusterService;
    @Autowired
    private PulsarOperator pulsarOperator;
    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private ConsumptionService consumptionService;

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        StreamResourceProcessForm form = (StreamResourceProcessForm) context.getProcessForm();
        InlongGroupInfo groupInfo = form.getGroupInfo();
        InlongStreamInfo streamInfo = form.getStreamInfo();
        final String groupId = streamInfo.getInlongGroupId();
        final String streamId = streamInfo.getInlongStreamId();

        String clusterTag = groupInfo.getInlongClusterTag();
        InlongClusterInfo clusterInfo = clusterService.getOne(clusterTag, null, ClusterType.CLS_PULSAR);
        PulsarClusterInfo pulsarCluster = (PulsarClusterInfo) clusterInfo;
        try (PulsarAdmin pulsarAdmin = PulsarUtils.getPulsarAdmin(pulsarCluster)) {
            // Query data sink info based on groupId and streamId
            List<String> sinkTypeList = sinkService.getSinkTypeList(groupId, streamId);
            if (sinkTypeList == null || sinkTypeList.size() == 0) {
                log.warn("sink info is empty for groupId={}, streamId={}, skip to create pulsar group",
                        groupId, streamId);
                return ListenerResult.success();
            }

            String tenant = pulsarCluster.getTenant();
            String namespace = groupInfo.getMqResource();
            String topic = streamInfo.getMqResource();
            PulsarTopicBean topicBean = new PulsarTopicBean();
            topicBean.setTenant(tenant);
            topicBean.setNamespace(namespace);
            topicBean.setTopicName(topic);

            // Create a subscription in the Pulsar cluster, you need to ensure that the Topic exists
            boolean exist = pulsarOperator.topicIsExists(pulsarAdmin, tenant, namespace, topic);
            if (!exist) {
                String fullTopic = tenant + "/" + namespace + "/" + topic;
                String msg = String.format("topic=%s not exists in %s", fullTopic, pulsarCluster.getAdminUrl());
                log.error(msg);
                throw new BusinessException(msg);
            }

            // Consumer naming rules: clusterTag_topicName_consumer_group
            String subscription = clusterTag + "_" + topic + "_consumer_group";
            pulsarOperator.createSubscription(pulsarAdmin, topicBean, subscription);

            // Insert the consumption data into the consumption table
            consumptionService.saveSortConsumption(groupInfo, topic, subscription);
        } catch (Exception e) {
            log.error("failed to create pulsar subscription for groupId=" + groupId + " streamId=" + streamId, e);
            throw new WorkflowListenerException("failed to create pulsar subscription: " + e.getMessage());
        }

        log.info("success to create pulsar subscription for groupId={}, streamId={}", groupId, streamId);
        return ListenerResult.success();
    }

    @Override
    public boolean async() {
        return false;
    }

}
