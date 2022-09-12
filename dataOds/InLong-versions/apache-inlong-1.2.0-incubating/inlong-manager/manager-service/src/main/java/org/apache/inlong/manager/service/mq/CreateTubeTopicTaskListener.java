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
import org.apache.inlong.manager.common.pojo.cluster.InlongClusterInfo;
import org.apache.inlong.manager.common.pojo.cluster.tube.TubeClusterInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.mq.util.TubeMQOperator;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.QueueOperateListener;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Create a listener for MQ resource tasks
 */
@Component
@Slf4j
public class CreateTubeTopicTaskListener implements QueueOperateListener {

    @Autowired
    private InlongClusterService clusterService;
    @Autowired
    private TubeMQOperator tubeMQOperator;
    @Autowired
    private InlongGroupService groupService;

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        GroupResourceProcessForm form = (GroupResourceProcessForm) context.getProcessForm();
        log.info("begin create tube topic for groupId={}", form.getInlongGroupId());

        String groupId = form.getInlongGroupId();
        try {
            InlongGroupInfo groupInfo = form.getGroupInfo();
            String clusterTag = groupInfo.getInlongClusterTag();
            InlongClusterInfo clusterInfo = clusterService.getOne(clusterTag, null, ClusterType.CLS_TUBE);
            tubeMQOperator.createTopic((TubeClusterInfo) clusterInfo, groupInfo.getMqResource(), context.getOperator());
            log.info("finish to create tube topic for groupId={}", groupId);
        } catch (Exception e) {
            log.error("create tube topic for groupId={} error, exception {} ", groupId, e.getMessage(), e);
        }
        return ListenerResult.success();
    }

    @Override
    public boolean async() {
        return false;
    }

}
