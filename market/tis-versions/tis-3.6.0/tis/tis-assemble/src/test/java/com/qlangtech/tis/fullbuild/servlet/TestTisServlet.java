/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.fullbuild.servlet;

import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.fullbuild.IFullBuildContext;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.manage.common.HttpUtils;
import junit.framework.TestCase;
import org.easymock.EasyMock;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-06 15:39
 */
public class TestTisServlet extends TestCase {
    /**
     * 测试执行单表流程触发创建
     */
    public void testCreateNewTask() {

        HttpUtils.addMockApply(0, "do_create_new_task"
                , "create_new_task_single_table_index_build_response.json", TestTisServlet.class);

        String collectionName = "search4employee4local";
        Integer taskid = 800;
        String historyId = "666";
        TisServlet tisServlet = new TisServlet();
        IExecChainContext execChainContext = EasyMock.createMock("execChainContext", IExecChainContext.class);

        EasyMock.expect(execChainContext.getWorkflowId()).andReturn(null).anyTimes();
        execChainContext.setAttribute(JobCommon.KEY_TASK_ID, taskid);
        EasyMock.expect(execChainContext.getExecutePhaseRange()).andReturn(ExecutePhaseRange.fullRange());
        EasyMock.expect(execChainContext.hasIndexName()).andReturn(true);
        EasyMock.expect(execChainContext.getIndexName()).andReturn(collectionName);
        EasyMock.expect(execChainContext.getString(IFullBuildContext.KEY_BUILD_HISTORY_TASK_ID)).andReturn(historyId);
        EasyMock.replay(execChainContext);
        Integer newTask = tisServlet.createNewTask(execChainContext);
        assertEquals(taskid, newTask);

        EasyMock.verify(execChainContext);
    }
}
