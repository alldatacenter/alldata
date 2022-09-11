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

package org.apache.inlong.manager.service.sort;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.MQType;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.common.pojo.workflow.form.ProcessForm;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.EventSelector;

/**
 * Event selector for whether ZooKeeper is enabled.
 */
@Slf4j
@Deprecated
public class ZookeeperEnabledSelector implements EventSelector {

    @Override
    public boolean accept(WorkflowContext context) {
        ProcessForm processForm = context.getProcessForm();
        String groupId = processForm.getInlongGroupId();
        if (!(processForm instanceof GroupResourceProcessForm)) {
            log.info("zookeeper enabled was [false] for groupId [{}]", groupId);
            return false;
        }

        GroupResourceProcessForm groupResourceForm = (GroupResourceProcessForm) processForm;
        InlongGroupInfo groupInfo = groupResourceForm.getGroupInfo();
        boolean enable =
                groupInfo.getEnableZookeeper() == 1 && MQType.forType(groupInfo.getMqType()) != MQType.NONE;
        log.info("zookeeper enabled was [{}] for groupId [{}]", enable, groupId);
        return enable;
    }

}
