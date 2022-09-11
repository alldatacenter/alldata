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
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.pojo.cluster.InlongClusterInfo;
import org.apache.inlong.manager.common.pojo.cluster.pulsar.PulsarClusterInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.group.pulsar.InlongPulsarInfo;
import org.apache.inlong.manager.common.pojo.pulsar.PulsarTopicBean;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.common.settings.InlongGroupSettings;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.core.InlongStreamService;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.mq.util.PulsarOperator;
import org.apache.inlong.manager.service.mq.util.PulsarUtils;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.QueueOperateListener;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Create Pulsar tenant, namespace and topic
 */
@Slf4j
@Component
public class CreatePulsarResourceTaskListener implements QueueOperateListener {

    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private InlongStreamService streamService;
    @Autowired
    private InlongClusterService clusterService;
    @Autowired
    private PulsarOperator pulsarOperator;

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        GroupResourceProcessForm form = (GroupResourceProcessForm) context.getProcessForm();
        String groupId = form.getInlongGroupId();
        log.info("begin to create pulsar resource for groupId={}", groupId);

        InlongGroupInfo groupInfo = groupService.get(groupId);
        if (groupInfo == null) {
            throw new WorkflowListenerException("inlong group or pulsar cluster not found for groupId=" + groupId);
        }

        try {
            InlongPulsarInfo pulsarInfo = (InlongPulsarInfo) groupInfo;
            InlongClusterInfo clusterInfo = clusterService.getOne(groupInfo.getInlongClusterTag(), null,
                    ClusterType.CLS_PULSAR);
            this.createPulsarProcess(pulsarInfo, (PulsarClusterInfo) clusterInfo);
        } catch (Exception e) {
            log.error("create pulsar resource error for groupId={}", groupId, e);
            throw new WorkflowListenerException("create pulsar resource error for groupId=" + groupId);
        }

        log.info("success to create pulsar resource for groupId={}", groupId);
        return ListenerResult.success();
    }

    /**
     * Create Pulsar tenant, namespace and topic
     */
    private void createPulsarProcess(InlongPulsarInfo pulsarInfo, PulsarClusterInfo pulsarCluster) throws Exception {
        String groupId = pulsarInfo.getInlongGroupId();
        log.info("begin to create pulsar resource for groupId={} in cluster={}", groupId, pulsarCluster);

        String namespace = pulsarInfo.getMqResource();
        Preconditions.checkNotNull(namespace, "pulsar namespace cannot be empty for groupId=" + groupId);
        String queueModule = pulsarInfo.getQueueModule();
        Preconditions.checkNotNull(queueModule, "queue module cannot be empty for groupId=" + groupId);

        try (PulsarAdmin pulsarAdmin = PulsarUtils.getPulsarAdmin(pulsarCluster)) {
            // create pulsar tenant
            String tenant = pulsarCluster.getTenant();
            if (StringUtils.isEmpty(tenant)) {
                tenant = InlongGroupSettings.DEFAULT_PULSAR_TENANT;
            }
            pulsarOperator.createTenant(pulsarAdmin, tenant);

            // create pulsar namespace
            pulsarOperator.createNamespace(pulsarAdmin, pulsarInfo, tenant, namespace);

            // create pulsar topic
            Integer partitionNum = pulsarInfo.getPartitionNum();
            List<InlongStreamBriefInfo> streamTopicList = streamService.getTopicList(groupId);
            PulsarTopicBean topicBean = PulsarTopicBean.builder()
                    .tenant(tenant).namespace(namespace).numPartitions(partitionNum).queueModule(queueModule).build();

            for (InlongStreamBriefInfo streamInfo : streamTopicList) {
                topicBean.setTopicName(streamInfo.getMqResource());
                pulsarOperator.createTopic(pulsarAdmin, topicBean);
            }
        }
        log.info("finish to create pulsar resource for groupId={}, cluster={}", groupId, pulsarCluster);
    }

    @Override
    public boolean async() {
        return false;
    }

}
