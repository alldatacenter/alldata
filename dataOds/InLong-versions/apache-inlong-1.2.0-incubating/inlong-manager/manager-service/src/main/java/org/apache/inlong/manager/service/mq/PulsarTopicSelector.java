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
import org.apache.inlong.manager.common.enums.MQType;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.ProcessForm;
import org.apache.inlong.manager.common.pojo.workflow.form.StreamResourceProcessForm;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.EventSelector;

@Slf4j
public class PulsarTopicSelector implements EventSelector {

    @Override
    public boolean accept(WorkflowContext context) {
        ProcessForm processForm = context.getProcessForm();
        if (!(processForm instanceof StreamResourceProcessForm)) {
            return false;
        }
        StreamResourceProcessForm streamResourceProcessForm = (StreamResourceProcessForm) processForm;
        MQType mqType = MQType.forType(streamResourceProcessForm.getGroupInfo().getMqType());
        if (mqType == MQType.PULSAR || mqType == MQType.TDMQ_PULSAR) {
            return true;
        } else {
            InlongStreamInfo streamInfo = streamResourceProcessForm.getStreamInfo();
            log.warn("no need to create pulsar topic for groupId={}, streamId={}, as the middlewareType={}",
                    streamInfo.getInlongGroupId(), streamInfo.getInlongStreamId(), mqType);
            return false;
        }
    }
}
