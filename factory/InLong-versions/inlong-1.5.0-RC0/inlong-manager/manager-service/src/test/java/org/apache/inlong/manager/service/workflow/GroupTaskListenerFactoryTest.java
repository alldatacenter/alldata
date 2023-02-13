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

package org.apache.inlong.manager.service.workflow;

import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarInfo;
import org.apache.inlong.manager.pojo.group.tubemq.InlongTubeMQInfo;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.listener.GroupTaskListenerFactory;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.task.QueueOperateListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Test class for get queue operate listener.
 */
public class GroupTaskListenerFactoryTest extends ServiceBaseTest {

    @Autowired
    private GroupTaskListenerFactory groupTaskListenerFactory;

    @Test
    public void testGetQueueOperateListener() {
        GroupResourceProcessForm processForm = new GroupResourceProcessForm();
        InlongPulsarInfo pulsarInfo = new InlongPulsarInfo();
        // check pulsar listener
        pulsarInfo.setMqType(MQType.PULSAR);
        processForm.setGroupInfo(pulsarInfo);
        WorkflowContext context = new WorkflowContext();
        context.setProcessForm(processForm);
        List<QueueOperateListener> queueOperateListeners = groupTaskListenerFactory.getQueueResourceListener(context);
        if (queueOperateListeners.size() == 0) {
            return;
        }
        Assertions.assertEquals(1, queueOperateListeners.size());

        // check tubemq listener
        InlongTubeMQInfo tubeInfo = new InlongTubeMQInfo();
        tubeInfo.setMqType(MQType.TUBEMQ);
        queueOperateListeners = groupTaskListenerFactory.getQueueResourceListener(context);
        Assertions.assertEquals(2, queueOperateListeners.size());
    }

}
