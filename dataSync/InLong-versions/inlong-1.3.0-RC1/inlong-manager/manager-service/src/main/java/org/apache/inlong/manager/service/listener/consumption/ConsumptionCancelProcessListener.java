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
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ConsumptionStatus;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyConsumptionProcessForm;
import org.apache.inlong.manager.dao.entity.ConsumptionEntity;
import org.apache.inlong.manager.dao.mapper.ConsumptionEntityMapper;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEvent;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Added data consumption process cancellation event listener
 */
@Slf4j
@Component
public class ConsumptionCancelProcessListener implements ProcessEventListener {

    @Autowired
    private ConsumptionEntityMapper consumptionEntityMapper;

    @Autowired
    public ConsumptionCancelProcessListener(ConsumptionEntityMapper consumptionEntityMapper) {
        this.consumptionEntityMapper = consumptionEntityMapper;
    }

    @Override
    public ProcessEvent event() {
        return ProcessEvent.CANCEL;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        ApplyConsumptionProcessForm processForm = (ApplyConsumptionProcessForm) context.getProcessForm();

        ConsumptionEntity update = consumptionEntityMapper.selectByPrimaryKey(processForm.getConsumptionInfo().getId());
        update.setStatus(ConsumptionStatus.CANCELED.getStatus());
        int rowCount = consumptionEntityMapper.updateByPrimaryKeySelective(update);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            log.error("consumption information has already updated, id={}, curVersion={}",
                    update.getId(), update.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }
        return ListenerResult.success("Application process is cancelled");
    }

}
