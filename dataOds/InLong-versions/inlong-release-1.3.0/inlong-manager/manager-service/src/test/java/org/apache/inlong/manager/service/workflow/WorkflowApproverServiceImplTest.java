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

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.workflow.ApproverPageRequest;
import org.apache.inlong.manager.pojo.workflow.ApproverResponse;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.WorkflowApproverServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test class for workflow approver service.
 */
public class WorkflowApproverServiceImplTest extends ServiceBaseTest {

    @Autowired
    protected WorkflowApproverServiceImpl workflowApproverService;

    @Test
    public void testListAndGet() {
        // The workflow approvers was init by SQL file.
        PageResult<ApproverResponse> approverList = workflowApproverService.listByCondition(
                ApproverPageRequest.builder().build());
        Assertions.assertTrue(approverList.getList().size() > 0);

        Integer id = approverList.getList().get(0).getId();
        ApproverResponse approverResponse = workflowApproverService.get(id);
        Assertions.assertEquals(id, approverResponse.getId());
    }

}
