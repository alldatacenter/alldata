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

package org.apache.inlong.manager.service.workflow.consumption.listener;

import com.alibaba.druid.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.pojo.consumption.ConsumptionInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.ConsumptionApproveForm;
import org.apache.inlong.manager.common.pojo.workflow.form.NewConsumptionProcessForm;
import org.apache.inlong.manager.service.core.ConsumptionService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;
import org.apache.inlong.manager.workflow.event.task.TaskEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * New data consumption-system administrator approval task event listener
 */
@Slf4j
@Component
public class ConsumptionPassTaskListener implements TaskEventListener {

    @Autowired
    private ConsumptionService consumptionService;

    @Override
    public TaskEvent event() {
        return TaskEvent.APPROVE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        NewConsumptionProcessForm form = (NewConsumptionProcessForm) context.getProcessForm();
        ConsumptionApproveForm approveForm = (ConsumptionApproveForm) context.getActionContext().getForm();
        ConsumptionInfo info = form.getConsumptionInfo();
        if (StringUtils.equals(approveForm.getConsumerGroup(), info.getConsumerGroup())) {
            return ListenerResult.success("The consumer group has not been modified");
        }
        boolean exist = consumptionService.isConsumerGroupExists(approveForm.getConsumerGroup(), info.getId());
        if (exist) {
            log.error("consumer group {} already exist", approveForm.getConsumerGroup());
            throw new BusinessException(ErrorCodeEnum.CONSUMER_GROUP_DUPLICATED);
        }
        return ListenerResult.success("Consumer group from " + info.getConsumerGroup()
                + " change to " + approveForm.getConsumerGroup());
    }

    @Override
    public boolean async() {
        return false;
    }

}
