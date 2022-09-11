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
import org.apache.inlong.manager.common.enums.GlobalConstants;
import org.apache.inlong.manager.common.enums.MQType;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.group.pulsar.InlongPulsarInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.common.pojo.workflow.form.ProcessForm;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.EventSelector;

/**
 * Selector of pulsar event for create pulsar resource.
 */
@Slf4j
public class PulsarEventSelector implements EventSelector {

    @Override
    public boolean accept(WorkflowContext context) {
        ProcessForm processForm = context.getProcessForm();
        if (!(processForm instanceof GroupResourceProcessForm)) {
            return false;
        }

        GroupResourceProcessForm form = (GroupResourceProcessForm) processForm;
        String groupId = form.getInlongGroupId();
        InlongGroupInfo groupInfo = form.getGroupInfo();
        MQType mqType = MQType.forType(groupInfo.getMqType());
        if (mqType == MQType.PULSAR || mqType == MQType.TDMQ_PULSAR) {
            InlongPulsarInfo pulsarInfo = (InlongPulsarInfo) groupInfo;
            boolean enable = GlobalConstants.ENABLE_CREATE_RESOURCE.equals(pulsarInfo.getEnableCreateResource());
            if (enable) {
                log.info("need to create pulsar resource as the createResource was true for groupId [{}]", groupId);
                return true;
            } else {
                log.info("skip to create pulsar resource as the createResource was false for groupId [{}]", groupId);
                return false;
            }
        }

        log.warn("skip to create pulsar subscription as the mq type is {} for groupId [{}]", mqType, groupId);
        return false;
    }

}
