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

package org.apache.inlong.manager.service.listener.sort;

import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.process.ProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.process.StreamResourceProcessForm;
import org.apache.inlong.manager.service.resource.sort.SortConfigOperator;
import org.apache.inlong.manager.service.resource.sort.SortConfigOperatorFactory;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.SortOperateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Event listener of build the Sort config,
 * such as update the form config, or build and push config to ZK, etc.
 */
@Component
public class SortConfigListener implements SortOperateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SortConfigListener.class);

    @Autowired
    private SortConfigOperatorFactory operatorFactory;

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public boolean accept(WorkflowContext context) {
        ProcessForm processForm = context.getProcessForm();
        String className = processForm.getClass().getSimpleName();
        String groupId = processForm.getInlongGroupId();
        if (processForm instanceof GroupResourceProcessForm || processForm instanceof StreamResourceProcessForm) {
            LOGGER.info("accept sort config listener as the process is {} for groupId [{}]", className, groupId);
            return true;
        } else {
            LOGGER.info("not accept sort config listener as the process is {} for groupId [{}]", className, groupId);
            return false;
        }
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        GroupResourceProcessForm form = (GroupResourceProcessForm) context.getProcessForm();
        String groupId = form.getInlongGroupId();
        LOGGER.info("begin to build sort config for groupId={}", groupId);

        GroupOperateType operateType = form.getGroupOperateType();
        if (operateType == GroupOperateType.SUSPEND || operateType == GroupOperateType.DELETE) {
            LOGGER.info("no need to build sort config for groupId={} as the operate type is {}", groupId, operateType);
            return ListenerResult.success();
        }

        InlongGroupInfo groupInfo = form.getGroupInfo();
        List<InlongStreamInfo> streamInfos = form.getStreamInfos();
        if (CollectionUtils.isEmpty(streamInfos)) {
            LOGGER.warn("no need to build sort config for groupId={}, as not found any stream", groupId);
            return ListenerResult.success();
        }

        int sinkCount = streamInfos.stream()
                .map(stream -> stream.getSinkList() == null ? 0 : stream.getSinkList().size())
                .reduce(0, Integer::sum);
        if (sinkCount == 0) {
            LOGGER.warn("not build sort config for groupId={}, as not found any sink", groupId);
            return ListenerResult.success();
        }

        try {
            SortConfigOperator operator = operatorFactory.getInstance(groupInfo.getEnableZookeeper());
            operator.buildConfig(groupInfo, streamInfos, false);
        } catch (Exception e) {
            String msg = String.format("failed to build sort config for groupId=%s, ", groupId);
            LOGGER.error(msg + "streamInfos=" + streamInfos, e);
            throw new WorkflowListenerException(msg + e.getMessage());
        }

        LOGGER.info("success to build sort config for groupId={}", groupId);
        return ListenerResult.success();
    }

}
