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

package org.apache.inlong.manager.service.workflow.group;

import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test class for ldefine process.
 */
public class CreateGroupWorkflowDefinitionTest extends ServiceBaseTest {

    @Autowired
    CreateGroupWorkflowDefinition createGroupWorkflowDefinition;

    @Test
    public void testDefineProcess() throws CloneNotSupportedException {
        WorkflowProcess process = createGroupWorkflowDefinition.defineProcess();
        WorkflowProcess cloneProcess1 = process.clone();
        WorkflowProcess cloneProcess2 = cloneProcess1.clone();
        Assert.assertTrue(cloneProcess2 != cloneProcess1);
        Assert.assertEquals("Group Resource Creation", process.getType());
        Assert.assertNotNull(process.getTaskByName("initSource"));
        Assert.assertNotNull(process.getTaskByName("initMQ"));
        Assert.assertNotNull(process.getTaskByName("initSort"));
        Assert.assertNotNull(process.getTaskByName("initSink"));
        Assert.assertEquals(4, process.getNameToTaskMap().size());
    }

}
