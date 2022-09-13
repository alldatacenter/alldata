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

package org.apache.inlong.manager.service.resource.queue.pulsar;

import com.google.common.base.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.MQType;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarInfo;
import org.apache.inlong.manager.pojo.queue.pulsar.PulsarTopicBean;
import org.apache.inlong.manager.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.core.ConsumptionService;
import org.apache.inlong.manager.service.resource.queue.QueueResourceOperator;
import org.apache.inlong.manager.service.stream.InlongStreamService;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Operator for create Pulsar Tenant, Namespace, Topic and Subscription
 */
@Slf4j
@Service
public class PulsarResourceOperator implements QueueResourceOperator {

    @Autowired
    private InlongStreamService streamService;
    @Autowired
    private InlongClusterService clusterService;
    @Autowired
    private ConsumptionService consumptionService;
    @Autowired
    private PulsarOperator pulsarOperator;

    @Override
    public boolean accept(String mqType) {
        return MQType.PULSAR.equals(mqType) || MQType.TDMQ_PULSAR.equals(mqType);
    }

    @Override
    public void createQueueForGroup(InlongGroupInfo groupInfo, String operator) {
        Preconditions.checkNotNull(groupInfo, "inlong group info cannot be null");
        Preconditions.checkNotNull(operator, "operator cannot be null");

        String groupId = groupInfo.getInlongGroupId();
        log.info("begin to create pulsar resource for groupId={}", groupId);

        // get pulsar cluster via the inlong cluster tag from the inlong group
        String clusterTag = groupInfo.getInlongClusterTag();
        PulsarClusterInfo pulsarCluster = (PulsarClusterInfo) clusterService.getOne(clusterTag, null,
                ClusterType.PULSAR);
        try (PulsarAdmin pulsarAdmin = PulsarUtils.getPulsarAdmin(pulsarCluster)) {
            // 1. create pulsar tenant and namespace
            String tenant = pulsarCluster.getTenant();
            if (StringUtils.isEmpty(tenant)) {
                tenant = InlongConstants.DEFAULT_PULSAR_TENANT;
            }
            String namespace = groupInfo.getMqResource();
            InlongPulsarInfo pulsarInfo = (InlongPulsarInfo) groupInfo;
            // if the group was not successful, need create tenant and namespace
            if (!Objects.equal(GroupStatus.CONFIG_SUCCESSFUL.getCode(), groupInfo.getStatus())) {
                pulsarOperator.createTenant(pulsarAdmin, tenant);
                log.info("success to create pulsar tenant for groupId={}, tenant={}", groupId, tenant);
                pulsarOperator.createNamespace(pulsarAdmin, pulsarInfo, tenant, namespace);
                log.info("success to create pulsar namespace for groupId={}, namespace={}", groupId, namespace);
            }

            // 2. create Pulsar Topic - each Inlong Stream corresponds to a Pulsar Topic
            List<InlongStreamBriefInfo> streamInfoList = streamService.getTopicList(groupId);
            if (streamInfoList == null || streamInfoList.isEmpty()) {
                log.warn("skip to create pulsar topic and subscription as no streams for groupId={}", groupId);
                return;
            }
            for (InlongStreamBriefInfo streamInfo : streamInfoList) {
                this.createPulsarTopic(groupInfo, pulsarCluster, streamInfo.getMqResource());
            }
        } catch (Exception e) {
            String msg = String.format("failed to create pulsar resource for groupId=%s", groupId);
            log.error(msg, e);
            throw new WorkflowListenerException(msg + ": " + e.getMessage());
        }

        log.info("success to create pulsar resource for groupId={}, cluster={}", groupId, pulsarCluster);
    }

    @Override
    public void deleteQueueForGroup(InlongGroupInfo groupInfo, String operator) {
        Preconditions.checkNotNull(groupInfo, "inlong group info cannot be null");

        String groupId = groupInfo.getInlongGroupId();
        log.info("begin to delete pulsar resource for groupId={}", groupId);

        ClusterInfo clusterInfo = clusterService.getOne(groupInfo.getInlongClusterTag(), null, ClusterType.PULSAR);
        try {
            List<InlongStreamBriefInfo> streamInfoList = streamService.getTopicList(groupId);
            if (streamInfoList == null || streamInfoList.isEmpty()) {
                log.warn("skip to create pulsar topic and subscription as no streams for groupId={}", groupId);
                return;
            }
            for (InlongStreamBriefInfo streamInfo : streamInfoList) {
                this.deletePulsarTopic(groupInfo, (PulsarClusterInfo) clusterInfo, streamInfo.getMqResource());
            }
        } catch (Exception e) {
            log.error("failed to delete pulsar resource for groupId=" + groupId, e);
            throw new WorkflowListenerException("failed to delete pulsar resource: " + e.getMessage());
        }

        log.info("success to delete pulsar resource for groupId={}, cluster={}", groupId, clusterInfo);
    }

