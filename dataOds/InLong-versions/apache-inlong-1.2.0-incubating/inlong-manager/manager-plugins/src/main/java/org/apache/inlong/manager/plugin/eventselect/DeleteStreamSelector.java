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

package org.apache.inlong.manager.plugin.eventselect;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.pojo.workflow.form.ProcessForm;
import org.apache.inlong.manager.common.pojo.workflow.form.StreamResourceProcessForm;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.EventSelector;

/**
 * Selector of delete stream event.
 */
@Slf4j
public class DeleteStreamSelector implements EventSelector {

    @SneakyThrows
    @Override
    public boolean accept(WorkflowContext context) {
        ProcessForm processForm = context.getProcessForm();
        String groupId = processForm.getInlongGroupId();
        if (!(processForm instanceof StreamResourceProcessForm)) {
            log.info("not add deleteStream listener, as the form was not StreamResourceProcessForm for groupId [{}]",
                    groupId);
            return false;
        }

        StreamResourceProcessForm streamResourceProcessForm = (StreamResourceProcessForm) processForm;
        String streamId = streamResourceProcessForm.getStreamInfo().getInlongStreamId();
        boolean flag = streamResourceProcessForm.getGroupOperateType() == GroupOperateType.DELETE;
        if (!flag) {
            log.info("not add deleteStream listener, as the operate was not DELETE for groupId [{}] and streamId [{}]",
                    groupId, streamId);
            return false;
        }

        log.info("add deleteStream listener for groupId [{}] and streamId [{}]", groupId, streamId);
        return true;
    }
}
