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

package org.apache.inlong.manager.service.listener.consume.apply;

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ConsumeStatus;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.ProcessEvent;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.dao.entity.InlongConsumeEntity;
import org.apache.inlong.manager.dao.mapper.InlongConsumeEntityMapper;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyConsumeProcessForm;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Inlong consume process rejection event listener
 */
@Component
public class RejectConsumeProcessListener implements ProcessEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RejectConsumeProcessListener.class);

    @Autowired
    private InlongConsumeEntityMapper consumeMapper;

    @Autowired
    public RejectConsumeProcessListener(InlongConsumeEntityMapper consumeMapper) {
        this.consumeMapper = consumeMapper;
    }

    @Override
    public ProcessEvent event() {
        return ProcessEvent.REJECT;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        ApplyConsumeProcessForm processForm = (ApplyConsumeProcessForm) context.getProcessForm();
        InlongConsumeEntity consumeEntity = consumeMapper.selectById(processForm.getConsumeInfo().getId());
        consumeEntity.setStatus(ConsumeStatus.APPROVE_REJECTED.getCode());

        int rowCount = consumeMapper.updateByIdSelective(consumeEntity);
        if (rowCount != InlongConstants.AFFECTED_ONE_ROW) {
            LOGGER.error("inlong consume has already updated, id={}, curVersion={}",
                    consumeEntity.getId(), consumeEntity.getVersion());
            throw new BusinessException(ErrorCodeEnum.CONFIG_EXPIRED);
        }

        return ListenerResult.success("The consume application was rejected");
    }
}
