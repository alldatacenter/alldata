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

package org.apache.inlong.manager.service.core.operation;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.ConsumptionStatus;
import org.apache.inlong.manager.common.enums.MQType;
import org.apache.inlong.manager.common.pojo.consumption.ConsumptionInfo;
import org.apache.inlong.manager.common.pojo.consumption.ConsumptionPulsarInfo;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.common.pojo.workflow.form.NewConsumptionProcessForm;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.ConsumptionPulsarEntity;
import org.apache.inlong.manager.dao.mapper.ConsumptionPulsarEntityMapper;
import org.apache.inlong.manager.service.core.ConsumptionService;
import org.apache.inlong.manager.service.workflow.ProcessName;
import org.apache.inlong.manager.service.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConsumptionProcessOperation {

    @Autowired
    private ConsumptionService consumptionService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private ConsumptionPulsarEntityMapper consumptionPulsarMapper;

    public WorkflowResult startProcess(Integer id, String operator) {
        ConsumptionInfo consumptionInfo = consumptionService.get(id);
        Preconditions.checkTrue(ConsumptionStatus.ALLOW_START_WORKFLOW_STATUS.contains(
                        ConsumptionStatus.fromStatus(consumptionInfo.getStatus())),
                "current status not allow start workflow");

        consumptionInfo.setStatus(ConsumptionStatus.WAIT_APPROVE.getStatus());
        boolean isSuccess = consumptionService.update(consumptionInfo,operator);
        Preconditions.checkTrue(isSuccess, "update consumption failed");

        return workflowService.start(ProcessName.NEW_CONSUMPTION_PROCESS, operator,
                genNewConsumptionProcessForm(consumptionInfo));
    }

    private NewConsumptionProcessForm genNewConsumptionProcessForm(ConsumptionInfo consumptionInfo) {
        NewConsumptionProcessForm form = new NewConsumptionProcessForm();
        Integer id = consumptionInfo.getId();
        MQType mqType = MQType.forType(consumptionInfo.getMqType());
        if (mqType == MQType.PULSAR || mqType == MQType.TDMQ_PULSAR) {
            ConsumptionPulsarEntity consumptionPulsarEntity = consumptionPulsarMapper.selectByConsumptionId(id);
            ConsumptionPulsarInfo pulsarInfo = CommonBeanUtils.copyProperties(consumptionPulsarEntity,
                    ConsumptionPulsarInfo::new);
            consumptionInfo.setMqExtInfo(pulsarInfo);
        }
        form.setConsumptionInfo(consumptionInfo);
        return form;
    }
}
