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
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.pojo.cluster.tube.TubeClusterInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.mq.util.TubeMQOperator;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.QueueOperateListener;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Event listener of create tube consumer group.
 */
@Component
@Slf4j
public class CreateTubeGroupTaskListener implements QueueOperateListener {

    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private InlongClusterService clusterService;
    @Autowired
    private TubeMQOperator tubeMQOperator;

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        GroupResourceProcessForm form = (GroupResourceProcessForm) context.getProcessForm();
        String groupId = form.getInlongGroupId();
        log.info("begin to create tube consumer group for groupId {}", groupId);

        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(groupId);
        String clusterTag = groupEntity.getInlongClusterTag();
        TubeClusterInfo tubeCluster = (TubeClusterInfo) clusterService.getOne(clusterTag, null, ClusterType.CLS_TUBE);

        String topicName = groupEntity.getMqResource();
        // Consumer naming rules: clusterTag_topicName_consumer_group
        String consumeGroup = clusterTag + "_" + topicName + "_consumer_group";
        try {
            tubeMQOperator.createConsumerGroup(tubeCluster, topicName, consumeGroup, context.getOperator());
        } catch (Exception e) {
            throw new WorkflowListenerException("create tube consumer group for groupId=" + groupId + " error", e);
        }
        log.info("finish to create tube consumer group for groupId={}", groupId);
        return ListenerResult.success();
    }

    @Override
    public boolean async() {
        return false;
    }

}
