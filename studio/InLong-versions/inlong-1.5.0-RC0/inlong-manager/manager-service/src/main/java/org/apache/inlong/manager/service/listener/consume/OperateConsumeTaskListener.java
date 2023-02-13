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

package org.apache.inlong.manager.service.listener.consume;

import com.alibaba.druid.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.pojo.consume.InlongConsumeInfo;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyConsumeProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.task.ConsumeApproveForm;
import org.apache.inlong.manager.service.consume.InlongConsumeService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.TaskEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * New inlong consume approval task event listener
 */
@Slf4j
@Component
public class OperateConsumeTaskListener implements TaskEventListener {

    @Autowired
    private InlongConsumeService consumeService;

    @Override
    public TaskEvent event() {
        return TaskEvent.APPROVE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        ApplyConsumeProcessForm consumeForm = (ApplyConsumeProcessForm) context.getProcessForm();
        ConsumeApproveForm approveForm = (ConsumeApproveForm) context.getActionContext().getForm();
        InlongConsumeInfo consumeInfo = consumeForm.getConsumeInfo();
        if (StringUtils.equals(approveForm.getConsumerGroup(), consumeInfo.getConsumerGroup())) {
            return ListenerResult.success("Consumer group has not been modified");
        }

        boolean exist = consumeService.consumerGroupExists(approveForm.getConsumerGroup(), consumeInfo.getId());
        if (exist) {
            log.error("consumer group {} already exist", approveForm.getConsumerGroup());
            throw new BusinessException(ErrorCodeEnum.CONSUMER_GROUP_DUPLICATED);
        }
        return ListenerResult.success(String.format("Consumer group %s change to %s",
                consumeInfo.getConsumerGroup(), approveForm.getConsumerGroup()));
    }

}
