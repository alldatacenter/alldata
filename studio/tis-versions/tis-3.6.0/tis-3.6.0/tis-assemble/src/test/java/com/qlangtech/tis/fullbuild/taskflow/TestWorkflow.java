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
package com.qlangtech.tis.fullbuild.taskflow;

import java.util.Map;
import com.qlangtech.tis.fullbuild.taskflow.TaskWorkflow;
import com.qlangtech.tis.fullbuild.taskflow.impl.EndTask;
import com.qlangtech.tis.fullbuild.taskflow.impl.ForkTask;
import com.qlangtech.tis.fullbuild.taskflow.impl.StartTask;
import com.google.common.collect.Maps;
import junit.framework.TestCase;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年12月1日
 */
public class TestWorkflow extends TestCase {

    public void testWorkflow() {
        TaskWorkflow workflow = new TaskWorkflow();
        StartTask start = new StartTask();
        start.setSuccessTo("fork");
        workflow.addTask(start);
        EndTask end = new EndTask();
        end.setName("final");
        workflow.addTask(end);
        ForkTask fork = new ForkTask();
        fork.setName("fork");
        fork.setSuccessTo("1,2,3");
        workflow.addTask(fork);
        TestTask t = new TestTask("1", "4");
        workflow.addTask(t);
        t = new TestTask("2", "4");
        workflow.addTask(t);
        t = new TestTask("3", "final");
        workflow.addTask(t);
        t = new TestTask("4", "fork2");
        workflow.addTask(t);
        fork = new ForkTask();
        fork.setName("fork2");
        fork.setSuccessTo("5,6,7");
        workflow.addTask(fork);
        t = new TestTask("5", "final");
        workflow.addTask(t);
        t = new TestTask("6", "final");
        workflow.addTask(t);
        t = new TestTask("7", "final");
        workflow.addTask(t);
        workflow.init();
        for (TaskDependency d : workflow.tasksMap.values()) {
            System.out.println(d.getTask().getName() + "->" + d.getPrecondition());
        }
        System.out.println("======================================");
        Map<String, Object> params = Maps.newHashMap();
        workflow.startExecute(params);
        System.out.println("execute successful");
    }
}
