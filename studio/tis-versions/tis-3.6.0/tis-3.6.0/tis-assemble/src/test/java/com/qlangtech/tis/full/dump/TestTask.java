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
package com.qlangtech.tis.full.dump;

import com.qlangtech.tis.manage.common.DagTaskUtils;
import org.apache.commons.lang3.StringUtils;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.assemble.TriggerType;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.manage.common.DagTaskUtils.NewTaskParam;
import junit.framework.TestCase;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年7月3日
 */
public class TestTask extends TestCase {

    public void testCreateTask() {
        NewTaskParam newTaskParam = new NewTaskParam();
        // newTaskParam.setBuildHistoryId(taskId);
        String indexname = "search4supplyUnionTabs";
        if (StringUtils.isNotBlank(indexname)) {
            newTaskParam.setAppname(indexname);
        }
        newTaskParam.setWorkflowid(15);
        ExecutePhaseRange execRange = new ExecutePhaseRange(FullbuildPhase.FullDump, FullbuildPhase.IndexBackFlow);
        // (FullbuildPhase.FullDump);
        newTaskParam.setExecuteRanage(execRange);
        // newTaskParam.setToPhase(FullbuildPhase.IndexBackFlow);
        newTaskParam.setTriggerType(TriggerType.MANUAL);
        Integer taskid = DagTaskUtils.createNewTask(newTaskParam);
        System.out.println(taskid);
    }
}