    @Override
    public void createQueueForStream(InlongGroupInfo groupInfo, InlongStreamInfo streamInfo, String operator) {
        Preconditions.checkNotNull(groupInfo, "inlong group info cannot be null");
        Preconditions.checkNotNull(streamInfo, "inlong stream info cannot be null");
        Preconditions.checkNotNull(operator, "operator cannot be null");

        String groupId = streamInfo.getInlongGroupId();
        String streamId = streamInfo.getInlongStreamId();
        log.info("begin to create pulsar resource for groupId={}, streamId={}", groupId, streamId);

        try {
            // get pulsar cluster via the inlong cluster tag from the inlong group
            String clusterTag = groupInfo.getInlongClusterTag();
            ClusterInfo clusterInfo = clusterService.getOne(clusterTag, null, ClusterType.PULSAR);
            // create pulsar topic
            this.createPulsarTopic(groupInfo, (PulsarClusterInfo) clusterInfo, streamInfo.getMqResource());
        } catch (Exception e) {
            String msg = String.format("failed to create pulsar topic for groupId=%s, streamId=%s", groupId, streamId);
            log.error(msg, e);
            throw new WorkflowListenerException(msg + ": " + e.getMessage());
        }

        log.info("success to create pulsar resource for groupId={}, streamId={}", groupId, streamId);
    }

    @Override
    public void deleteQueueForStream(InlongGroupInfo groupInfo, InlongStreamInfo streamInfo, String operator) {
        Preconditions.checkNotNull(groupInfo, "inlong group info cannot be null");
        Preconditions.checkNotNull(streamInfo, "inlong stream info cannot be null");

        String groupId = streamInfo.getInlongGroupId();
        String streamId = streamInfo.getInlongStreamId();
        log.info("begin to delete pulsar resource for groupId={} streamId={}", groupId, streamId);

        try {
            ClusterInfo clusterInfo = clusterService.getOne(groupInfo.getInlongClusterTag(), null, ClusterType.PULSAR);
            this.deletePulsarTopic(groupInfo, (PulsarClusterInfo) clusterInfo, streamInfo.getMqResource());
            log.info("success to delete pulsar topic for groupId={}, streamId={}", groupId, streamId);
        } catch (Exception e) {
            String msg = String.format("failed to delete pulsar topic for groupId=%s, streamId=%s", groupId, streamId);
            log.error(msg, e);
            throw new WorkflowListenerException(msg);
        }

        log.info("success to delete pulsar resource for groupId={}, streamId={}", groupId, streamId);
    }

    /**
     * Create Pulsar Topic and Subscription, and save the consumer group info.
     */
    private void createPulsarTopic(InlongGroupInfo groupInfo, PulsarClusterInfo pulsarCluster, String topicName)
            throws Exception {
        try (PulsarAdmin pulsarAdmin = PulsarUtils.getPulsarAdmin(pulsarCluster)) {
            // 1. create pulsar topic
            InlongPulsarInfo pulsarInfo = (InlongPulsarInfo) groupInfo;
            String tenant = pulsarCluster.getTenant();
            if (StringUtils.isEmpty(tenant)) {
                tenant = InlongConstants.DEFAULT_PULSAR_TENANT;
            }
            String namespace = pulsarInfo.getMqResource();
            PulsarTopicBean topicBean = PulsarTopicBean.builder()
                    .tenant(tenant)
                    .namespace(namespace)
                    .topicName(topicName)
                    .queueModule(pulsarInfo.getQueueModule())
                    .numPartitions(pulsarInfo.getPartitionNum())
                    .build();
            pulsarOperator.createTopic(pulsarAdmin, topicBean);

            // 2. create a subscription for the pulsar topic
            boolean exist = pulsarOperator.topicIsExists(pulsarAdmin, tenant, namespace, topicName,
                    InlongConstants.PULSAR_QUEUE_TYPE_PARALLEL.equals(topicBean.getQueueModule()));
            if (!exist) {
                String topicFullName = tenant + "/" + namespace + "/" + topicName;
                String serviceUrl = pulsarCluster.getAdminUrl();
                log.error("topic={} not exists in {}", topicFullName, serviceUrl);
                throw new WorkflowListenerException("topic=" + topicFullName + " not exists in " + serviceUrl);
            }

            // subscription naming rules: clusterTag_topicName_consumer_group
            String subscription = groupInfo.getInlongClusterTag() + "_" + topicName + "_consumer_group";
            pulsarOperator.createSubscription(pulsarAdmin, topicBean, subscription);
            String groupId = groupInfo.getInlongGroupId();
            log.info("success to create pulsar subscription for groupId={}, topic={}, subs={}",
                    groupId, topicName, subscription);

            // 3. insert the consumer group info into the consumption table
            consumptionService.saveSortConsumption(groupInfo, topicName, subscription);
            log.info("success to save consume for groupId={}, topic={}, subs={}", groupId, topicName, subscription);
        }
    }

    /**
     * Delete Pulsar Topic and Subscription, and delete the consumer group info.
     */
    private void deletePulsarTopic(InlongGroupInfo groupInfo, PulsarClusterInfo pulsarCluster, String topicName)
            throws Exception {
        try (PulsarAdmin pulsarAdmin = PulsarUtils.getPulsarAdmin(pulsarCluster)) {
            // 1. delete pulsar topic
            String tenant = pulsarCluster.getTenant();
            if (StringUtils.isEmpty(tenant)) {
                tenant = InlongConstants.DEFAULT_PULSAR_TENANT;
            }
            String namespace = groupInfo.getMqResource();
            PulsarTopicBean topicBean = PulsarTopicBean.builder()
                    .tenant(tenant)
                    .namespace(namespace)
                    .topicName(topicName)
                    .build();
            pulsarOperator.forceDeleteTopic(pulsarAdmin, topicBean);
        }
    }

}
